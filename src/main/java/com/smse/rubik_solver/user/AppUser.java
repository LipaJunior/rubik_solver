package com.smse.rubik_solver.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Uzytkownik aplikacji. Tworzony przy pierwszym logowaniu (Faza 2 - Google OAuth).
 * subscriptionStatus decyduje o dostepie do funkcji premium (Faza 4/5).
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    // Id klienta w Stripe (ustawiane po pierwszej platnosci).
    private String stripeCustomerId;

    // free | active | canceled - zrodlem prawdy jest webhook Stripe (Faza 4).
    @Column(nullable = false)
    private String subscriptionStatus = "free";
}
