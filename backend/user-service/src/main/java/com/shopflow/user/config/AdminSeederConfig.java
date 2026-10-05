package com.shopflow.user.config;

import com.shopflow.common.security.SecurityHeaders;
import com.shopflow.user.domain.UserAccount;
import com.shopflow.user.domain.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Membuat akun admin pertama saat aplikasi start.
 *
 * <p>Kenapa tidak pakai {@code INSERT} di migration SQL? Karena hash BCrypt harus
 * dihitung dengan encoder yang sama, dan menempelkan hash di file migrasi membuat
 * password "rahasia" itu ikut ke Git. Di sini password sumbernya dari env var.
 *
 * <p>Matikan di produksi: {@code SHOPFLOW_SEED_ENABLED=false}
 */
@Configuration
@ConditionalOnProperty(name = "shopflow.seed.enabled", havingValue = "true")
public class AdminSeederConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminSeederConfig.class);

    @Bean
    public ApplicationRunner seedAdminUser(UserAccountRepository repository,
                                           PasswordEncoder passwordEncoder,
                                           org.springframework.core.env.Environment environment) {
        return args -> {
            String email = environment.getProperty("shopflow.seed.admin-email", "admin@shopflow.dev");
            String password = environment.getProperty("shopflow.seed.admin-password", "admin12345");

            if (repository.existsByEmailIgnoreCase(email)) {
                log.info("Seed dilewati: admin '{}' sudah ada", email);
                return;
            }

            UserAccount admin = new UserAccount(
                    email.toLowerCase(),
                    passwordEncoder.encode(password),
                    "ShopFlow Admin",
                    SecurityHeaders.ROLE_CUSTOMER + "," + SecurityHeaders.ROLE_ADMIN);
            repository.save(admin);

            log.warn("Akun admin development dibuat: {} / {}  <-- GANTI di produksi!",
                    email, password);
        };
    }
}
