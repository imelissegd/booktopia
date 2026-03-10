package com.mgd.bookstore.dto;

import java.util.List;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private List<OrderSummaryDTO> orders;
    private CartResponseDTO cart;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<OrderSummaryDTO> getOrders() { return orders; }
    public void setOrders(List<OrderSummaryDTO> orders) { this.orders = orders; }

    public CartResponseDTO getCart() { return cart; }

    public void setCart(CartResponseDTO cart) { this.cart = cart; }
}