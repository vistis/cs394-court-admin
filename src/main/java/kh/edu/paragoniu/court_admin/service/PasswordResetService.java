package kh.edu.paragoniu.court_admin.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;

@Service
public class PasswordResetService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /**
     * Returns the matched user's ID if username+email match an existing, active account.
     */
    public Optional<UUID> verifyIdentity(String username, String email) {
        return userRepository.findByUsernameAndEmail(username, email)
            .filter(User::isActive) // deactivated accounts can't reset either — they can't log in anyway
            .map(User::getUserId);
    }

    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}