package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.dto.OrderSummaryDTO;
import com.mgd.bookstore.dto.UserDTO;
import com.mgd.bookstore.model.*;
import com.mgd.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("hashed_password");
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setOrders(new ArrayList<>());
        user.setCart(null);
    }

    // -----------------------------------------------------------------------
    // registerUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("registerUser - saves and returns the user")
    void registerUser_savesAndReturnsUser() {
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.registerUser(user);

        assertThat(result).isEqualTo(user);
        assertThat(result.getUsername()).isEqualTo("john_doe");
        verify(userRepository).save(user);
    }

    // -----------------------------------------------------------------------
    // createUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createUser - sets enabled to true and saves")
    void createUser_setsEnabledAndSaves() {
        user.setEnabled(false);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createUser(user);

        assertThat(result.isEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    // -----------------------------------------------------------------------
    // findByUsername
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByUsername - returns user when found")
    void findByUsername_returnsUser_whenFound() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("john_doe");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("findByUsername - returns empty Optional when not found")
    void findByUsername_returnsEmpty_whenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("ghost");

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // findByEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findByEmail - returns user when found")
    void findByEmail_returnsUser_whenFound() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("john@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("findByEmail - returns empty Optional when not found")
    void findByEmail_returnsEmpty_whenNotFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getUserById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getUserById - returns user when found")
    void getUserById_returnsUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("getUserById - returns empty Optional when not found")
    void getUserById_returnsEmpty_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserById(99L);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getAllUsers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllUsers - returns all users")
    void getAllUsers_returnsAllUsers() {
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ROLE_ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(user, admin));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getAllUsers - returns empty list when no users")
    void getAllUsers_returnsEmpty_whenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // searchUsers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("searchUsers - returns matching users by username or email")
    void searchUsers_returnsMatchingUsers() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("john", "john"))
                .thenReturn(List.of(user));

        List<User> result = userService.searchUsers("john");

        assertThat(result).containsExactly(user);
    }

    @Test
    @DisplayName("searchUsers - returns all users when query is blank")
    void searchUsers_returnsAllUsers_whenQueryIsBlank() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.searchUsers("  ");

        assertThat(result).containsExactly(user);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("searchUsers - returns all users when query is null")
    void searchUsers_returnsAllUsers_whenQueryIsNull() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.searchUsers(null);

        assertThat(result).containsExactly(user);
    }

    // -----------------------------------------------------------------------
    // updateUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateUser - updates fields and returns updated user")
    void updateUser_updatesFields() {
        User updated = new User();
        updated.setUsername("john_updated");
        updated.setEmail("new@example.com");
        updated.setRole(Role.ROLE_ADMIN);
        updated.setPassword(null); // no password change

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUser(1L, updated);

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john_updated");
        assertThat(result.get().getEmail()).isEqualTo("new@example.com");
        assertThat(result.get().getRole()).isEqualTo(Role.ROLE_ADMIN);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser - encodes password when non-blank password provided")
    void updateUser_encodesPassword_whenProvided() {
        User updated = new User();
        updated.setUsername("john_doe");
        updated.setEmail("john@example.com");
        updated.setRole(Role.ROLE_USER);
        updated.setPassword("newplaintext");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newplaintext")).thenReturn("newhashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.updateUser(1L, updated);

        assertThat(result).isPresent();
        assertThat(result.get().getPassword()).isEqualTo("newhashed");
        verify(passwordEncoder).encode("newplaintext");
    }

    @Test
    @DisplayName("updateUser - returns empty Optional when user not found")
    void updateUser_returnsEmpty_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.updateUser(99L, new User());

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // deactivateUser / reactivateUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deactivateUser - sets enabled to false")
    void deactivateUser_setsEnabledFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.deactivateUser(1L);

        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("deactivateUser - returns empty Optional when user not found")
    void deactivateUser_returnsEmpty_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.deactivateUser(99L)).isEmpty();
    }

    @Test
    @DisplayName("reactivateUser - sets enabled to true")
    void reactivateUser_setsEnabledTrue() {
        user.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.reactivateUser(1L);

        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("reactivateUser - returns empty Optional when user not found")
    void reactivateUser_returnsEmpty_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.reactivateUser(99L)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // changeRole
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("changeRole - changes user role to ROLE_ADMIN")
    void changeRole_changesRoleToAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.changeRole(1L, "ROLE_ADMIN");

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("changeRole - throws IllegalStateException when demoting last admin")
    void changeRole_throwsException_whenDemotingLastAdmin() {
        user.setRole(Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // No other admins exist
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThatThrownBy(() -> userService.changeRole(1L, "ROLE_USER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Should have at least one admin");
    }

    @Test
    @DisplayName("changeRole - allows demoting admin when another admin exists")
    void changeRole_allowsDemotion_whenAnotherAdminExists() {
        user.setRole(Role.ROLE_ADMIN);
        User anotherAdmin = new User();
        anotherAdmin.setId(2L);
        anotherAdmin.setRole(Role.ROLE_ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findAll()).thenReturn(List.of(user, anotherAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.changeRole(1L, "ROLE_USER");

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("changeRole - returns empty Optional when user not found")
    void changeRole_returnsEmpty_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = userService.changeRole(99L, "ROLE_ADMIN");

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // mapToDTO
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("mapToDTO - maps basic user fields correctly including role and enabled")
    void mapToDTO_mapsBasicUserFields() {
        UserDTO dto = userService.mapToDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("john_doe");
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getRole()).isEqualTo("ROLE_USER");
        assertThat(dto.isEnabled()).isTrue();
        assertThat(dto.getOrders()).isEmpty();
        assertThat(dto.getCart()).isNull();
    }

    @Test
    @DisplayName("mapToDTO - maps orders to OrderSummaryDTO correctly")
    void mapToDTO_mapsOrders_whenUserHasOrders() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderDate(LocalDateTime.of(2024, 1, 15, 10, 0));
        order.setStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setOrderItems(List.of());
        user.setOrders(List.of(order));

        UserDTO dto = userService.mapToDTO(user);

        assertThat(dto.getOrders()).hasSize(1);
        assertThat(dto.getOrders().get(0).getOrderId()).isEqualTo(10L);
        assertThat(dto.getOrders().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("mapToDTO - maps cart correctly when user has a cart")
    void mapToDTO_mapsCart_whenUserHasCart() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setPrice(new BigDecimal("39.99"));

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setBook(book);
        cartItem.setQuantity(2);

        Cart cart = new Cart(user);
        cart.setId(1L);
        cart.setItems(List.of(cartItem));
        user.setCart(cart);

        UserDTO dto = userService.mapToDTO(user);

        assertThat(dto.getCart()).isNotNull();
        CartResponseDTO cartDTO = dto.getCart();
        assertThat(cartDTO.getUsername()).isEqualTo("john_doe");
        assertThat(cartDTO.getItems()).hasSize(1);
        assertThat(cartDTO.getItems().get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(cartDTO.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cartDTO.getItems().get(0).getUnitPrice()).isEqualByComparingTo("39.99");
        assertThat(cartDTO.getItems().get(0).getTotalPrice()).isEqualByComparingTo("79.98");
    }

    @Test
    @DisplayName("mapToDTO - null orders mapped to empty list")
    void mapToDTO_nullOrders_mappedToEmptyList() {
        user.setOrders(null);

        UserDTO dto = userService.mapToDTO(user);

        assertThat(dto.getOrders()).isEmpty();
    }

    @Test
    @DisplayName("mapToDTO - maps null role as null string")
    void mapToDTO_nullRole_mappedAsNull() {
        user.setRole(null);

        UserDTO dto = userService.mapToDTO(user);

        assertThat(dto.getRole()).isNull();
    }
}