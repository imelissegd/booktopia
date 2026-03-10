package com.mgd.bookstore.repository;

import com.mgd.bookstore.model.Cart;
import com.mgd.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}