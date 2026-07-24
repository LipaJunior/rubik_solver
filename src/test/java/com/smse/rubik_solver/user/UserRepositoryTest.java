package com.smse.rubik_solver.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository users;

    @Test
    void savesAndFindsUserByEmail() {
        AppUser u = new AppUser();
        u.setEmail("test@example.com");
        u.setName("Test User");
        users.save(u);

        var found = users.findByEmail("test@example.com");
        assertTrue(found.isPresent(), "Uzytkownik powinien byc znaleziony po emailu");
        assertEquals("free", found.get().getSubscriptionStatus(), "Domyslny status subskrypcji = free");
    }
}
