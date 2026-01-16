package com.example.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.DeviceType;
import com.example.demo.model.ModeRule;
import com.example.demo.model.ModeType;
import com.example.demo.model.Permission;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.ModeRuleRepository;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@RequiredArgsConstructor
public class DemoApplication implements ApplicationRunner {
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ModeRuleRepository modeRuleRepository;

    private final String jwtSecret = "66546A555E5A7234753778214123222A472D4B614E645262356B587032733576";
    private final Long jwtExpiration = 86400000L;
    private final String telegramToken = "8427237335:AAF_lDzXJjUzcEUHdrNbmlkvCYEI5C0GmEQ";
    private final String telegramChatId = "648084323";
    private final String datasourceUrl = "jdbc:h2:mem:testdb;DB_CLOSE_ON_EXIT=FALSE";

    @PostConstruct
    public void checkConfiguration() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ПРОВЕРКА КОНФИГУРАЦИИ ПРИЛОЖЕНИЯ");
        System.out.println("=".repeat(60));

        System.out.println("🔐 JWT Конфигурация:");
        System.out.println("   ✓ JWT Secret: ЗАГРУЖЕН (" + jwtSecret.length() + " символов)");
        System.out.println("     Первые 10 символов: " + jwtSecret.substring(0, Math.min(10, jwtSecret.length())) + "...");
        System.out.println("   ✓ JWT Expiration: " + (jwtExpiration / 1000 / 60) + " минут");

        System.out.println("\n🤖 Telegram Конфигурация:");
        System.out.println("   ✓ Telegram Token: ЗАГРУЖЕН (" + telegramToken.length() + " символов)");
        System.out.println("     Первые 10 символов: " + telegramToken.substring(0, Math.min(10, telegramToken.length())) + "...");
        System.out.println("   ✓ Telegram Chat ID: " + telegramChatId);

        System.out.println("\n🗄️ База данных:");
        System.out.println("   ✓ Datasource URL: " + datasourceUrl);

        System.out.println("=".repeat(60) + "\n");
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("🚀 Запуск инициализации данных...");

        Permission userRead = createPermissionIfNotFound("USER", "READ");
        Permission userWrite = createPermissionIfNotFound("USER", "WRITE");
        Permission deviceRead = createPermissionIfNotFound("DEVICE", "READ");
        Permission deviceWrite = createPermissionIfNotFound("DEVICE", "WRITE");
        Permission roomRead = createPermissionIfNotFound("ROOM", "READ");
        Permission roomWrite = createPermissionIfNotFound("ROOM", "WRITE");
        Permission modeControl = createPermissionIfNotFound("MODE", "CONTROL");
        Permission fileRead = createPermissionIfNotFound("FILE", "READ");
        Permission fileWrite = createPermissionIfNotFound("FILE", "WRITE");

        Role userRole = createRoleIfNotFound("USER", Set.of(
            deviceRead, roomRead, fileRead
        ));

        Role adminRole = createRoleIfNotFound("ADMIN", Set.of(
            userRead, userWrite, deviceRead, deviceWrite,
            roomRead, roomWrite, modeControl, fileRead, fileWrite
        ));

        // Создаем пользователей с правильными ролями
        createUserIfNotFound("user", "user", userRole);
        createUserIfNotFound("admin", "admin", adminRole);

        // --- Новый блок: Создание начальных правил ---
        if (modeRuleRepository.count() == 0) {
            System.out.println("\n📋 Создание начальных правил режимов...");

            // Пример 1: Правило для экономичного режима для кондиционеров
            ModeRule ecoCoolerRule = new ModeRule();
            ecoCoolerRule.setModeType(ModeType.ECO);
            ecoCoolerRule.setDeviceType(DeviceType.AC);
            ecoCoolerRule.setTitlePattern(".*");
            ecoCoolerRule.setMinPower(0.1);
            ecoCoolerRule.setMaxPower(0.5);
            ecoCoolerRule.setShouldBeActive(true);
            ecoCoolerRule.setPriority(1);
            modeRuleRepository.save(ecoCoolerRule);

            // Пример 2: Правило для активного режима для обогревателей
            ModeRule heatHeaterRule = new ModeRule();
            heatHeaterRule.setModeType(ModeType.HEAT);
            heatHeaterRule.setDeviceType(DeviceType.HEATER);
            heatHeaterRule.setTitlePattern("Main.*");
            heatHeaterRule.setMinPower(0.8);
            heatHeaterRule.setMaxPower(1.0);
            heatHeaterRule.setShouldBeActive(false);
            heatHeaterRule.setPriority(2);
            modeRuleRepository.save(heatHeaterRule);

            // Пример 3: Правило для автоматического режима
            ModeRule autoFanRule = new ModeRule();
            autoFanRule.setModeType(ModeType.AUTO);
            autoFanRule.setDeviceType(DeviceType.FAN);
            autoFanRule.setTitlePattern("LivingRoom.*");
            autoFanRule.setMinPower(0.2);
            autoFanRule.setMaxPower(0.7);
            autoFanRule.setShouldBeActive(true);
            autoFanRule.setPriority(3);
            modeRuleRepository.save(autoFanRule);

            System.out.println("✓ Создано 3 начальных правила режимов");
        } else {
            System.out.println("\n📋 Правила режимов уже созданы: " + modeRuleRepository.count() + " правил");
        }
        // --- Конец нового блока ---

        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ ИНИЦИАЛИЗАЦИЯ ДАННЫХ ЗАВЕРШЕНА");
        System.out.println("=".repeat(60));
        System.out.println("👤 ПОЛЬЗОВАТЕЛИ ДЛЯ ТЕСТИРОВАНИЯ:");
        System.out.println("   USER:  логин: user, пароль: user (только просмотр)");
        System.out.println("   ADMIN: логин: admin, пароль: admin (полные права)");
        System.out.println("=".repeat(60));
    }

    private Permission createPermissionIfNotFound(String resource, String operation) {
        Permission permission = permissionRepository.findByResourceAndOperation(resource, operation);
        if (permission == null) {
            permission = new Permission(resource, operation);
            permission = permissionRepository.save(permission);
            System.out.println("   ✓ Создано право: " + resource + ":" + operation);
        }
        return permission;
    }

    private Role createRoleIfNotFound(String name, Set<Permission> permissions) {
        Role role = roleRepository.findByName(name);
        if (role == null) {
            role = new Role();
            role.setName(name);
            role.setPermissions(permissions);
            role = roleRepository.save(role);
            System.out.println("   ✓ Создана роль: " + name + " с " + permissions.size() + " правами");
        }
        return role;
    }

    private void createUserIfNotFound(String username, String password, Role role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .build();
            userRepository.save(user);
            System.out.println("   ✓ Создан пользователь: " + username + " с ролью: " + role.getName());
        } else {
            System.out.println("   ⏭️  Пользователь уже существует: " + username);
        }
    }
}