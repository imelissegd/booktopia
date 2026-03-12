package com.mgd.bookstore.dto;

import java.util.List;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean enabled;
    private List<OrderSummaryDTO> orders;
    private CartResponseDTO cart;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<OrderSummaryDTO> getOrders() { return orders; }
    public void setOrders(List<OrderSummaryDTO> orders) { this.orders = orders; }

    public CartResponseDTO getCart() { return cart; }
    public void setCart(CartResponseDTO cart) { this.cart = cart; }
}