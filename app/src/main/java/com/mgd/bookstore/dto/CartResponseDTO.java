package com.mgd.bookstore.dto;

import java.util.List;

public class CartResponseDTO {
    private Long userId;
    private String username;
    private List<CartItemDTO> items;

    // getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items; }
}