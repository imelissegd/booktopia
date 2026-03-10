package com.mgd.bookstore.repository;

import com.mgd.bookstore.model.Book;
import com.mgd.bookstore.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByAuthorContainingIgnoreCase(String author);
    // For filtering by category enum
    List<Book> findByCategoriesContaining(Category category);
}