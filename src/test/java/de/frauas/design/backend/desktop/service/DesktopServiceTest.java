package de.frauas.design.backend.desktop.service;

import de.frauas.design.backend.desktop.dto.DesktopDto;
import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.model.Desktop;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.desktop.repository.DesktopRepository;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DesktopService")
class DesktopServiceTest {

    @Mock DesktopRepository desktopRepository;
    @Mock DesktopGroupRepository desktopGroupRepository;
    @Mock UserRepository userRepository;

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
        @DisplayName("createDesktop persists and returns DTO")
        void createDesktop_valid_returnsDto() {
            DesktopDto req = DesktopDto.builder().name("WS1").description("Workstation 1").build();
            when(desktopRepository.save(any())).thenAnswer(inv -> {
                Desktop d = inv.getArgument(0);
                d.setId(1);
                return d;
            });

            DesktopDto dto = desktopService.createDesktop(req);

            assertThat(dto.getName()).isEqualTo("WS1");
            assertThat(dto.getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("createDesktop falls back to description when name is null")
        void createDesktop_noName_usesDescription() {
            DesktopDto req = DesktopDto.builder().description("FallbackDesc").build();
            when(desktopRepository.save(any())).thenAnswer(inv -> {
                Desktop d = inv.getArgument(0);
                d.setId(2);
                return d;
            });

            DesktopDto dto = desktopService.createDesktop(req);

            assertThat(dto.getName()).isEqualTo("FallbackDesc");
        }

        @Test
        @DisplayName("updateDesktop updates name and description")
        void updateDesktop_updatesFields() {
            Desktop d = new Desktop();
            d.setId(1);
            d.setName("Old");
            d.setDescription("Old desc");
            when(desktopRepository.findById(1)).thenReturn(Optional.of(d));
            when(desktopRepository.save(any())).thenReturn(d);

            DesktopDto req = DesktopDto.builder().name("New").description("New desc").build();
            DesktopDto dto = desktopService.updateDesktop(1, req);

            assertThat(d.getName()).isEqualTo("New");
            assertThat(d.getDescription()).isEqualTo("New desc");
        }

        @Test
        @DisplayName("updateDesktop throws when not found")
        void updateDesktop_notFound_throws() {
            when(desktopRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.updateDesktop(99, DesktopDto.builder().build()))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("deleteDesktop calls deleteById")
        void deleteDesktop_found_deletes() {
            Desktop d = new Desktop(); d.setId(1);
            when(desktopRepository.findById(1)).thenReturn(Optional.of(d));

            desktopService.deleteDesktop(1);

            verify(desktopRepository).deleteById(1);
        }

        @Test
        @DisplayName("deleteDesktop throws when not found")
        void deleteDesktop_notFound_throws() {
            when(desktopRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.deleteDesktop(99))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("getAllDesktops returns mapped DTOs")
        void getAllDesktops_returnsDtos() {
            Desktop d = new Desktop(); d.setId(1); d.setName("D1");
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
            DesktopGroupDto req = DesktopGroupDto.builder().name("DG1").description("Group 1").build();
            when(desktopGroupRepository.save(any())).thenAnswer(inv -> {
                DesktopGroup g = inv.getArgument(0);
                g.setId(1);
                return g;
            });

            DesktopGroupDto dto = desktopService.createDesktopGroup(req);

            assertThat(dto.getName()).isEqualTo("DG1");
        }

        @Test
        @DisplayName("getDesktopGroupById throws when not found")
        void getDesktopGroupById_notFound_throws() {
            when(desktopGroupRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.getDesktopGroupById(99))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("deleteDesktopGroup throws when not found")
        void deleteDesktopGroup_notFound_throws() {
            when(desktopGroupRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> desktopService.deleteDesktopGroup(99))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // -------------------------------------------------------------------------
    // checkUserDesktopAccess
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("checkUserDesktopAccess")
    class CheckAccess {

        @Test
        @DisplayName("returns deny when user not found")
        void checkAccess_userNotFound_deny() {
            when(userRepository.findUserById(99)).thenReturn(Optional.empty());

            Map<String, Object> result = desktopService.checkUserDesktopAccess(99, 1);

            assertThat(result).containsEntry("result", "deny");
        }

        @Test
        @DisplayName("returns allow when user has access to desktop via group chain")
        void checkAccess_userHasAccess_allow() {
            Desktop d = new Desktop(); d.setId(10);

            DesktopGroup dg = new DesktopGroup();
            dg.setId(5);
            dg.setName("DG");
            dg.getDesktops().add(d);

            UserGroup ug = new UserGroup();
            ug.setId(1);
            ug.setName("UG");
            ug.getDesktopGroups().add(dg);

            User u = new User();
            u.setId(1);
            u.setName("Alice");
            u.setEmail("alice@e.com");
            u.setPassword("h");
            u.setEnabled(true);
            u.setGuid("g");
            u.getGroups().add(ug);

            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));

            Map<String, Object> result = desktopService.checkUserDesktopAccess(1, 10);

            assertThat(result).containsEntry("result", "allow");
        }

        @Test
        @DisplayName("returns deny when user has no desktop access")
        void checkAccess_noAccess_deny() {
            Desktop d = new Desktop(); d.setId(99); // different desktop

            DesktopGroup dg = new DesktopGroup();
            dg.setId(5);
            dg.setName("DG");
            dg.getDesktops().add(d);

            UserGroup ug = new UserGroup();
            ug.setId(1);
            ug.setName("UG");
            ug.getDesktopGroups().add(dg);

            User u = new User();
            u.setId(1);
            u.setName("Alice");
            u.setEmail("alice@e.com");
            u.setPassword("h");
            u.setEnabled(true);
            u.setGuid("g");
            u.getGroups().add(ug);

            when(userRepository.findUserById(1)).thenReturn(Optional.of(u));

            Map<String, Object> result = desktopService.checkUserDesktopAccess(1, 10); // looking for 10

            assertThat(result).containsEntry("result", "deny");
        }
    }
}

