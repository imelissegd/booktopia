package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.OrderItemDTO;
import com.mgd.bookstore.dto.OrderResponseDTO;
import com.mgd.bookstore.model.*;
import com.mgd.bookstore.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
    }

    public OrderResponseDTO mapOrderToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setUsername(order.getUser().getUsername());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());

        List<OrderItemDTO> items = order.getOrderItems().stream().map(item -> {
            OrderItemDTO iDto = new OrderItemDTO();
            iDto.setBookId(item.getBook().getId());
            iDto.setBookTitle(item.getBook().getTitle());
            iDto.setAuthor(item.getBook().getAuthor());
            iDto.setQuantity(item.getQuantity());
            iDto.setUnitPrice(item.getBook().getPrice());
            iDto.setTotalPrice(item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return iDto;
        }).toList();

        dto.setItems(items);
        dto.setOrderTotal(items.stream()
                .map(OrderItemDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        return dto;
    }

    @Transactional
    public OrderResponseDTO checkout(User user, List<Long> cartItemIds) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Determine which items to checkout
        List<CartItem> itemsToCheckout;
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            // Checkout all items
            itemsToCheckout = cart.getItems();
        } else {
            // Checkout only selected cart items
            itemsToCheckout = cart.getItems().stream()
                    .filter(item -> cartItemIds.contains(item.getId()))
                    .toList();

            if (itemsToCheckout.isEmpty()) {
                throw new RuntimeException("No selected items found in cart");
            }
        }

        // Create Order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = itemsToCheckout.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setBook(cartItem.getBook());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getBook().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItem.setOrder(order);
            return orderItem;
        }).toList();

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // Remove checked out items from cart
        cart.getItems().removeAll(itemsToCheckout);
        cartRepository.saveAndFlush(cart);

        return mapOrderToDTO(savedOrder);
    }

    @Transactional
    public OrderResponseDTO buyNow(User user, Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        OrderItem orderItem = new OrderItem();
        orderItem.setBook(book);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(book.getPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
        orderItem.setOrder(order);

        order.setOrderItems(List.of(orderItem));

        return mapOrderToDTO(orderRepository.save(order));
    }

    public List<OrderResponseDTO> getOrdersByUserDTO(User user) {
        return orderRepository.findByUser(user).stream()
                .map(this::mapOrderToDTO)
                .toList();
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return mapOrderToDTO(orderRepository.save(order));
    }
}