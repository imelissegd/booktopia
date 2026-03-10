package com.mgd.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgd.bookstore.dto.AddToCartRequestDTO;
import com.mgd.bookstore.dto.CartItemDTO;
import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.dto.UpdateCartItemRequestDTO;
import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.service.CartService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartController Tests")
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CartController cartController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;
    private CartResponseDTO cartResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
        objectMapper = new ObjectMapper();

        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setRole(Role.ROLE_USER);

        CartItemDTO item = new CartItemDTO();
        item.setCartItemId(100L);
        item.setBookId(10L);
        item.setBookTitle("Clean Code");
        item.setAuthor("Robert Martin");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("39.99"));
        item.setTotalPrice(new BigDecimal("79.98"));

        cartResponseDTO = new CartResponseDTO();
        cartResponseDTO.setUserId(1L);
        cartResponseDTO.setUsername("john_doe");
        cartResponseDTO.setItems(List.of(item));
    }

    // -----------------------------------------------------------------------
    // GET /api/cart/{username}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/cart/{username} - returns 200 with cart DTO")
    void getCart_returns200_withCartDTO() throws Exception {
        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.getCartDTO(user)).thenReturn(cartResponseDTO);

        mockMvc.perform(get("/api/cart/john_doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Clean Code"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("GET /api/cart/{username} - returns 404 when user not found")
    void getCart_returns404_whenUserNotFound() throws Exception {
        when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cart/unknown"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /api/cart/{username}/add
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/cart/{username}/add - returns 200 with updated cart")
    void addToCart_returns200_withUpdatedCart() throws Exception {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setBookId(10L);
        request.setQuantity(2);

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.addItemToCart(user, 10L, 2)).thenReturn(cartResponseDTO);

        mockMvc.perform(post("/api/cart/john_doe/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/cart/{username}/add - returns 404 when user not found")
    void addToCart_returns404_whenUserNotFound() throws Exception {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setBookId(10L);
        request.setQuantity(1);

        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/cart/ghost/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/cart/{username}/add - returns 400 when service throws RuntimeException")
    void addToCart_returns400_whenServiceThrows() throws Exception {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setBookId(999L);
        request.setQuantity(1);

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.addItemToCart(user, 999L, 1))
                .thenThrow(new RuntimeException("Book not found"));

        mockMvc.perform(post("/api/cart/john_doe/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/cart/{username}/update/{cartItemId}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/cart/{username}/update/{cartItemId} - returns 200 with updated cart")
    void updateCartItem_returns200_withUpdatedCart() throws Exception {
        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO();
        request.setQuantity(5);

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.updateCartItemQuantity(user, 100L, 5)).thenReturn(cartResponseDTO);

        mockMvc.perform(patch("/api/cart/john_doe/update/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    @DisplayName("PATCH /api/cart/{username}/update/{cartItemId} - returns 404 when user not found")
    void updateCartItem_returns404_whenUserNotFound() throws Exception {
        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO();
        request.setQuantity(3);

        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/cart/ghost/update/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/cart/{username}/update/{cartItemId} - returns 400 when service throws")
    void updateCartItem_returns400_whenServiceThrows() throws Exception {
        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO();
        request.setQuantity(3);

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.updateCartItemQuantity(user, 999L, 3))
                .thenThrow(new RuntimeException("Cart item not found"));

        mockMvc.perform(patch("/api/cart/john_doe/update/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/cart/{username}/remove/{cartItemId}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/cart/{username}/remove/{cartItemId} - returns 200 after removal")
    void removeCartItem_returns200_afterRemoval() throws Exception {
        CartResponseDTO emptyCart = new CartResponseDTO();
        emptyCart.setUserId(1L);
        emptyCart.setUsername("john_doe");
        emptyCart.setItems(List.of());

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.removeCartItem(user, 100L)).thenReturn(emptyCart);

        mockMvc.perform(delete("/api/cart/john_doe/remove/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/cart/{username}/remove/{cartItemId} - returns 404 when user not found")
    void removeCartItem_returns404_whenUserNotFound() throws Exception {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/cart/ghost/remove/100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/cart/{username}/remove/{cartItemId} - returns 400 when service throws")
    void removeCartItem_returns400_whenServiceThrows() throws Exception {
        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(cartService.removeCartItem(user, 999L))
                .thenThrow(new RuntimeException("Cart item not found"));

        mockMvc.perform(delete("/api/cart/john_doe/remove/999"))
                .andExpect(status().isBadRequest());
    }
}