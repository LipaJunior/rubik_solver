package com.smse.rubik_solver.user;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@RestController
public class BillingController {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.price-id}")
    private String priceId;

    @Value("${app.url}")
    private String appUrl;

    /**
     * Tworzy testowa/produkcyjna sesje platnosci Stripe (subskrypcja) i zwraca URL,
     * na ktory front przekierowuje uzytkownika. Endpoint pod /api/** => wymaga
     * zalogowania (SecurityConfig), wiec principal jest zawsze dostepny.
     */
    @PostMapping("/api/checkout")
    public Map<String, String> checkout(@AuthenticationPrincipal OidcUser principal) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(appUrl + "/?upgraded=1")
                .setCancelUrl(appUrl + "/")
                .setCustomerEmail(principal.getEmail())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .build();

        Session session = Session.create(params);
        return Map.of("url", session.getUrl());
    }
}
