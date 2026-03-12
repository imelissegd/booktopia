package com.mgd.bookstore.service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.mgd.bookstore.dto.CartItemDTO;
import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.dto.OrderSummaryDTO;
import com.mgd.bookstore.dto.UserDTO;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.mgd.bookstore.model.Role;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setEnabled(user.isEnabled());

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

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.isBlank()) return userRepository.findAll();
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    }

    public User createUser(User user) {
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User updated) {
        return userRepository.findById(id).map(existing -> {
            existing.setUsername(updated.getUsername());
            existing.setEmail(updated.getEmail());
            existing.setRole(updated.getRole());
            if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(updated.getPassword())); // ← encode here
            }
            return userRepository.save(existing);
        });
    }

    public Optional<User> deactivateUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setEnabled(false);
            return userRepository.save(user);
        });
    }

    public Optional<User> reactivateUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setEnabled(true);
            return userRepository.save(user);
        });
    }

    public Optional<User> changeRole(Long id, String roleName) {
        return userRepository.findById(id).map(user -> {
            if (roleName.equals("ROLE_USER")) {
                long adminCount = userRepository.findAll().stream()
                        .filter(u -> u.getRole().name().equals("ROLE_ADMIN") && u.getId() != id)
                        .count();
                if (adminCount == 0) throw new IllegalStateException("Should have at least one admin");
            }
            user.setRole(Role.valueOf(roleName));
            return userRepository.save(user);
        });
    }
}