package com.example.specdriven.usecases.uc009_manage_users_and_roles;

import com.example.domain.Role;
import com.example.domain.User;
import com.example.security.CustomUserDetailsService;
import com.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC009ManageUsersAndRolesTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private User sampleOwner;
    private User sampleKasir;

    @BeforeEach
    void setUp() {
        sampleOwner = userService.findByUsername("admin").orElseGet(() ->
                userService.createUser("admin_test", "Admin Test", "admin123", "admin123", Role.OWNER));

        sampleKasir = userService.findByUsername("kasir").orElseGet(() ->
                userService.createUser("kasir_test", "Kasir Test", "kasir123", "kasir123", Role.CASHIER));
    }

    @Test
    void mainFlow_createUserSuccessfully() {
        User created = userService.createUser("kasir_budi", "Budi Operator", "password123", "password123", Role.CASHIER);
        assertNotNull(created.getId());
        assertEquals("kasir_budi", created.getUsername());
        assertEquals("Budi Operator", created.getFullName());
        assertEquals(Role.CASHIER, created.getRole());
        assertTrue(created.isActive());

        // Password must be hashed with BCrypt
        assertNotEquals("password123", created.getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", created.getPasswordHash()));
    }

    @Test
    void af1_editUserAndChangePassword() {
        User staff = userService.createUser("staff_update", "Staff Lama", "rahasia1", "rahasia1", Role.CASHIER);

        User updated = userService.updateUser(
                staff.getId(), "Staff Baru", Role.CASHIER, "rahasia2", "rahasia2", sampleOwner.getId());

        assertEquals("Staff Baru", updated.getFullName());
        assertTrue(passwordEncoder.matches("rahasia2", updated.getPasswordHash()));
    }

    @Test
    void af2_deactivateUserAccount() {
        User staff = userService.createUser("staff_deact", "Staff Deact", "pass123", "pass123", Role.CASHIER);
        assertTrue(staff.isActive());

        User deactivated = userService.deactivateUser(staff.getId(), sampleOwner.getId());
        assertFalse(deactivated.isActive());

        // Attempting to load deactivated user through UserDetailsService throws exception
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("staff_deact");
        });
    }

    @Test
    void af3_preventDeactivatingOwnAccount() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            userService.deactivateUser(sampleOwner.getId(), sampleOwner.getId());
        });
        assertTrue(ex.getMessage().contains("akun Anda sendiri"));
    }

    @Test
    void br02_usernameFormatAndUniqueness() {
        // Less than 4 characters
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("usr", "Nama", "pwd123", "pwd123", Role.CASHIER);
        });

        // Contains invalid symbols
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("user-invalid!", "Nama", "pwd123", "pwd123", Role.CASHIER);
        });

        // Duplicate
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(sampleOwner.getUsername(), "Nama", "pwd123", "pwd123", Role.CASHIER);
        });
    }

    @Test
    void br03_passwordsHashedWithBCrypt() {
        User user = userService.createUser("bcrypt_user", "Bcrypt Test", "mySecurePass", "mySecurePass", Role.CASHIER);
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$"));
    }

    @Test
    void br04_mustMaintainAtLeastOneActiveOwner() {
        // If there's only 1 active owner (sampleOwner), deactivating or changing role to CASHIER must fail
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            userService.updateUser(sampleOwner.getId(), "Owner", Role.CASHIER, null, null, 999L);
        });
        assertTrue(ex.getMessage().contains("minimal satu akun OWNER"));
    }

    @Test
    void br06_deactivatedUserCannotAuthenticate() {
        User staff = userService.createUser("staff_block", "Staff Blocked", "block123", "block123", Role.CASHIER);
        userService.deactivateUser(staff.getId(), sampleOwner.getId());

        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("staff_block");
        });
    }
}
