package de.frauas.design.backend.desktop.service;

import de.frauas.design.backend.desktop.dto.DesktopDto;
import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.exception.DesktopGroupNameRequiredException;
import de.frauas.design.backend.desktop.exception.DesktopGroupNotFoundException;
import de.frauas.design.backend.desktop.exception.UserGroupAlreadyAssociatedException;
import de.frauas.design.backend.desktop.model.Desktop;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.desktop.repository.DesktopRepository;
import de.frauas.design.backend.user.exception.UserGroupNotFoundException;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DesktopService")
class DesktopServiceTest {

    @Mock
    DesktopRepository desktopRepository;

    @Mock
    DesktopGroupRepository desktopGroupRepository;

    @Mock
    UserGroupRepository userGroupRepository;

    @InjectMocks
    DesktopService desktopService;

    // -------------------------------------------------------------------------
    // Desktop CRUD
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Desktop CRUD")
    class DesktopCrud {

        @Test
        @DisplayName("getDesktopById throws when not found")
        void getDesktopById_notFound_throws() {
            when(desktopRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.getDesktopById(99))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("getAllDesktops returns mapped DTOs")
        void getAllDesktops_returnsDtos() {
            Desktop d = new Desktop();
            d.setId(1);
            d.setName("D1");
            when(desktopRepository.findAll()).thenReturn(List.of(d));

            List<DesktopDto> result = desktopService.getAllDesktops();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("D1");
        }
    }

    // -------------------------------------------------------------------------
    // DesktopGroup CRUD
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DesktopGroup CRUD")
    class DesktopGroupCrud {

        @Test
        @DisplayName("createDesktopGroup persists and returns DTO")
        void createDesktopGroup_valid_returnsDto() {
            DesktopGroupDto req =
                    DesktopGroupDto.builder().name("DG1").description("Group 1").build();
            when(desktopGroupRepository.save(any())).thenAnswer(inv -> {
                DesktopGroup g = inv.getArgument(0);
                g.setId(1);
                return g;
            });

            DesktopGroupDto dto = desktopService.createDesktopGroup(req);

            assertThat(dto.getName()).isEqualTo("DG1");
        }

        @Test
        @DisplayName("createDesktopGroup falls back to description when name is blank")
        void createDesktopGroup_blankName_fallsBackToDescription() {
            DesktopGroupDto req =
                    DesktopGroupDto.builder().name(" ").description("Group 1").build();
            when(desktopGroupRepository.save(any())).thenAnswer(inv -> {
                DesktopGroup g = inv.getArgument(0);
                g.setId(1);
                return g;
            });

            DesktopGroupDto dto = desktopService.createDesktopGroup(req);

            assertThat(dto.getName()).isEqualTo("Group 1");
        }

        @Test
        @DisplayName("createDesktopGroup rejects blank name and description")
        void createDesktopGroup_bothBlank_throws() {
            DesktopGroupDto req =
                    DesktopGroupDto.builder().name(" ").description(" ").build();

            assertThatThrownBy(() -> desktopService.createDesktopGroup(req))
                    .isInstanceOf(DesktopGroupNameRequiredException.class);
        }
    }

    // -------------------------------------------------------------------------
    // User group association
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addUserGroupToDesktopGroup")
    class AddUserGroupToDesktopGroup {

        @Test
        @DisplayName("throws DesktopGroupNotFoundException when desktop group is missing")
        void desktopGroupMissing_throws() {
            when(desktopGroupRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.addUserGroupToDesktopGroup(1, 2))
                    .isInstanceOf(DesktopGroupNotFoundException.class);
        }

        @Test
        @DisplayName("throws UserGroupNotFoundException when user group is missing")
        void userGroupMissing_throws() {
            DesktopGroup dg = new DesktopGroup();
            dg.setId(1);
            when(desktopGroupRepository.findById(1)).thenReturn(Optional.of(dg));
            when(userGroupRepository.findById(2)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.addUserGroupToDesktopGroup(1, 2))
                    .isInstanceOf(UserGroupNotFoundException.class);
        }

        @Test
        @DisplayName("throws UserGroupAlreadyAssociatedException when already associated")
        void alreadyAssociated_throws() {
            DesktopGroup dg = new DesktopGroup();
            dg.setId(1);
            UserGroup ug = new UserGroup();
            ug.setId(2);
            ug.getDesktopGroups().add(dg);
            when(desktopGroupRepository.findById(1)).thenReturn(Optional.of(dg));
            when(userGroupRepository.findById(2)).thenReturn(Optional.of(ug));

            assertThatThrownBy(() -> desktopService.addUserGroupToDesktopGroup(1, 2))
                    .isInstanceOf(UserGroupAlreadyAssociatedException.class);
        }

        @Test
        @DisplayName("associates the user group and returns the desktop group's user groups")
        void notAssociated_createsAssociation() {
            DesktopGroup dg = new DesktopGroup();
            dg.setId(1);
            UserGroup ug = new UserGroup();
            ug.setId(2);
            when(desktopGroupRepository.findById(1)).thenReturn(Optional.of(dg));
            when(userGroupRepository.findById(2)).thenReturn(Optional.of(ug));
            when(userGroupRepository.save(any())).thenAnswer(inv -> {
                // Simulate the bidirectional relationship JPA maintains on flush.
                dg.getUserGroups().add(ug);
                return ug;
            });

            List<?> result = desktopService.addUserGroupToDesktopGroup(1, 2);

            assertThat(ug.getDesktopGroups()).contains(dg);
            assertThat(result).hasSize(1);
        }
    }
}
