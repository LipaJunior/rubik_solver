package com.smse.rubik_solver.user;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final UserRepository users;

    public AccountController(UserRepository users) {
        this.users = users;
    }

    /**
     * Publiczny status konta - front na jego podstawie pokazuje login/logout i
     * ewentualnie funkcje premium. Dla niezalogowanych zwraca authenticated=false.
     */
    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser principal) {
        Map<String, Object> result = new HashMap<>();
        if (principal == null) {
            result.put("authenticated", false);
            return result;
        }

        AppUser user = users.findByEmail(principal.getEmail()).orElse(null);
        result.put("authenticated", true);
        result.put("email", principal.getEmail());
        result.put("name", principal.getFullName());
        result.put("premium", user != null && "active".equals(user.getSubscriptionStatus()));
        return result;
    }
}
