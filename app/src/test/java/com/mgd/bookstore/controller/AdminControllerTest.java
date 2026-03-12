package com.mgd.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgd.bookstore.dto.UserDTO;
import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Category;
import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.repository.BookRepository;
import com.mgd.bookstore.repository.UserRepository;
import com.mgd.bookstore.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Book book;
    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();

        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setPrice(new BigDecimal("39.99"));
        book.setDescription("Agile software craftsmanship");
        book.setCategories(List.of(Category.TECHNOLOGY));
        book.setActive(true);
        book.setStock(10);

        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setOrders(new ArrayList<>());

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john_doe");
        userDTO.setEmail("john@example.com");
        userDTO.setRole("ROLE_USER");
        userDTO.setEnabled(true);
        userDTO.setOrders(List.of());
    }

    // -----------------------------------------------------------------------
    // GET /api/admin/users
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/admin/users - returns 200 with all user DTOs")
    void getAllUsers_returns200_withAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(user));
        when(userService.mapToDTO(user)).thenReturn(userDTO);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("john_doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[0].role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/admin/users - returns empty list when no users")
    void getAllUsers_returnsEmptyList_whenNoUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -----------------------------------------------------------------------
    // POST /api/admin/books
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/admin/books - saves and returns the new book")
    void addBook_savesAndReturnsBook() throws Exception {
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        mockMvc.perform(post("/api/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert Martin"))
                .andExpect(jsonPath("$.price").value(39.99));
    }

    // -----------------------------------------------------------------------
    // PUT /api/admin/books/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/admin/books/{id} - updates and returns book")
    void editBook_updatesAndReturnsBook() throws Exception {
        Book updatedBook = new Book();
        updatedBook.setTitle("Clean Architecture");
        updatedBook.setAuthor("Robert Martin");
        updatedBook.setPrice(new BigDecimal("44.99"));
        updatedBook.setDescription("A craftsman's guide to structure");
        updatedBook.setCategories(List.of(Category.TECHNOLOGY));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/admin/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Architecture"))
                .andExpect(jsonPath("$.price").value(44.99));
    }

    @Test
    @DisplayName("PUT /api/admin/books/{id} - returns 404 when book not found")
    void editBook_throwsException_whenBookNotFound() throws Exception {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/books/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/admin/books/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/admin/books/{id} - deletes and returns confirmation message")
    void deleteBook_deletesAndReturnsMessage() throws Exception {
        doNothing().when(bookRepository).deleteById(1L);

        mockMvc.perform(delete("/api/admin/books/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Book deleted successfully"));

        verify(bookRepository).deleteById(1L);
    }

    // -----------------------------------------------------------------------
    // POST /api/admin/users/{id}/promote
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/admin/users/{id}/promote - promotes user to ADMIN")
    void promoteToAdmin_promotesUserToAdmin() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/admin/users/1/promote"))
                .andExpect(status().isOk())
                .andExpect(content().string("User john_doe promoted to ADMIN"));

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/promote - returns 404 when user not found")
    void promoteToAdmin_throwsException_whenUserNotFound() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/users/99/promote"))
                .andExpect(status().isNotFound());
    }
}