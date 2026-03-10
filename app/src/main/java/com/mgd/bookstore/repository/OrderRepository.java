package com.mgd.bookstore.repository;

import com.mgd.bookstore.model.Order;
import com.mgd.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}