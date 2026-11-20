package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class UserService {
    private final UserRepository users;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    public UserService(UserRepository users, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }
    public List<User> findAll() { return users.findAll(); }
    public User findById(Long id) { return users.findById(id).orElseThrow(() -> Rules.missing("User", id)); }
    public User create(User user) {
        Rules.text(user.getUsername(), "username");
        Rules.text(user.getEmail(), "email");
        String password = user.getPassword();
        if (password == null || password.length() < 10
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !password.matches("(?s).*[A-Z].*") || !password.matches("(?s).*[a-z].*")
                || !password.matches("(?s).*[0-9].*"))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Password: 10+ characters, uppercase, lowercase, digit; max 72 UTF-8 bytes");
        Rules.require(!users.existsByUsernameOrEmail(user.getUsername(), user.getEmail()), "Account already exists");
        user.setPasswordHash(encoder.encode(password)); user.setPassword(null);
        user.setRole("TRANSLATOR"); user.setEnabled(true);
        user.setId(null);
        user.setCreatedAt(Instant.now());
        return users.save(user);
    }
    public User update(Long id, User patch) {
        User user = findById(id);
        if (patch.getUsername() != null) { Rules.text(patch.getUsername(), "username"); user.setUsername(patch.getUsername()); }
        if (patch.getEmail() != null) { Rules.text(patch.getEmail(), "email"); user.setEmail(patch.getEmail()); }
        return users.save(user);
    }
    public void delete(Long id) {
        // Внешние ключи запрещают удаление владельца проекта или автора перевода.
        users.delete(findById(id));
    }
}

