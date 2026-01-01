package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.backend.entity.DocumentFolder;
import com.example.backend.entity.User;
import com.example.backend.repository.DocumentFolderRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.util.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ensures a default admin account exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DocumentFolderRepository documentFolderRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.admin.username:管理员}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        userRepository.findByEmail(adminEmail)
                .ifPresentOrElse(this::normalizeExistingAdmin, this::createDefaultAdmin);
    }

    private void normalizeExistingAdmin(User user) {
        boolean changed = false;
        if (!"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
            changed = true;
        }
        if (!"".equals(user.getStatus())) {
            user.setStatus("");
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
            log.info("Normalized default admin account {}", adminEmail);
        } else {
            log.info("Default admin account {} already exists", adminEmail);
        }
        ensureRootFolder(user);
    }

    private void createDefaultAdmin() {
        String publicId;
        do {
            publicId = IdGenerator.generatePublicId();
        } while (userRepository.existsByPublicId(publicId));

        User user = User.builder()
                .publicId(publicId)
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role("ADMIN")
                .status("")
                .build();

        user = userRepository.save(user);
        ensureRootFolder(user);
        log.warn("Default admin created. email={}, password={} (please change after login)", adminEmail, adminPassword);
    }

    private void ensureRootFolder(User user) {
        if (documentFolderRepository.findRootFolderByOwnerId(user.getId()).isEmpty()) {
            DocumentFolder root = DocumentFolder.builder()
                    .owner(user)
                    .name("根目录")
                    .parent(null)
                    .status("")
                    .build();
            documentFolderRepository.save(root);
        }
    }
}
