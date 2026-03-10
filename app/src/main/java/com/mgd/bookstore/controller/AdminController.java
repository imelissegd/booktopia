package com.mgd.bookstore.controller;

import com.mgd.bookstore.dto.UserDTO;
import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.repository.BookRepository;
import com.mgd.bookstore.repository.UserRepository;
import com.mgd.bookstore.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // only admins can call these
public class AdminController {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserService userService;


    public AdminController(BookRepository bookRepository, UserRepository userRepository, UserService userService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // ✅ Get all users
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> dtos = userService.getAllUsers().stream()
                .map(userService::mapToDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // ✅ Add a new book
    @PostMapping("/books")
    public Book addBook(@RequestBody Book book) {
        return bookRepository.save(book);
    }

    // ✅ Edit a book
    @PutMapping("/books/{id}")
    public Book editBook(@PathVariable Long id, @RequestBody Book updatedBook) {
        Book book = bookRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found")
        );

        book.setTitle(updatedBook.getTitle());
        book.setAuthor(updatedBook.getAuthor());
        book.setPrice(updatedBook.getPrice());
        book.setDescription(updatedBook.getDescription());
        book.setCategories(updatedBook.getCategories());
        return bookRepository.save(book);
    }

    // ✅ Delete a book
    @DeleteMapping("/books/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookRepository.deleteById(id);
        return "Book deleted successfully";
    }

    // ✅ Promote a user to admin
    @PostMapping("/users/{id}/promote")
    public String promoteToAdmin(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        user.setRole(Role.ROLE_ADMIN);
        userRepository.save(user);
        return "User " + user.getUsername() + " promoted to ADMIN";
    }
}