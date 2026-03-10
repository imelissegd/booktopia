package com.mgd.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgd.bookstore.dto.UserDTO;
import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("secret");
        user.setRole(Role.ROLE_USER);
        user.setOrders(new ArrayList<>());

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john_doe");
        userDTO.setEmail("john@example.com");
        userDTO.setOrders(List.of());
        userDTO.setCart(null);
    }

    // -----------------------------------------------------------------------
    // POST /api/users/register
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/users/register - returns 200 with registered user")
    void register_returns200_withRegisteredUser() throws Exception {
        when(userService.registerUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    // -----------------------------------------------------------------------
    // GET /api/users/{username}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users/{username} - returns 200 with user DTO")
    void getUser_returns200_withUserDTO() throws Exception {
        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/john_doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /api/users/{username} - returns 404 when user not found")
    void getUser_returns404_whenUserNotFound() throws Exception {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/ghost"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/users
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users - returns 200 with all user DTOs")
    void getAllUsers_returns200_withAllUserDTOs() throws Exception {
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setOrders(new ArrayList<>());

        UserDTO adminDTO = new UserDTO();
        adminDTO.setId(2L);
        adminDTO.setUsername("admin");
        adminDTO.setEmail("admin@example.com");

        when(userService.getAllUsers()).thenReturn(List.of(user, admin));
        when(userService.mapToDTO(user)).thenReturn(userDTO);
        when(userService.mapToDTO(admin)).thenReturn(adminDTO);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("john_doe"))
                .andExpect(jsonPath("$[1].username").value("admin"));
    }

    @Test
    @DisplayName("GET /api/users - returns empty list when no users")
    void getAllUsers_returnsEmptyList_whenNoUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}