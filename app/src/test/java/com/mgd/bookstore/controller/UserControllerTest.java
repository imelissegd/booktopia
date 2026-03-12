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
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
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
        user.setEnabled(true);
        user.setOrders(new ArrayList<>());

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john_doe");
        userDTO.setEmail("john@example.com");
        userDTO.setRole("ROLE_USER");
        userDTO.setEnabled(true);
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
    // POST /api/users (createUser - admin)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/users - returns 200 with created user")
    void createUser_returns200_withCreatedUser() throws Exception {
        when(userService.createUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"));
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
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/users/{username} - returns 404 when user not found")
    void getUser_returns404_whenUserNotFound() throws Exception {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/ghost"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/users/id/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/users/id/{id} - returns 200 with user DTO")
    void getUserById_returns200_withUserDTO() throws Exception {
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/id/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("GET /api/users/id/{id} - returns 404 when user not found")
    void getUserById_returns404_whenNotFound() throws Exception {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/id/99"))
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
        admin.setEnabled(true);
        admin.setOrders(new ArrayList<>());

        UserDTO adminDTO = new UserDTO();
        adminDTO.setId(2L);
        adminDTO.setUsername("admin");
        adminDTO.setEmail("admin@example.com");
        adminDTO.setRole("ROLE_ADMIN");

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

    @Test
    @DisplayName("GET /api/users?search= - returns filtered users matching search query")
    void getAllUsers_withSearch_returnsFilteredUsers() throws Exception {
        when(userService.searchUsers("john")).thenReturn(List.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users").param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("john_doe"));
    }

    // -----------------------------------------------------------------------
    // PUT /api/users/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/users/{id} - returns 200 with updated user DTO")
    void updateUser_returns200_withUpdatedDTO() throws Exception {
        User updated = new User();
        updated.setUsername("john_updated");
        updated.setEmail("updated@example.com");
        updated.setRole(Role.ROLE_USER);

        UserDTO updatedDTO = new UserDTO();
        updatedDTO.setId(1L);
        updatedDTO.setUsername("john_updated");
        updatedDTO.setEmail("updated@example.com");

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(Optional.of(updated));
        when(userService.mapToDTO(updated)).thenReturn(updatedDTO);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_updated"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - returns 404 when user not found")
    void updateUser_returns404_whenNotFound() throws Exception {
        when(userService.updateUser(eq(99L), any(User.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/users/{id}/deactivate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/users/{id}/deactivate - returns 200 with deactivated user")
    void deactivateUser_returns200() throws Exception {
        user.setEnabled(false);
        userDTO.setEnabled(false);
        when(userService.deactivateUser(1L)).thenReturn(Optional.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/deactivate - returns 404 when user not found")
    void deactivateUser_returns404_whenNotFound() throws Exception {
        when(userService.deactivateUser(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/users/99/deactivate"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/users/{id}/reactivate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/users/{id}/reactivate - returns 200 with reactivated user")
    void reactivateUser_returns200() throws Exception {
        userDTO.setEnabled(true);
        when(userService.reactivateUser(1L)).thenReturn(Optional.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(patch("/api/users/1/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/reactivate - returns 404 when user not found")
    void reactivateUser_returns404_whenNotFound() throws Exception {
        when(userService.reactivateUser(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/users/99/reactivate"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/users/{id}/role
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/users/{id}/role - returns 200 when role changed successfully")
    void changeRole_returns200_whenSuccessful() throws Exception {
        userDTO.setRole("ROLE_ADMIN");
        when(userService.changeRole(1L, "ROLE_ADMIN")).thenReturn(Optional.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(patch("/api/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/role - returns 404 when user not found")
    void changeRole_returns404_whenNotFound() throws Exception {
        when(userService.changeRole(99L, "ROLE_ADMIN")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/users/99/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/role - returns 400 when demoting last admin")
    void changeRole_returns400_whenDemotingLastAdmin() throws Exception {
        when(userService.changeRole(1L, "ROLE_USER"))
                .thenThrow(new IllegalStateException("Should have at least one admin"));

        mockMvc.perform(patch("/api/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }
}