package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.CartItemDTO;
import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.model.*;
import com.mgd.bookstore.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository,
                       BookRepository bookRepository,
                       CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public Optional<Cart> getCartByUser(User user) {
        return cartRepository.findByUser(user);
    }

    private CartResponseDTO mapCartToDTO(User user, Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setItems(cart.getItems().stream()
                .map(this::mapCartItemToDTO)
                .toList());
        return dto;
    }

    private CartItemDTO mapCartItemToDTO(CartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setCartItemId(item.getId());
        dto.setBookId(item.getBook().getId());
        dto.setBookTitle(item.getBook().getTitle());
        dto.setAuthor(item.getBook().getAuthor());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getBook().getPrice());
        dto.setTotalPrice(item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }

    public CartResponseDTO getCartDTO(User user) {
        Cart cart = getCartByUser(user)
                .orElseGet(() -> cartRepository.saveAndFlush(new Cart(user)));
        return mapCartToDTO(user, cart);
    }

    @Transactional
    public CartResponseDTO addItemToCart(User user, Long bookId, int quantity) {
        if (bookId == null || quantity <= 0) {
            throw new RuntimeException("Invalid bookId or quantity");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Cart cart = getCartByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));

        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setBook(book);
            newItem.setQuantity(quantity);
            newItem.setCart(cart);
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapCartToDTO(user, savedCart);
    }

    /**
     * Update the quantity of a cart item.
     * If quantity drops to 0 or below, the item is removed automatically.
     */
    @Transactional
    public CartResponseDTO updateCartItemQuantity(User user, Long cartItemId, int newQuantity) {
        Cart cart = getCartByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (newQuantity <= 0) {
            // Treat as removal
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(newQuantity);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapCartToDTO(user, savedCart);
    }

    /**
     * Remove a cart item by its ID.
     */
    @Transactional
    public CartResponseDTO removeCartItem(User user, Long cartItemId) {
        Cart cart = getCartByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        Cart savedCart = cartRepository.save(cart);
        return mapCartToDTO(user, savedCart);
    }
}