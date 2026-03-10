package com.mgd.bookstore.controller;

import com.mgd.bookstore.dto.*;
import com.mgd.bookstore.model.Order;
import com.mgd.bookstore.model.OrderStatus;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.service.OrderService;
import com.mgd.bookstore.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    //  Checkout: Convert user's cart (or select items) into an order
    @PostMapping("/{username}/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(
            @PathVariable String username,
            @RequestBody(required = false) CheckoutRequestDTO request) {

        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(
                        orderService.checkout(user,
                                request != null ? request.getCartItemIds() : null)
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{username}/buy-now")
    public ResponseEntity<OrderResponseDTO> buyNow(@PathVariable String username,
                                                   @RequestBody AddToCartRequestDTO request) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(orderService.buyNow(user, request.getBookId(), request.getQuantity())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{username}/history")
    public ResponseEntity<List<OrderResponseDTO>> getOrderHistory(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(user -> ResponseEntity.ok(orderService.getOrdersByUserDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Update order status (admin or system)
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(@PathVariable Long orderId,
                                                              @RequestParam OrderStatus status) {
        try {
            return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}