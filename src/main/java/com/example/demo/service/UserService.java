package com.example.demo.service;

import com.example.demo.dto.UserCreateDto;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    // Проверка, есть ли пользователи в системе
    public boolean hasUsers() {
        return userRepository.count() > 0;
    }

    // создание пользователя с валидацией
    public User createUserWithRole(UserCreateDto userCreateDto) {
        logger.debug("Creating user with role: {}", userCreateDto.roleId());
        
        // Проверка уникальности username
        if (userRepository.existsByUsername(userCreateDto.username())) {
            logger.error("Username already exists: {}", userCreateDto.username());
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Поиск роли
        Role role = roleRepository.findById(userCreateDto.roleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        // Создание пользователя
        User user = User.builder()
            .username(userCreateDto.username())
            .password(passwordEncoder.encode(userCreateDto.password()))
            .role(role)
            .build();
        
        logger.info("Created user: {}", userCreateDto.username());
        return userRepository.save(user);
    }

    // Создание администратора
    public User createAdminUser(String username, String password) {
        logger.debug("Creating admin user: {}", username);
        Role adminRole = roleRepository.findByName("ADMIN");
        if (adminRole == null) {
            logger.error("ADMIN role not found");
            throw new IllegalStateException("ADMIN role not found");
        }
        
        User user = User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .role(adminRole)
            .build();
        
        logger.info("Created admin user: {}", username);
        return userRepository.save(user);
    }

    public User toggleUserStatus(Long userId, boolean enabled) {
        logger.debug("Toggling user status: {}", userId);
        User user = getUserById(userId);
        if (user == null) {
            logger.error("User not found: {}", userId);
            throw new IllegalArgumentException("User not found");
        }
        logger.info("Toggled user status for user: {}", userId);
        return userRepository.save(user);
    }

    // Создание пользователя
    public User createUser(User user) {
        logger.debug("Creating user: {}", user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Кодируем пароль
        User savedUser = userRepository.save(user);
        logger.info("Created user: {}", user.getUsername());
        return savedUser;
    }

    // Получение всех пользователей
    public List<User> getAllUsers() {
        logger.info("Getting all users");
        return userRepository.findAll();
    }

    // Получение пользователя по ID
    public User getUserById(Long id) {
        logger.debug("Getting user by id: {}", id);
        return userRepository.findById(id).orElse(null);
    }

    // Получение пользователя по username
    public User getUserByUsername(String username) {
        logger.debug("Getting user by username: {}", username);
        return userRepository.findByUsername(username).orElse(null);
    }

    // Обновление пользователя
    public User updateUser(Long id, User userDetails) {
        logger.debug("Updating user with id: {}", id);
        return userRepository.findById(id)
            .map(existingUser -> {
                if (userDetails.getUsername() != null) {
                    existingUser.setUsername(userDetails.getUsername());
                }
                if (userDetails.getPassword() != null) {
                    existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                }
                logger.info("Updated user: {}", id);
                return userRepository.save(existingUser);
            })
            .orElse(null);
    }

    // Удаление пользователя
    public boolean deleteUser(Long id) {
        logger.debug("Deleting user with id: {}", id);
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            logger.info("Deleted user: {}", id);
            return true;
        }
        logger.warn("User not found for deletion: {}", id);
        return false;
    }
}