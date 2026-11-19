package com.example.service;

import com.example.domain.Role;
import com.example.domain.User;
import com.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(String username, String fullName, String password, String confirmPassword, Role role) {
        if (username == null || !username.trim().matches("^[a-zA-Z0-9_]{4,}$")) {
            throw new IllegalArgumentException("Username harus berupa alfanumerik minimal 4 karakter tanpa spasi.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama lengkap wajib diisi.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password minimal 6 karakter.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Konfirmasi password tidak cocok.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role user wajib dipilih.");
        }
        if (userRepository.existsByUsername(username.trim().toLowerCase())) {
            throw new IllegalArgumentException("Username sudah terdaftar.");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username.trim().toLowerCase(), hashedPassword, fullName.trim(), role);
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, Role role, String newPassword, String confirmPassword, Long currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama lengkap wajib diisi.");
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password baru minimal 6 karakter.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Konfirmasi password baru tidak cocok.");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        // Check if changing role from OWNER to CASHIER leaves zero active owners
        if (user.getRole() == Role.OWNER && role == Role.CASHIER) {
            long activeOwners = userRepository.countByRoleAndActiveTrue(Role.OWNER);
            if (activeOwners <= 1) {
                throw new IllegalStateException("Sistem harus memiliki minimal satu akun OWNER aktif.");
            }
        }

        user.setFullName(fullName.trim());
        if (role != null) {
            user.setRole(role);
        }
        return userRepository.save(user);
    }

    public User deactivateUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalStateException("Anda tidak dapat menonaktifkan akun Anda sendiri yang sedang aktif.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (user.getRole() == Role.OWNER && user.isActive()) {
            long activeOwners = userRepository.countByRoleAndActiveTrue(Role.OWNER);
            if (activeOwners <= 1) {
                throw new IllegalStateException("Sistem harus memiliki minimal satu akun OWNER aktif.");
            }
        }

        user.setActive(false);
        return userRepository.save(user);
    }

    public User activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        user.setActive(true);
        return userRepository.save(user);
    }
}
