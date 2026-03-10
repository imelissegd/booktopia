package com.mgd.bookstore.dto;

import java.util.List;

public class CheckoutRequestDTO {
    private List<Long> cartItemIds;

    public List<Long> getCartItemIds() { return cartItemIds; }
    public void setCartItemIds(List<Long> cartItemIds) { this.cartItemIds = cartItemIds; }
}