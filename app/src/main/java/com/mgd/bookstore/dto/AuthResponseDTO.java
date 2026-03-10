package com.mgd.bookstore.dto;

import java.util.Set;

// AuthResponse
public class AuthResponseDTO {
    private String token;
    private String username;
    private Set<String> roles;
}