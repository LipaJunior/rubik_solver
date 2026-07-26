package com.smse.rubik_solver.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

/**
 * Odbiera zdarzenia od Stripe. To webhook (a nie klient) nadaje/odbiera premium.
 * Endpoint jest publiczny (permitAll w SecurityConfig), ale kazde zdarzenie jest
 * weryfikowane podpisem - bez wlasciwego STRIPE_WEBHOOK_SECRET nic nie przejdzie.
 */
@RestController
public class StripeWebhookController {

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    private final UserRepository users;
    private final ObjectMapper mapper = new ObjectMapper();

    public StripeWebhookController(UserRepository users) {
        this.users = users;
    }

    @PostMapping("/api/stripe/webhook")
    public ResponseEntity<String> handle(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        // Weryfikacja podpisu - bez wlasciwego sekretu odrzucamy.
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        // Dane czytamy z surowego JSON-a (odporne na niezgodnosc wersji API/SDK).
        JsonNode object;
        try {
            object = mapper.readTree(payload).path("data").path("object");
        } catch (Exception e) {
            return ResponseEntity.ok(""); // nie blokuj Stripe'a - i tak potwierdzamy odbior
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                String email = text(object, "customer_email");
                String customer = text(object, "customer");
                if (email != null) {
                    users.findByEmail(email).ifPresent(u -> {
                        u.setStripeCustomerId(customer);
                        u.setSubscriptionStatus("active");
                        users.save(u);
                    });
                }
            }
            case "customer.subscription.deleted" -> {
                String customer = text(object, "customer");
                if (customer != null) {
                    users.findByStripeCustomerId(customer).ifPresent(u -> {
                        u.setSubscriptionStatus("canceled");
                        users.save(u);
                    });
                }
            }
            default -> {
                // inne zdarzenia ignorujemy
            }
        }

        return ResponseEntity.ok("");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText() : null;
    }
}
