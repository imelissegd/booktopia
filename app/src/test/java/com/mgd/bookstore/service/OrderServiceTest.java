package com.mgd.bookstore.service;

import com.mgd.bookstore.dto.OrderResponseDTO;
import com.mgd.bookstore.model.*;
import com.mgd.bookstore.repository.BookRepository;
import com.mgd.bookstore.repository.CartRepository;
import com.mgd.bookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Book book;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setRole(Role.ROLE_USER);

        book = new Book();
        book.setId(10L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setPrice(new BigDecimal("39.99"));
        book.setCategories(List.of(Category.TECHNOLOGY));
        book.setStock(100); // sufficient stock by default

        cart = new Cart(user);
        cart.setId(1L);

        cartItem = new CartItem();
        cartItem.setId(100L);
        cartItem.setBook(book);
        cartItem.setQuantity(2);
        cartItem.setCart(cart);

        cart.setItems(new ArrayList<>(List.of(cartItem)));
    }

    // -----------------------------------------------------------------------
    // Helper to build a saved Order
    // -----------------------------------------------------------------------

    private Order buildSavedOrder(List<CartItem> items) {
        Order order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTransactionId("TXN001");

        List<OrderItem> orderItems = items.stream().map(ci -> {
            OrderItem oi = new OrderItem();
            oi.setId(1L);
            oi.setBook(ci.getBook());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getBook().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            oi.setOrder(order);
            return oi;
        }).toList();

        order.setOrderItems(orderItems);
        return order;
    }

    // -----------------------------------------------------------------------
    // checkout - all items
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("checkout - checks out all cart items when no IDs specified")
    void checkout_allItems_whenNoIdsSpecified() {
        Order savedOrder = buildSavedOrder(List.of(cartItem));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(cart);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        OrderResponseDTO result = orderService.checkout(user, null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getOrderTotal()).isEqualByComparingTo("79.98");
    }

    @Test
    @DisplayName("checkout - checks out selected items by cart item ID")
    void checkout_selectedItems_byCartItemIds() {
        Order savedOrder = buildSavedOrder(List.of(cartItem));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(cart);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        OrderResponseDTO result = orderService.checkout(user, List.of(100L));

        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("checkout - removes checked out items from cart")
    void checkout_removesItemsFromCart_afterCheckout() {
        Order savedOrder = buildSavedOrder(List.of(cartItem));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(cart);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        orderService.checkout(user, null);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).saveAndFlush(cart);
    }

    @Test
    @DisplayName("checkout - decrements book stock on checkout")
    void checkout_decrementsBookStock() {
        book.setStock(10);
        Order savedOrder = buildSavedOrder(List.of(cartItem)); // quantity = 2
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(cart);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        orderService.checkout(user, null);

        assertThat(book.getStock()).isEqualTo(8); // 10 - 2
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("checkout - throws exception when stock is insufficient")
    void checkout_throwsException_whenStockInsufficient() {
        book.setStock(1); // only 1 in stock, but cartItem quantity = 2
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(user, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("checkout - throws exception when cart not found")
    void checkout_throwsException_whenCartNotFound() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout(user, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    @DisplayName("checkout - throws exception when cart is empty")
    void checkout_throwsException_whenCartIsEmpty() {
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(user, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("checkout - throws exception when selected cart item IDs not in cart")
    void checkout_throwsException_whenSelectedItemsNotInCart() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout(user, List.of(999L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No selected items found in cart");
    }

    @Test
    @DisplayName("checkout - order is set to PENDING status")
    void checkout_orderStatus_isPending() {
        Order savedOrder = buildSavedOrder(List.of(cartItem));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(cart);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        OrderResponseDTO result = orderService.checkout(user, null);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("checkout - order includes transactionId")
    void checkout_orderIncludesTransactionId() {
        Order savedOrder = buildSavedOrder(List.of(cartItem));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(cartRepository.saveAndFlush(any(Cart.class))).thenReturn(cart);
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        OrderResponseDTO result = orderService.checkout(user, null);

        assertThat(result.getTransactionId()).isNotNull().isNotBlank();
    }

    // -----------------------------------------------------------------------
    // buyNow
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("buyNow - creates order directly for a single book")
    void buyNow_createsOrder_forSingleBook() {
        Order savedOrder = new Order();
        savedOrder.setId(2L);
        savedOrder.setUser(user);
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTransactionId("XYZ123");
        OrderItem oi = new OrderItem();
        oi.setBook(book);
        oi.setQuantity(3);
        oi.setPrice(book.getPrice().multiply(BigDecimal.valueOf(3)));
        oi.setOrder(savedOrder);
        savedOrder.setOrderItems(List.of(oi));

        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDTO result = orderService.buyNow(user, 10L, 3);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(result.getOrderTotal()).isEqualByComparingTo("119.97");
    }

    @Test
    @DisplayName("buyNow - decrements book stock")
    void buyNow_decrementsBookStock() {
        book.setStock(10);
        Order savedOrder = new Order();
        savedOrder.setId(2L);
        savedOrder.setUser(user);
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTransactionId("ABC999");
        OrderItem oi = new OrderItem();
        oi.setBook(book);
        oi.setQuantity(2);
        oi.setPrice(book.getPrice().multiply(BigDecimal.valueOf(2)));
        oi.setOrder(savedOrder);
        savedOrder.setOrderItems(List.of(oi));

        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.buyNow(user, 10L, 2);

        assertThat(book.getStock()).isEqualTo(8);
        verify(bookRepository).save(book);
    }

    @Test
    @DisplayName("buyNow - throws exception when stock is insufficient")
    void buyNow_throwsException_whenStockInsufficient() {
        book.setStock(1);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> orderService.buyNow(user, 10L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("buyNow - throws exception when book not found")
    void buyNow_throwsException_whenBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.buyNow(user, 999L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("buyNow - does not touch cart")
    void buyNow_doesNotTouchCart() {
        Order savedOrder = new Order();
        savedOrder.setId(2L);
        savedOrder.setUser(user);
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTransactionId("NOTOUCHCART");
        OrderItem oi = new OrderItem();
        oi.setBook(book);
        oi.setQuantity(1);
        oi.setPrice(book.getPrice());
        oi.setOrder(savedOrder);
        savedOrder.setOrderItems(List.of(oi));

        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.buyNow(user, 10L, 1);

        verify(cartRepository, never()).findByUser(any());
        verify(cartRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // getOrdersByUserDTO
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getOrdersByUserDTO - returns list of order DTOs for user")
    void getOrdersByUserDTO_returnsOrderDTOs() {
        Order order = buildSavedOrder(List.of(cartItem));
        when(orderRepository.findByUser(user)).thenReturn(List.of(order));

        List<OrderResponseDTO> result = orderService.getOrdersByUserDTO(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("getOrdersByUserDTO - returns empty list when user has no orders")
    void getOrdersByUserDTO_returnsEmpty_whenNoOrders() {
        when(orderRepository.findByUser(user)).thenReturn(List.of());

        List<OrderResponseDTO> result = orderService.getOrdersByUserDTO(user);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // updateOrderStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateOrderStatus - updates order status successfully")
    void updateOrderStatus_updatesStatus_successfully() {
        Order order = buildSavedOrder(List.of(cartItem));
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponseDTO result = orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateOrderStatus - throws exception when order not found")
    void updateOrderStatus_throwsException_whenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(999L, OrderStatus.PAID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    // -----------------------------------------------------------------------
    // mapOrderToDTO
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("mapOrderToDTO - correctly maps order total")
    void mapOrderToDTO_correctlyMapsOrderTotal() {
        Order order = buildSavedOrder(List.of(cartItem)); // 2 * 39.99 = 79.98

        OrderResponseDTO dto = orderService.mapOrderToDTO(order);

        assertThat(dto.getOrderTotal()).isEqualByComparingTo("79.98");
        assertThat(dto.getUsername()).isEqualTo("john_doe");
        assertThat(dto.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("mapOrderToDTO - maps transactionId correctly")
    void mapOrderToDTO_mapsTransactionId() {
        Order order = buildSavedOrder(List.of(cartItem));
        order.setTransactionId("TXN001");

        OrderResponseDTO dto = orderService.mapOrderToDTO(order);

        assertThat(dto.getTransactionId()).isEqualTo("TXN001");
    }

    @Test
    @DisplayName("mapOrderToDTO - maps each order item correctly")
    void mapOrderToDTO_mapsOrderItems_correctly() {
        Order order = buildSavedOrder(List.of(cartItem));

        OrderResponseDTO dto = orderService.mapOrderToDTO(order);

        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getBookTitle()).isEqualTo("Clean Code");
        assertThat(dto.getItems().get(0).getAuthor()).isEqualTo("Robert Martin");
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(dto.getItems().get(0).getUnitPrice()).isEqualByComparingTo("39.99");
        assertThat(dto.getItems().get(0).getTotalPrice()).isEqualByComparingTo("79.98");
    }
}