package com.edgareldy.springmodulithtutorial.auth;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the administrator account at startup from {@code APP_ADMIN_EMAIL} / {@code APP_ADMIN_PASSWORD},
 * only when both are set and the account does not exist yet. Without them, no ADMIN account exists.
 * <p>
 * Created edgar.muhamyangabo on 9/26/26
 * Author : edgar.muhamyangabo
 * Date : 9/26/26
 * Project : spring-modulith-tutorial
 */
// An ApplicationRunner runs once the context is fully started (Flyway has migrated, the roles are
// seeded), which is exactly when an account can be inserted. Keeping the credentials in environment
// variables means no default password ever sits in the code or in a migration.
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param properties      module settings, holding the optional admin credentials
     * @param userRepository  accounts
     * @param roleRepository  seeded roles
     * @param passwordEncoder password hashing
     */
    AdminBootstrap(AuthProperties properties, UserRepository userRepository, RoleRepository roleRepository,
                   PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.Admin admin = properties.admin();
        if (admin == null || !admin.isConfigured()) {
            log.debug("No administrator configured, skipping the admin bootstrap");
            return;
        }
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            log.info("Administrator account {} already exists", email);
            return;
        }
        Role adminRole = roleRepository.findByRoleName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role " + Role.ADMIN + " is not seeded"));
        User user = new User("Admin", "Administrator", email, passwordEncoder.encode(admin.password()));
        user.setEnabled(true);
        user.addRole(adminRole);
        userRepository.save(user);
        log.info("Administrator account {} created", email);
    }
}
