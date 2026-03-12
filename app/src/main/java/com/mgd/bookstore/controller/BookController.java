package com.mgd.bookstore.controller;

import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Category;
import com.mgd.bookstore.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    // Admin: fetch all books including delisted
    @GetMapping("/admin/all")
    public List<Book> getAllBooksAdmin() {
        return bookService.getAllBooksAdmin();
    }

    /**
     * Search by title and/or author, and optionally filter by category.
     * Examples:
     *   GET /api/books/search?title=java
     *   GET /api/books/search?author=tolkien
     *   GET /api/books/search?title=ring&category=FANTASY
     *   GET /api/books/search?category=SCIENCE
     */
    @GetMapping("/search")
    public List<Book> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category) {

        Category cat = null;
        if (category != null && !category.isBlank()) {
            try {
                cat = Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid category values
            }
        }

        return bookService.searchBooks(title, author, cat);
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book saved = bookService.saveBook(book);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        return bookService.updateBook(id, book)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (bookService.getBookById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/delist")
    public ResponseEntity<Book> delistBook(@PathVariable Long id) {
        return bookService.delistBook(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/relist")
    public ResponseEntity<Book> relistBook(@PathVariable Long id) {
        return bookService.relistBook(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}