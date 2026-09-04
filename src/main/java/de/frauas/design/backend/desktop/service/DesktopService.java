package de.frauas.design.backend.desktop.service;

import de.frauas.design.backend.desktop.dto.DesktopCreateRequest;
import de.frauas.design.backend.desktop.dto.DesktopDto;
import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.exception.DesktopGroupNameRequiredException;
import de.frauas.design.backend.desktop.exception.DesktopGroupNotFoundException;
import de.frauas.design.backend.desktop.exception.DesktopNotFoundException;
import de.frauas.design.backend.desktop.exception.UserGroupAlreadyAssociatedException;
import de.frauas.design.backend.desktop.model.Desktop;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.desktop.repository.DesktopRepository;
import de.frauas.design.backend.user.dto.UserGroupDto;
import de.frauas.design.backend.user.exception.UserGroupNotFoundException;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Core business logic for desktop and desktop-group management, including user-group
 * association handling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesktopService {

    private final DesktopRepository desktopRepository;
    private final DesktopGroupRepository desktopGroupRepository;
    private final UserGroupRepository userGroupRepository;

    // ---- Desktops ----

    /**
     * Returns every desktop.
     *
     * @return all desktops
     */
    @Transactional(readOnly = true)
    public List<DesktopDto> getAllDesktops() {
        log.debug("getAllDesktops");
        return desktopRepository.findAll().stream().map(DesktopDto::from).toList();
    }

    /**
     * Looks up a single desktop by ID.
     *
     * @param id the desktop ID
     * @return the matching desktop
     * @throws DesktopNotFoundException if no desktop exists with the given id
     */
    @Transactional(readOnly = true)
    public DesktopDto getDesktopById(Integer id) {
        log.debug("getDesktopById — id={}", id);
        return DesktopDto.from(findDesktopById(id));
    }

    /**
     * Creates a new desktop, optionally cascade-creating desktop subgroups for it.
     *
     * <p>If a subgroup has no name, its description is used as the name instead — at
     * least one of the two must be present, enforced via {@link
     * DesktopCreateRequest.SubGroupRequest#getDescription()} being {@code @NotBlank}.</p>
     *
     * @param request the desktop to create, plus any subgroups to create alongside it
     * @return the created desktop
     */
    @Transactional
    public DesktopDto createDesktop(DesktopCreateRequest request) {
        log.debug("createDesktop(DesktopCreateRequest) — name={}", request.getName());

        Desktop desktop = new Desktop();
        desktop.setName(resolveName(request.getName(), request.getDescription()));
        desktop.setDescription(request.getDescription());
        Desktop savedDesktop = desktopRepository.save(desktop);
        if (request.getGroups() != null) {
            for (DesktopCreateRequest.SubGroupRequest sgReq : request.getGroups()) {
                DesktopGroup g = new DesktopGroup();
                g.setName(resolveName(sgReq.getName(), sgReq.getDescription()));
                g.setDescription(sgReq.getDescription());
                linkDesktopAndGroup(savedDesktop, g);
                desktopGroupRepository.save(g);
                log.debug(
                        "createDesktop — created sub-group name={} for desktopId={}",
                        g.getName(),
                        savedDesktop.getId());
            }
        }
        log.info(
                "createDesktop — desktop created id={} name={} groups={}",
                savedDesktop.getId(),
                savedDesktop.getName(),
                request.getGroups() != null ? request.getGroups().size() : 0);
        return DesktopDto.from(savedDesktop);
    }

    // ---- DesktopGroups ----

    /**
     * Returns every desktop group.
     *
     * @return all desktop groups
     */
    @Transactional(readOnly = true)
    public List<DesktopGroupDto> getAllDesktopGroups() {
        log.debug("getAllDesktopGroups");
        return desktopGroupRepository.findAll().stream()
                .map(DesktopGroupDto::from)
                .toList();
    }

    /**
     * Creates a new desktop group.
     *
     * <p>If no name is given, the description is used as the name instead — at least one
     * of the two must be present.</p>
     *
     * @param request the group data; {@code name} falls back to {@code description} when blank
     * @return the created group
     * @throws DesktopGroupNameRequiredException if both {@code name} and {@code description}
     *     are blank
     */
    @Transactional
    public DesktopGroupDto createDesktopGroup(DesktopGroupDto request) {
        log.debug("createDesktopGroup — name={}", request.getName());
        DesktopGroup g = new DesktopGroup();
        g.setName(resolveName(request.getName(), request.getDescription()));
        g.setDescription(request.getDescription());
        DesktopGroup savedGroup = desktopGroupRepository.save(g);
        log.info("createDesktopGroup — created id={} name={}", savedGroup.getId(), savedGroup.getName());
        return DesktopGroupDto.from(savedGroup);
    }

    /**
     * Associates a {@code UserGroup} with a {@code DesktopGroup}, granting that user group's
     * members access to every desktop in the desktop group.
     *
     * @param desktopGroupId the ID of the desktop group to associate
     * @param userGroupId the ID of the user group to associate
     * @return the desktop group's user groups after the association is added
     * @throws DesktopGroupNotFoundException if no desktop group exists with the given id
     * @throws UserGroupNotFoundException if no user group exists with the given id
     * @throws UserGroupAlreadyAssociatedException if the user group is already associated
     *     with the desktop group
     */
    @Transactional
    public List<UserGroupDto> addUserGroupToDesktopGroup(Integer desktopGroupId, Integer userGroupId) {
        log.debug("addUserGroupToDesktopGroup — desktopGroupId={} userGroupId={}", desktopGroupId, userGroupId);
        DesktopGroup dg = findDesktopGroupById(desktopGroupId);
        UserGroup ug = findUserGroupById(userGroupId);
        if (ug.getDesktopGroups().contains(dg)) {
            log.warn(
                    "addUserGroupToDesktopGroup — desktopGroupId={} userGroupId={} already associated",
                    desktopGroupId,
                    userGroupId);
            throw new UserGroupAlreadyAssociatedException(desktopGroupId, userGroupId);
        }
        linkUserGroupAndDesktopGroup(ug, dg);
        userGroupRepository.save(ug);
        log.info(
                "addUserGroupToDesktopGroup — desktopGroupId={} userGroupId={} association created",
                desktopGroupId,
                userGroupId);
        return dg.getUserGroups().stream().map(UserGroupDto::from).toList();
    }

    /**
     * Resolves the effective name for a desktop/desktop-group, falling back to
     * {@code description} when {@code name} is blank.
     *
     * @param name the requested name, possibly blank or {@code null}
     * @param description the requested description, used as a fallback name
     * @return {@code name} if non-blank, otherwise {@code description}
     * @throws DesktopGroupNameRequiredException if both {@code name} and {@code description}
     *     are blank
     */
    private String resolveName(String name, String description) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (description != null && !description.isBlank()) {
            return description;
        }
        throw new DesktopGroupNameRequiredException();
    }

    private Desktop findDesktopById(Integer id) {
        return desktopRepository.findById(id).orElseThrow(() -> {
            log.warn("getDesktopById — desktopId={} not found", id);
            return new DesktopNotFoundException(id);
        });
    }

    private DesktopGroup findDesktopGroupById(Integer id) {
        return desktopGroupRepository.findById(id).orElseThrow(() -> {
            log.warn("addUserGroupToDesktopGroup — desktopGroupId={} not found", id);
            return new DesktopGroupNotFoundException(id);
        });
    }

    private UserGroup findUserGroupById(Integer id) {
        return userGroupRepository.findById(id).orElseThrow(() -> {
            log.warn("addUserGroupToDesktopGroup — userGroupId={} not found", id);
            return new UserGroupNotFoundException(id);
        });
    }

    private void linkDesktopAndGroup(Desktop desktop, DesktopGroup group) {
        if (!group.getDesktops().contains(desktop)) {
            group.getDesktops().add(desktop);
        }
        if (!desktop.getDesktopGroups().contains(group)) {
            desktop.getDesktopGroups().add(group);
        }
    }

    private void linkUserGroupAndDesktopGroup(UserGroup userGroup, DesktopGroup desktopGroup) {
        if (!userGroup.getDesktopGroups().contains(desktopGroup)) {
            userGroup.getDesktopGroups().add(desktopGroup);
        }
        if (!desktopGroup.getUserGroups().contains(userGroup)) {
            desktopGroup.getUserGroups().add(userGroup);
        }
    }
}
