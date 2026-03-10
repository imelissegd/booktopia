package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.CartItemDTO;
import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.dto.OrderSummaryDTO;
import com.mgd.bookstore.dto.UserDTO;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user) {
        // You can hash the password here before saving
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        // map orders
        dto.setOrders(user.getOrders() == null ? List.of() :
                user.getOrders().stream().map(order -> {
                    OrderSummaryDTO oDto = new OrderSummaryDTO();
                    oDto.setOrderId(order.getId());
                    oDto.setOrderDate(order.getOrderDate());
                    oDto.setStatus(order.getStatus());
                    return oDto;
                }).toList()
        );

        // Map cart (if exists)
        if (user.getCart() != null) {
            CartResponseDTO cartDTO = new CartResponseDTO();
            cartDTO.setUserId(user.getId());
            cartDTO.setUsername(user.getUsername());
            cartDTO.setItems(user.getCart().getItems().stream().map(item -> {
                CartItemDTO itemDTO = new CartItemDTO();
                itemDTO.setBookId(item.getBook().getId());
                itemDTO.setBookTitle(item.getBook().getTitle());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getBook().getPrice()); // price per book
                itemDTO.setTotalPrice(item.getBook().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))); // quantity * unitPrice
                return itemDTO;
            }).toList());
            dto.setCart(cartDTO);
        }
        return dto;
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}