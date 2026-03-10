package com.mgd.bookstore.dto;

import com.mgd.bookstore.model.Order;
import com.mgd.bookstore.model.OrderStatus;

import java.time.LocalDateTime;

public class OrderSummaryDTO {
    private Long orderId;
    private LocalDateTime orderDate;
    private OrderStatus status;

    // getters and setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}