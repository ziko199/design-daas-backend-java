package de.frauas.design.backend.user.service;

import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.repository.UserRepository;
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
@DisplayName("UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl service;

    @Test
    @DisplayName("returns UserDetails with SCOPE_user for a User entity")
    void loadByUsername_user_returnsScopeUser() {
        User u = new User();
        u.setId(1);
        u.setEmail("alice@example.com");
        u.setPassword("hashed");
        u.setEnabled(true);
        u.setName("Alice");
        u.setGuid("g");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("alice@example.com");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("SCOPE_user");
    }

    @Test
    @DisplayName("returns UserDetails with SCOPE_admin for an Admin entity")
    void loadByUsername_admin_returnsScopeAdmin() {
        Admin a = new Admin();
        a.setId(2);
        a.setEmail("admin@example.com");
        a.setPassword("hashed");
        a.setEnabled(true);
        a.setName("Admin");
        a.setGuid("ga");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(a));

        UserDetails details = service.loadUserByUsername("admin@example.com");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("SCOPE_admin");
    }

    @Test
    @DisplayName("returns disabled UserDetails when user is not enabled")
    void loadByUsername_disabledUser_returnsDisabledDetails() {
        User u = new User();
        u.setId(3);
        u.setEmail("disabled@example.com");
        u.setPassword("hashed");
        u.setEnabled(false);
        u.setName("Dis");
        u.setGuid("gd");

        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("disabled@example.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("throws UsernameNotFoundException when email not found")
    void loadByUsername_notFound_throws() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@example.com");
    }
}

