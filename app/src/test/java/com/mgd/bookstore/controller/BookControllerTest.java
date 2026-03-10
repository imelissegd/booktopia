package com.mgd.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Category;
import com.mgd.bookstore.service.BookService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookController Tests")
class BookControllerTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Book techBook;
    private Book fantasyBook;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookController).build();
        objectMapper = new ObjectMapper();

        techBook = new Book();
        techBook.setId(1L);
        techBook.setTitle("Clean Code");
        techBook.setAuthor("Robert Martin");
        techBook.setPrice(new BigDecimal("39.99"));
        techBook.setDescription("Agile software craftsmanship");
        techBook.setCategories(List.of(Category.TECHNOLOGY));

        fantasyBook = new Book();
        fantasyBook.setId(2L);
        fantasyBook.setTitle("The Hobbit");
        fantasyBook.setAuthor("J.R.R. Tolkien");
        fantasyBook.setPrice(new BigDecimal("14.99"));
        fantasyBook.setDescription("A fantasy adventure");
        fantasyBook.setCategories(List.of(Category.FANTASY));
    }

    // -----------------------------------------------------------------------
    // GET /api/books
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/books - returns 200 with all books")
    void getAllBooks_returns200_withAllBooks() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of(techBook, fantasyBook));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[1].title").value("The Hobbit"));
    }

    @Test
    @DisplayName("GET /api/books - returns empty array when no books")
    void getAllBooks_returnsEmptyArray_whenNoBooks() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -----------------------------------------------------------------------
    // GET /api/books/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/books/{id} - returns 200 with book when found")
    void getBookById_returns200_whenFound() throws Exception {
        when(bookService.getBookById(1L)).thenReturn(Optional.of(techBook));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert Martin"));
    }

    @Test
    @DisplayName("GET /api/books/{id} - returns 404 when not found")
    void getBookById_returns404_whenNotFound() throws Exception {
        when(bookService.getBookById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/books/search
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/books/search?title= - returns books matching title")
    void searchBooks_byTitle_returnsMatchingBooks() throws Exception {
        when(bookService.searchBooks("Clean", null, null)).thenReturn(List.of(techBook));

        mockMvc.perform(get("/api/books/search").param("title", "Clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/books/search?author= - returns books matching author")
    void searchBooks_byAuthor_returnsMatchingBooks() throws Exception {
        when(bookService.searchBooks(null, "Tolkien", null)).thenReturn(List.of(fantasyBook));

        mockMvc.perform(get("/api/books/search").param("author", "Tolkien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].author").value("J.R.R. Tolkien"));
    }

    @Test
    @DisplayName("GET /api/books/search?category=FANTASY - returns books in that category")
    void searchBooks_byCategory_returnsMatchingBooks() throws Exception {
        when(bookService.searchBooks(null, null, Category.FANTASY)).thenReturn(List.of(fantasyBook));

        mockMvc.perform(get("/api/books/search").param("category", "FANTASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("The Hobbit"));
    }

    @Test
    @DisplayName("GET /api/books/search?category=invalid - returns all books ignoring invalid category")
    void searchBooks_withInvalidCategory_returnsAllBooks() throws Exception {
        when(bookService.searchBooks(null, null, null)).thenReturn(List.of(techBook, fantasyBook));

        mockMvc.perform(get("/api/books/search").param("category", "INVALID_CAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/books/search?title=&category=TECHNOLOGY - combines title and category")
    void searchBooks_byTitleAndCategory_returnsCombinedResults() throws Exception {
        when(bookService.searchBooks("Clean", null, Category.TECHNOLOGY)).thenReturn(List.of(techBook));

        mockMvc.perform(get("/api/books/search")
                        .param("title", "Clean")
                        .param("category", "TECHNOLOGY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    // -----------------------------------------------------------------------
    // POST /api/books
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/books - returns 200 with saved book")
    void createBook_returns200_withSavedBook() throws Exception {
        when(bookService.saveBook(any(Book.class))).thenReturn(techBook);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(techBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert Martin"));
    }
}