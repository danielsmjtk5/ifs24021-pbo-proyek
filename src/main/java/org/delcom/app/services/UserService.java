package org.delcom.app.services;

import java.util.Objects;
import java.util.UUID; // ✅ Tambahan Import

import org.delcom.app.entities.User;
import org.delcom.app.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createUser(String name, String email, String password) {
        User user = new User(name, email, password);
        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findFirstByEmail(email).orElse(null);
    }

    public User getUserById(UUID id) {
        // ✅ Fix: Membungkus id dengan Objects.requireNonNull
        return userRepository.findById(Objects.requireNonNull(id)).orElse(null);
    }

    @Transactional
    public User updateUser(UUID id, String name, String email) {
        // ✅ Fix: Membungkus id dengan Objects.requireNonNull
        User user = userRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (user == null) {
            return null;
        }
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(UUID id, String newPassword) {
        // ✅ Fix: Membungkus id dengan Objects.requireNonNull
        User user = userRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (user == null) {
            return null;
        }
        user.setPassword(newPassword);
        return userRepository.save(user);
    }
}