package com.mgd.bookstore.controller;

import com.mgd.bookstore.model.User;
import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.repository.UserRepository;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Register
    @PostMapping("/register")
    public Map<String, Object> registerUser(@RequestParam String username,
                                            @RequestParam String email,
                                            @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByUsername(username)) {
            response.put("success", false);
            response.put("message", "Username already exists");
            return response;
        }

        if (userRepository.existsByEmail(email)) {
            response.put("success", false);
            response.put("message", "Email already exists");
            return response;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_USER);

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "User registered successfully");
        return response;
    }

    // ✅ Login
    @PostMapping("/login")
    public Map<String, Object> loginUser(@RequestParam String username,
                                         @RequestParam String password,
                                         HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return response;
        }

        User user = userOpt.get();

        // Create Spring Security session
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(username, password,
                        List.of(new SimpleGrantedAuthority(user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        // Return user info for frontend
        response.put("success", true);
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        return response;
    }
}