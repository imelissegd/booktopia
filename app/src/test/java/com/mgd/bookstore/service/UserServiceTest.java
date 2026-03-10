package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.CartResponseDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
    // mapToDTO - basic user with no cart, no orders
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("mapToDTO - maps basic user fields correctly")
    void mapToDTO_mapsBasicUserFields() {
        UserDTO dto = userService.mapToDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("john_doe");
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getOrders()).isEmpty();
        assertThat(dto.getCart()).isNull();
    }

    @Test
    @DisplayName("mapToDTO - maps orders correctly")
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
}