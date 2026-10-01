package com.hungrybox.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds an admin account from ADMIN_EMAIL / ADMIN_PASSWORD so the admin dashboard is usable out of the box. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final String email;
    private final String password;

    public AdminBootstrap(UserService userService,
                          @Value("${hungrybox.admin.email:}") String email,
                          @Value("${hungrybox.admin.password:}") String password) {
        this.userService = userService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("No bootstrap admin configured");
            return;
        }
        if (userService.ensureAdmin(email, password)) {
            log.info("Created bootstrap admin account {}", email);
        }
    }
}
