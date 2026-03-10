package com.mgd.bookstore.security;

import com.mgd.bookstore.model.Role;
import com.mgd.bookstore.model.User;
import com.mgd.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User user;
    private User adminUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john_doe");
        user.setPassword("$2a$10$hashedpassword");
        user.setRole(Role.ROLE_USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setPassword("$2a$10$adminhashedpassword");
        adminUser.setRole(Role.ROLE_ADMIN);
    }

    // -----------------------------------------------------------------------
    // loadUserByUsername
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("loadUserByUsername - returns UserDetails for existing user")
    void loadUserByUsername_returnsUserDetails_forExistingUser() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("john_doe");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getPassword()).isEqualTo("$2a$10$hashedpassword");
    }

    @Test
    @DisplayName("loadUserByUsername - grants ROLE_USER authority to regular user")
    void loadUserByUsername_grantsRoleUser_toRegularUser() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("john_doe");

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername - grants ROLE_ADMIN authority to admin user")
    void loadUserByUsername_grantsRoleAdmin_toAdminUser() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        UserDetails result = userDetailsService.loadUserByUsername("admin");

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername - throws UsernameNotFoundException when user not found")
    void loadUserByUsername_throwsUsernameNotFoundException_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("loadUserByUsername - calls repository with exact username")
    void loadUserByUsername_callsRepositoryWithExactUsername() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        userDetailsService.loadUserByUsername("john_doe");

        verify(userRepository).findByUsername("john_doe");
        verifyNoMoreInteractions(userRepository);
    }
}