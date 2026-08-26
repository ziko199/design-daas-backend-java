package de.frauas.design.backend.user.service;

import de.frauas.design.backend.user.dto.UserGroupDto;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserGroupService")
class UserGroupServiceTest {

    @Mock
    UserGroupRepository userGroupRepository;

    @Mock
    UserRepository userRepository;

    UserGroupService userGroupService;

    @BeforeEach
    void setUp() {
        userGroupService = new UserGroupService(userGroupRepository, userRepository);
    }

    @Nested
    @DisplayName("createUserGroup")
    class CreateUserGroup {

        @Test
        @DisplayName("saves and returns DTO")
        void createUserGroup_valid_savesAndReturnsDto() {
            UserGroupDto req = UserGroupDto.builder()
                    .name("Devs")
                    .description("Developers")
                    .build();
            when(userGroupRepository.save(any())).thenAnswer(inv -> {
                UserGroup g = inv.getArgument(0);
                g.setId(1);
                return g;
            });

            UserGroupDto dto = userGroupService.createUserGroup(req);

            assertThat(dto.getName()).isEqualTo("Devs");
        }

        @Test
        @DisplayName("throws when both name and description are blank")
        void createUserGroup_blankNameAndDescription_throws() {
            UserGroupDto req = UserGroupDto.builder().name("  ").description("").build();

            assertThatThrownBy(() -> userGroupService.createUserGroup(req))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(userGroupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUserGroup")
    class UpdateUserGroup {

        @Test
        @DisplayName("updates fields")
        void updateUserGroup_updatesFields() {
            UserGroup g = new UserGroup();
            g.setId(1);
            g.setName("Old");
            g.setDescription("Old desc");

            when(userGroupRepository.findById(1)).thenReturn(Optional.of(g));
            when(userGroupRepository.save(any())).thenReturn(g);

            UserGroupDto req = UserGroupDto.builder().name("New").build();
            UserGroupDto dto = userGroupService.updateUserGroup(1, req);

            assertThat(dto.getName()).isEqualTo("New");
            assertThat(dto.getDescription()).isEqualTo("Old desc"); // unchanged
        }
    }
}
