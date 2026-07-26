package com.smse.rubik_solver.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

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
                // Email bywa w "customer_email" ALBO w "customer_details.email" (zaleznie
                // od wersji API) - probujemy obu.
                String email = text(object, "customer_email");
                if (email == null) {
                    JsonNode details = object.get("customer_details");
                    if (details != null) {
                        email = text(details, "email");
                    }
                }
                String customer = text(object, "customer");

                if (email == null) {
                    log.warn("Stripe checkout.session.completed bez emaila - nie moge nadac premium");
                } else {
                    final String userEmail = email;
                    // Upsert: jesli usera nie ma w bazie (np. H2 sie zresetowalo albo login
                    // go nie zapisal), tworzymy go od razu jako premium. E-mail pochodzi z
                    // uwierzytelnionej platnosci, wiec to bezpieczne.
                    AppUser u = users.findByEmail(userEmail).orElseGet(() -> {
                        AppUser nu = new AppUser();
                        nu.setEmail(userEmail);
                        return nu;
                    });
                    u.setStripeCustomerId(customer);
                    u.setSubscriptionStatus("active");
                    users.save(u);
                    log.info("Premium nadane uzytkownikowi {}", userEmail);
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
