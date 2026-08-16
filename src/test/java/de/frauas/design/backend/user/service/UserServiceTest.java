package de.frauas.design.backend.user.service;

import de.frauas.design.backend.user.dto.*;
import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserGroupRepository userGroupRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MailService mailService;

    @InjectMocks
    UserService userService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User makeUser(Integer id, String email, boolean enabled) {
        User u = new User();
        u.setId(id);
        u.setGuid("guid-" + id);
        u.setName("Test User");
        u.setEmail(email);
        u.setPassword("encoded");
        u.setEnabled(enabled);
        u.setRegistrationCode("ABCD1234");
        u.setRegistrationCodeTimeout(LocalDateTime.now().plusHours(1));
        return u;
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

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("creates user with valid data")
        void createUser_validData_savesAndReturnsDto() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Alice");
            req.setEmail("alice@example.com");
            req.setPassword("Password1");

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("Password1")).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1);
                return u;
            });

            UserDto dto = userService.createUser(req);

            assertThat(dto.getEmail()).isEqualTo("alice@example.com");
            assertThat(dto.getName()).isEqualTo("Alice");
            assertThat(dto.isEnabled()).isFalse();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws when email already in use")
        void createUser_duplicateEmail_throwsIllegalArgument() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Alice");
            req.setEmail("alice@example.com");
            req.setPassword("Password1");

            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(makeUser(1, "alice@example.com", true)));

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already in use");
        }

        @Test
        @DisplayName("throws when password is too short")
        void createUser_weakPassword_throwsIllegalArgument() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Alice");
            req.setEmail("alice@example.com");
            req.setPassword("weak");

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password must be at least 8");
        }

        @Test
        @DisplayName("throws when password has no uppercase")
        void createUser_noUppercase_throwsIllegalArgument() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Alice");
            req.setEmail("alice@example.com");
            req.setPassword("password1");

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when password has no digit")
        void createUser_noDigit_throwsIllegalArgument() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Alice");
            req.setEmail("alice@example.com");
            req.setPassword("PasswordOnly");

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("assigns groups when group IDs are provided")
        void createUser_withGroups_assignsGroups() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("Alice");
            req.setEmail("alice@example.com");
            req.setPassword("Password1");
            req.setGroups(List.of(10, 20));

            UserGroup g1 = new UserGroup(); g1.setId(10); g1.setName("G1");
            UserGroup g2 = new UserGroup(); g2.setId(20); g2.setName("G2");

            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userGroupRepository.findAllById(List.of(10, 20))).thenReturn(List.of(g1, g2));
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1);
                return u;
            });

            UserDto dto = userService.createUser(req);

            assertThat(dto.getGroups()).containsExactlyInAnyOrder(10, 20);
        }
    }

    // -------------------------------------------------------------------------
    // validateEmail
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("validateEmail")
    class ValidateEmail {

        @Test
        @DisplayName("returns true and enables user on valid code")
        void validateEmail_validCode_enablesUser() {
            User u = makeUser(1, "alice@example.com", false);

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(u));

            boolean result = userService.validateEmail("alice@example.com", "ABCD1234");

            assertThat(result).isTrue();
            assertThat(u.isEnabled()).isTrue();
            assertThat(u.getRegistrationCode()).isNull();
            verify(userRepository).save(u);
        }

        @Test
        @DisplayName("returns false when code is wrong")
        void validateEmail_wrongCode_returnsFalse() {
            User u = makeUser(1, "alice@example.com", false);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(u));

            boolean result = userService.validateEmail("alice@example.com", "WRONGCOD");

            assertThat(result).isFalse();
            assertThat(u.isEnabled()).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns false when code is expired")
        void validateEmail_expiredCode_returnsFalse() {
            User u = makeUser(1, "alice@example.com", false);
            u.setRegistrationCodeTimeout(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(u));

            boolean result = userService.validateEmail("alice@example.com", "ABCD1234");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false when user not found")
        void validateEmail_userNotFound_returnsFalse() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            boolean result = userService.validateEmail("missing@example.com", "ABCD1234");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false when found entity is Admin, not User")
        void validateEmail_adminFound_returnsFalse() {
            Admin a = makeAdmin(2, "admin@example.com");
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(a));

            boolean result = userService.validateEmail("admin@example.com", "ABCD1234");

            assertThat(result).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // getAllUsers with pagination
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("returns correct page of users")
        void getAllUsers_page0perPage2_returnsFirst2() {
            List<User> allUsers = List.of(
                    makeUser(1, "u1@e.com", true),
                    makeUser(2, "u2@e.com", true),
                    makeUser(3, "u3@e.com", true)
            );
            when(userRepository.findAllUsers()).thenReturn(allUsers);

            List<UserDto> page = userService.getAllUsers(0, 2);

            assertThat(page).hasSize(2);
            assertThat(page.get(0).getEmail()).isEqualTo("u1@e.com");
        }

        @Test
        @DisplayName("returns second page")
        void getAllUsers_page1perPage2_returnsThird() {
            List<User> allUsers = List.of(
                    makeUser(1, "u1@e.com", true),
                    makeUser(2, "u2@e.com", true),
                    makeUser(3, "u3@e.com", true)
            );
            when(userRepository.findAllUsers()).thenReturn(allUsers);

            List<UserDto> page = userService.getAllUsers(1, 2);

            assertThat(page).hasSize(1);
            assertThat(page.get(0).getEmail()).isEqualTo("u3@e.com");
        }

        @Test
        @DisplayName("returns empty list when page is beyond total")
        void getAllUsers_pageBeyondTotal_returnsEmpty() {
            when(userRepository.findAllUsers()).thenReturn(List.of(makeUser(1, "u@e.com", true)));

            List<UserDto> page = userService.getAllUsers(5, 20);

            assertThat(page).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getUserById
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns DTO when user exists")
        void getUserById_exists_returnsDto() {
            User u = makeUser(1, "u@e.com", true);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));

            UserDto dto = userService.getUserById(1);

            assertThat(dto.getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("throws NoSuchElementException when user not found")
        void getUserById_notFound_throws() {
            when(userRepository.findUserById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("99");
        }
    }

    // -------------------------------------------------------------------------
    // updateUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("updates name when provided")
        void updateUser_newName_updatesName() {
            User u = makeUser(1, "u@e.com", true);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));
            when(userRepository.save(any())).thenReturn(u);

            PatchUserRequest req = new PatchUserRequest();
            req.setName("New Name");

            UserDto dto = userService.updateUser(1, req);

            assertThat(u.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("updates password with encoding when provided")
        void updateUser_newPassword_encodesAndUpdates() {
            User u = makeUser(1, "u@e.com", true);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));
            when(passwordEncoder.encode("NewPass1")).thenReturn("hashed2");
            when(userRepository.save(any())).thenReturn(u);

            PatchUserRequest req = new PatchUserRequest();
            req.setPassword("NewPass1");

            userService.updateUser(1, req);

            assertThat(u.getPassword()).isEqualTo("hashed2");
        }

        @Test
        @DisplayName("throws on weak new password")
        void updateUser_weakPassword_throws() {
            User u = makeUser(1, "u@e.com", true);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));

            PatchUserRequest req = new PatchUserRequest();
            req.setPassword("weak");

            assertThatThrownBy(() -> userService.updateUser(1, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when user not found")
        void updateUser_notFound_throws() {
            when(userRepository.findUserById(99)).thenReturn(Optional.empty());

            PatchUserRequest req = new PatchUserRequest();
            req.setName("X");

            assertThatThrownBy(() -> userService.updateUser(99, req))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("deletes user when found")
        void deleteUser_found_deletes() {
            User u = makeUser(1, "u@e.com", true);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));

            userService.deleteUser(1);

            verify(userRepository).deleteById(1);
        }

        @Test
        @DisplayName("throws when user not found")
        void deleteUser_notFound_throws() {
            when(userRepository.findUserById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(99))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // -------------------------------------------------------------------------
    // enableUser / disableUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("enableUser / disableUser")
    class EnableDisableUser {

        @Test
        @DisplayName("enableUser sets enabled=true")
        void enableUser_setsEnabled() {
            User u = makeUser(1, "u@e.com", false);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));
            when(userRepository.save(any())).thenReturn(u);

            UserDto dto = userService.enableUser(1);

            assertThat(u.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("disableUser sets enabled=false")
        void disableUser_setsDisabled() {
            User u = makeUser(1, "u@e.com", true);
            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));
            when(userRepository.save(any())).thenReturn(u);

            UserDto dto = userService.disableUser(1);

            assertThat(u.isEnabled()).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // createAdmin
    // -------------------------------------------------------------------------

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

            AdminDto dto = userService.createAdmin(req);

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

            assertThatThrownBy(() -> userService.createAdmin(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already in use");
        }
    }

    // -------------------------------------------------------------------------
    // UserGroup CRUD
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("UserGroup CRUD")
    class UserGroupCrud {

        @Test
        @DisplayName("createUserGroup saves and returns DTO")
        void createUserGroup_valid_savesAndReturnsDto() {
            UserGroupDto req = UserGroupDto.builder().name("Devs").description("Developers").build();
            when(userGroupRepository.save(any())).thenAnswer(inv -> {
                UserGroup g = inv.getArgument(0);
                g.setId(1);
                return g;
            });

            UserGroupDto dto = userService.createUserGroup(req);

            assertThat(dto.getName()).isEqualTo("Devs");
        }

        @Test
        @DisplayName("getUserGroupById throws when not found")
        void getUserGroupById_notFound_throws() {
            when(userGroupRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserGroupById(99))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("deleteUserGroup throws when not found")
        void deleteUserGroup_notFound_throws() {
            when(userGroupRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUserGroup(99))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("updateUserGroup updates fields")
        void updateUserGroup_updatesFields() {
            UserGroup g = new UserGroup();
            g.setId(1);
            g.setName("Old");
            g.setDescription("Old desc");

            when(userGroupRepository.findById(1)).thenReturn(Optional.of(g));
            when(userGroupRepository.save(any())).thenReturn(g);

            UserGroupDto req = UserGroupDto.builder().name("New").build();
            UserGroupDto dto = userService.updateUserGroup(1, req);

            assertThat(g.getName()).isEqualTo("New");
            assertThat(g.getDescription()).isEqualTo("Old desc"); // unchanged
        }
    }

    // -------------------------------------------------------------------------
    // password validation boundary tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("password validation")
    class PasswordValidation {

        /** Tries to create a user with the given password using lenient mocks (success path). */
        private void tryCreate(String password) {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("X");
            req.setEmail("x@x.com");
            req.setPassword(password);
            lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            lenient().when(passwordEncoder.encode(any())).thenReturn("h");
            lenient().when(userRepository.save(any())).thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(1); return u; });
            userService.createUser(req);
        }

        @Test
        @DisplayName("accepts exactly 8-char password with upper, lower, digit")
        void password_exactly8chars_accepted() {
            assertThatCode(() -> tryCreate("Abc1defg")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rejects null password")
        void password_null_rejected() {
            CreateUserRequest req = new CreateUserRequest();
            req.setName("X");
            req.setEmail("x@x.com");
            req.setPassword(null);

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects 7-char password")
        void password_7chars_rejected() {
            // Call service directly — password check happens before any repository call
            CreateUserRequest req = new CreateUserRequest();
            req.setName("X");
            req.setEmail("x@x.com");
            req.setPassword("Abc1def");   // exactly 7 chars, valid pattern but too short

            assertThatThrownBy(() -> userService.createUser(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

