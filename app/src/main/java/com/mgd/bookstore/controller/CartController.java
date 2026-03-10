package com.mgd.bookstore.controller;

import com.mgd.bookstore.dto.AddToCartRequestDTO;
import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.dto.UpdateCartItemRequestDTO;
import com.mgd.bookstore.model.Cart;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.service.CartService;
import com.mgd.bookstore.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<CartResponseDTO> getCart(@PathVariable String username) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        CartResponseDTO dto = cartService.getCartDTO(userOpt.get());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{username}/add")
    public ResponseEntity<CartResponseDTO> addToCart(@PathVariable String username,
                                                     @RequestBody AddToCartRequestDTO request) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            CartResponseDTO cartDTO = cartService.addItemToCart(userOpt.get(), request.getBookId(), request.getQuantity());
            return ResponseEntity.ok(cartDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update quantity of a specific cart item.
     * If the new quantity is 0 or less, the item is removed automatically.
     */
    @PatchMapping("/{username}/update/{cartItemId}")
    public ResponseEntity<CartResponseDTO> updateCartItem(@PathVariable String username,
                                                          @PathVariable Long cartItemId,
                                                          @RequestBody UpdateCartItemRequestDTO request) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            CartResponseDTO cartDTO = cartService.updateCartItemQuantity(userOpt.get(), cartItemId, request.getQuantity());
            return ResponseEntity.ok(cartDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a specific cart item by its ID.
     */
    @DeleteMapping("/{username}/remove/{cartItemId}")
    public ResponseEntity<CartResponseDTO> removeCartItem(@PathVariable String username,
                                                          @PathVariable Long cartItemId) {
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        try {
            CartResponseDTO cartDTO = cartService.removeCartItem(userOpt.get(), cartItemId);
            return ResponseEntity.ok(cartDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}