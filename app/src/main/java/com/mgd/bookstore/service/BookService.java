package com.mgd.bookstore.service;

import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Category;
import com.mgd.bookstore.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    /**
     * Unified search: filter by title OR author (keyword), and/or category.
     * - If only keyword is provided: search title + author
     * - If only category is provided: filter by category
     * - If both: search title/author AND filter by category
     * - If neither: return all books
     */
    public List<Book> searchBooks(String title, String author, Category category) {
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasAuthor = author != null && !author.isBlank();
        boolean hasCategory = category != null;

        List<Book> result;

        if (hasTitle) {
            result = bookRepository.findByTitleContainingIgnoreCase(title);
        } else if (hasAuthor) {
            result = bookRepository.findByAuthorContainingIgnoreCase(author);
        } else {
            result = bookRepository.findAll();
        }

        if (hasCategory) {
            final Category cat = category;
            result = result.stream()
                    .filter(book -> book.getCategories() != null && book.getCategories().contains(cat))
                    .collect(Collectors.toList());
        }

        return result;
    }

    // Keep legacy methods for any existing callers
    public List<Book> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> searchByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }
}