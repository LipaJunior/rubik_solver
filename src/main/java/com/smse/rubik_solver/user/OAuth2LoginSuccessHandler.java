package com.smse.rubik_solver.user;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Po udanym logowaniu przez Google: jesli uzytkownika jeszcze nie ma w bazie,
 * tworzymy go (upsert po e-mailu). Potem przekierowanie na strone glowna.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository users;

    public OAuth2LoginSuccessHandler(UserRepository users) {
        this.users = users;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof OidcUser oidc && oidc.getEmail() != null) {
            users.findByEmail(oidc.getEmail()).orElseGet(() -> {
                AppUser u = new AppUser();
                u.setEmail(oidc.getEmail());
                u.setName(oidc.getFullName());
                return users.save(u);
            });
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
