package com.mgd.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgd.bookstore.dto.*;
import com.mgd.bookstore.model.OrderStatus;
import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.service.OrderService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;
    private OrderResponseDTO orderResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setRole(Role.ROLE_USER);

        OrderItemDTO item = new OrderItemDTO();
        item.setBookId(10L);
        item.setBookTitle("Clean Code");
        item.setAuthor("Robert Martin");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("39.99"));
        item.setTotalPrice(new BigDecimal("79.98"));

        orderResponseDTO = new OrderResponseDTO();
        orderResponseDTO.setOrderId(1L);
        orderResponseDTO.setUsername("john_doe");
        orderResponseDTO.setOrderDate(LocalDateTime.of(2024, 6, 1, 10, 0));
        orderResponseDTO.setStatus(OrderStatus.PENDING);
        orderResponseDTO.setItems(List.of(item));
        orderResponseDTO.setOrderTotal(new BigDecimal("79.98"));
    }

    // -----------------------------------------------------------------------
    // POST /api/orders/{username}/checkout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/orders/{username}/checkout - returns 200 with order DTO")
    void checkout_returns200_withOrderDTO() throws Exception {
        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(orderService.checkout(eq(user), any())).thenReturn(orderResponseDTO);

        mockMvc.perform(post("/api/orders/john_doe/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderTotal").value(79.98));
    }

    @Test
    @DisplayName("POST /api/orders/{username}/checkout - returns 200 with selected cart item IDs")
    void checkout_returns200_withSelectedCartItemIds() throws Exception {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setCartItemIds(List.of(100L));

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(orderService.checkout(user, List.of(100L))).thenReturn(orderResponseDTO);

        mockMvc.perform(post("/api/orders/john_doe/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    @DisplayName("POST /api/orders/{username}/checkout - returns 404 when user not found")
    void checkout_returns404_whenUserNotFound() throws Exception {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/orders/ghost/checkout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /api/orders/{username}/buy-now
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/orders/{username}/buy-now - returns 200 with order")
    void buyNow_returns200_withOrder() throws Exception {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setBookId(10L);
        request.setQuantity(1);

        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(orderService.buyNow(user, 10L, 1)).thenReturn(orderResponseDTO);

        mockMvc.perform(post("/api/orders/john_doe/buy-now")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/orders/{username}/buy-now - returns 404 when user not found")
    void buyNow_returns404_whenUserNotFound() throws Exception {
        AddToCartRequestDTO request = new AddToCartRequestDTO();
        request.setBookId(10L);
        request.setQuantity(1);

        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/orders/ghost/buy-now")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/orders/{username}/history
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/orders/{username}/history - returns 200 with order list")
    void getOrderHistory_returns200_withOrderList() throws Exception {
        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(orderService.getOrdersByUserDTO(user)).thenReturn(List.of(orderResponseDTO));

        mockMvc.perform(get("/api/orders/john_doe/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/orders/{username}/history - returns empty list when no orders")
    void getOrderHistory_returnsEmptyList_whenNoOrders() throws Exception {
        when(userService.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(orderService.getOrdersByUserDTO(user)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/john_doe/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/orders/{username}/history - returns 404 when user not found")
    void getOrderHistory_returns404_whenUserNotFound() throws Exception {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/ghost/history"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PUT /api/orders/{orderId}/status
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/orders/{orderId}/status - returns 200 with updated order")
    void updateOrderStatus_returns200_withUpdatedOrder() throws Exception {
        orderResponseDTO.setStatus(OrderStatus.SHIPPED);
        when(orderService.updateOrderStatus(1L, OrderStatus.SHIPPED)).thenReturn(orderResponseDTO);

        mockMvc.perform(put("/api/orders/1/status")
                        .param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("PUT /api/orders/{orderId}/status - returns 404 when order not found")
    void updateOrderStatus_returns404_whenOrderNotFound() throws Exception {
        when(orderService.updateOrderStatus(999L, OrderStatus.PAID))
                .thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(put("/api/orders/999/status")
                        .param("status", "PAID"))
                .andExpect(status().isNotFound());
    }
}