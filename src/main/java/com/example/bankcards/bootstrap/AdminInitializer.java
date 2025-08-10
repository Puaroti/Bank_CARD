package com.example.bankcards.bootstrap;

import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * Инициализатор, создающий пользователя-администратора при старте приложения,
 * если он отсутствует. Поведение управляется свойствами app.bootstrap.admin.*
 *
 * Свойства (application.yml / ENV):
 * - app.bootstrap.admin.enabled (boolean) — включить автосоздание (по умолчанию: true)
 * - app.bootstrap.admin.username (string) — логин, по умолчанию: admin
 * - app.bootstrap.admin.password (string) — пароль, по умолчанию: changeMe123
 * - app.bootstrap.admin.full-name (string) — отображаемое ФИО, по умолчанию: Администратор Системы
 * - app.bootstrap.admin.first-name / last-name / patronymic — обязательные ФИО для сущности
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password:changeMe123}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.full-name:Администратор Системы}")
    private String adminFullName;

    @Value("${app.bootstrap.admin.first-name:Администратор}")
    private String adminFirstName;

    @Value("${app.bootstrap.admin.last-name:Системы}")
    private String adminLastName;

    @Value("${app.bootstrap.admin.patronymic:По-умолчанию}")
    private String adminPatronymic;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userRepository.existsByUsername(adminUsername)) {
                log.info("Администратор '{}' уже существует — пропускаем создание.", adminUsername);
                return;
            }

            String hashed = passwordEncoder.encode(adminPassword);
            UserEntity admin = UserEntity.builder()
                    .username(adminUsername)
                    .passwordHash(hashed)
                    .fullName(adminFullName)
                    .firstName(adminFirstName)
                    .lastName(adminLastName)
                    .patronymic(adminPatronymic)
                    .role("ADMIN")
                    .build();

            userRepository.save(admin);
            log.warn("Создан пользователь-администратор '{}' с ролью ADMIN. НАСТОЯТЕЛЬНО рекомендуется сменить пароль!", adminUsername);
        } catch (Exception ex) {
            log.error("Ошибка при инициализации администратора: {}", ex.getMessage(), ex);
        }
    }
}
