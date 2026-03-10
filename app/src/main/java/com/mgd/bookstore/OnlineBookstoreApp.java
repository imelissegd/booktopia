package com.mgd.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
public class OnlineBookstoreApp {
    public static void main(String[] args) {
        System.out.println("Welcome to Booktopia by Mel!");

        SpringApplication.run(OnlineBookstoreApp.class, args);

    }
}
