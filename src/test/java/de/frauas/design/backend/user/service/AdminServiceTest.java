package de.frauas.design.backend.user.service;

import de.frauas.design.backend.user.dto.AdminDto;
import de.frauas.design.backend.user.dto.CreateAdminRequest;
import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService")
class AdminServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    AdminService adminService;

    @BeforeEach
    void setUp() {
        AccountValidator accountValidator = new AccountValidator(userRepository);
        adminService = new AdminService(userRepository, passwordEncoder, accountValidator);
    }

    private Admin makeAdmin(Integer id, String email) {
        Admin a = new Admin();
        a.setId(id);
        a.setGuid("guid-admin-" + id);
        a.setName("Admin User");
        a.setEmail(email);
        a.setPassword("encoded");
        a.setEnabled(true);
        return a;
    }

    @Nested
    @DisplayName("createAdmin")
    class CreateAdmin {

        @Test
        @DisplayName("creates admin with valid data")
        void createAdmin_valid_returnsDto() {
            CreateAdminRequest req = new CreateAdminRequest();
            req.setName("AdminX");
            req.setEmail("admin@example.com");
            req.setPassword("AdminPass1");

            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("AdminPass1")).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                Admin a = inv.getArgument(0);
                a.setId(5);
                return a;
            });

            AdminDto dto = adminService.createAdmin(req);

            assertThat(dto.getEmail()).isEqualTo("admin@example.com");
            assertThat(dto.getRole()).isEqualTo("admin");
        }

        @Test
        @DisplayName("throws on duplicate admin email")
        void createAdmin_duplicateEmail_throws() {
            CreateAdminRequest req = new CreateAdminRequest();
            req.setName("AdminX");
            req.setEmail("admin@example.com");
            req.setPassword("AdminPass1");

            when(userRepository.findByEmail("admin@example.com"))
                    .thenReturn(Optional.of(makeAdmin(1, "admin@example.com")));

            assertThatThrownBy(() -> adminService.createAdmin(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already in use");
        }
    }

    @Nested
    @DisplayName("getAllAdmins")
    class GetAllAdmins {

        @Test
        @DisplayName("returns all admins as DTOs")
        void getAllAdmins_returnsAllAsDtos() {
            when(userRepository.findAllAdmins())
                    .thenReturn(List.of(makeAdmin(1, "a1@e.com"), makeAdmin(2, "a2@e.com")));

            List<AdminDto> admins = adminService.getAllAdmins();

            assertThat(admins).hasSize(2);
            assertThat(admins).extracting(AdminDto::getEmail).containsExactlyInAnyOrder("a1@e.com", "a2@e.com");
        }
    }
}
