package com.mgd.bookstore.controller;

import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("$2a$10$hashedpassword");
        user.setRole(Role.ROLE_USER);
    }

    // -----------------------------------------------------------------------
    // POST /api/register
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/register - registers new user successfully")
    void register_returnsSuccess_whenNewUser() throws Exception {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    @DisplayName("POST /api/register - returns failure when username already exists")
    void register_returnsFailure_whenUsernameExists() throws Exception {
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        mockMvc.perform(post("/api/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/register - returns failure when email already exists")
    void register_returnsFailure_whenEmailExists() throws Exception {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /api/register - password is encoded before saving")
    void register_encodesPassword_beforeSaving() throws Exception {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$10$hashedvalue");
        when(userRepository.save(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "plaintext"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(passwordEncoder).encode("plaintext");
    }

    @Test
    @DisplayName("POST /api/register - new user is assigned ROLE_USER by default")
    void register_assignsRoleUser_byDefault() throws Exception {
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        // Capture what gets saved
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        mockMvc.perform(post("/api/register")
                        .param("username", "john_doe")
                        .param("email", "john@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userRepository).save(argThat(u -> u.getRole() == Role.ROLE_USER));
    }

    // -----------------------------------------------------------------------
    // POST /api/login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/login - returns success with valid credentials")
    void login_returnsSuccess_withValidCredentials() throws Exception {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        mockMvc.perform(post("/api/login")
                        .param("username", "john_doe")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /api/login - returns failure when username not found")
    void login_returnsFailure_whenUsernameNotFound() throws Exception {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/login")
                        .param("username", "ghost")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/login - returns failure when password is wrong")
    void login_returnsFailure_whenPasswordIsWrong() throws Exception {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        mockMvc.perform(post("/api/login")
                        .param("username", "john_doe")
                        .param("password", "wrongpassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}