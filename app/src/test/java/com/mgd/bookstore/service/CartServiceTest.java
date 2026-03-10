package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.CartResponseDTO;
import com.mgd.bookstore.model.*;
import com.mgd.bookstore.repository.BookRepository;
import com.mgd.bookstore.repository.CartItemRepository;
import com.mgd.bookstore.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Book book;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setRole(Role.ROLE_USER);

        book = new Book();
        book.setId(10L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setPrice(new BigDecimal("39.99"));

        cart = new Cart(user);
        cart.setId(1L);

        cartItem = new CartItem();
        cartItem.setId(100L);
        cartItem.setBook(book);
        cartItem.setQuantity(2);
        cartItem.setCart(cart);
    }

    // -----------------------------------------------------------------------
    // getCartByUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getCartByUser - returns cart when exists")
    void getCartByUser_returnsCart_whenExists() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        Optional<Cart> result = cartService.getCartByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCartByUser - returns empty when no cart")
    void getCartByUser_returnsEmpty_whenNoCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        Optional<Cart> result = cartService.getCartByUser(user);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getCartDTO
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getCartDTO - returns DTO for existing cart")
    void getCartDTO_returnsDTO_forExistingCart() {
        cart.setItems(List.of(cartItem));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponseDTO result = cartService.getCartDTO(user);

        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualByComparingTo("39.99");
        assertThat(result.getItems().get(0).getTotalPrice()).isEqualByComparingTo("79.98");
    }

    @Test
    @DisplayName("getCartDTO - creates new cart when none exists")
    void getCartDTO_createsNewCart_whenNoneExists() {
        Cart newCart = new Cart(user);
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(newCart);

        CartResponseDTO result = cartService.getCartDTO(user);

        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getItems()).isEmpty();
        verify(cartRepository).saveAndFlush(any(Cart.class));
    }

    // -----------------------------------------------------------------------
    // addItemToCart
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("addItemToCart - adds new item to empty cart")
    void addItemToCart_addsNewItem_toEmptyCart() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDTO result = cartService.addItemToCart(user, 10L, 1);

        assertThat(result).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getBook()).isEqualTo(book);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("addItemToCart - increments quantity for existing book in cart")
    void addItemToCart_incrementsQuantity_forExistingBook() {
        cart.getItems().add(cartItem); // cartItem already has quantity 2
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.addItemToCart(user, 10L, 3);

        assertThat(cartItem.getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    @DisplayName("addItemToCart - creates cart when user has none")
    void addItemToCart_createsCart_whenNoneExists() {
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        Cart newCart = new Cart(user);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        CartResponseDTO result = cartService.addItemToCart(user, 10L, 1);

        assertThat(result).isNotNull();
        verify(cartRepository, times(2)).save(any(Cart.class)); // once to create, once to save item
    }

    @Test
    @DisplayName("addItemToCart - throws exception when book not found")
    void addItemToCart_throwsException_whenBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(user, 999L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("addItemToCart - throws exception when bookId is null")
    void addItemToCart_throwsException_whenBookIdIsNull() {
        assertThatThrownBy(() -> cartService.addItemToCart(user, null, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid bookId or quantity");
    }

    @Test
    @DisplayName("addItemToCart - throws exception when quantity is zero")
    void addItemToCart_throwsException_whenQuantityIsZero() {
        assertThatThrownBy(() -> cartService.addItemToCart(user, 10L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid bookId or quantity");
    }

    @Test
    @DisplayName("addItemToCart - throws exception when quantity is negative")
    void addItemToCart_throwsException_whenQuantityIsNegative() {
        assertThatThrownBy(() -> cartService.addItemToCart(user, 10L, -1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid bookId or quantity");
    }

    // -----------------------------------------------------------------------
    // updateCartItemQuantity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateCartItemQuantity - updates quantity of existing item")
    void updateCartItemQuantity_updatesQuantity() {
        cart.getItems().add(cartItem);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDTO result = cartService.updateCartItemQuantity(user, 100L, 5);

        assertThat(cartItem.getQuantity()).isEqualTo(5);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateCartItemQuantity - removes item when quantity is zero")
    void updateCartItemQuantity_removesItem_whenQuantityIsZero() {
        cart.getItems().add(cartItem);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.updateCartItemQuantity(user, 100L, 0);

        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("updateCartItemQuantity - removes item when quantity is negative")
    void updateCartItemQuantity_removesItem_whenQuantityIsNegative() {
        cart.getItems().add(cartItem);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.updateCartItemQuantity(user, 100L, -5);

        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("updateCartItemQuantity - throws exception when cart not found")
    void updateCartItemQuantity_throwsException_whenCartNotFound() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(user, 100L, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    @DisplayName("updateCartItemQuantity - throws exception when cart item not found")
    void updateCartItemQuantity_throwsException_whenCartItemNotFound() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        // cart has no items

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(user, 999L, 3))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart item not found");
    }

    // -----------------------------------------------------------------------
    // removeCartItem
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("removeCartItem - removes item from cart")
    void removeCartItem_removesItem_successfully() {
        cart.getItems().add(cartItem);
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDTO result = cartService.removeCartItem(user, 100L);

        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).delete(cartItem);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("removeCartItem - throws exception when cart not found")
    void removeCartItem_throwsException_whenCartNotFound() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeCartItem(user, 100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    @DisplayName("removeCartItem - throws exception when cart item not found")
    void removeCartItem_throwsException_whenCartItemNotFound() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        // empty cart

        assertThatThrownBy(() -> cartService.removeCartItem(user, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart item not found");
    }
}