package de.frauas.design.backend.desktop.service;

import de.frauas.design.backend.desktop.dto.DesktopCreateRequest;
import de.frauas.design.backend.desktop.dto.DesktopDto;
import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.model.Desktop;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.desktop.repository.DesktopRepository;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesktopService {

    private final DesktopRepository desktopRepository;
    private final DesktopGroupRepository desktopGroupRepository;
    private final UserRepository userRepository;

    // ---- Desktops ----

    @Transactional(readOnly = true)
    public List<DesktopDto> getAllDesktops() {
        log.debug("getAllDesktops");
        return desktopRepository.findAll().stream().map(DesktopDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DesktopDto getDesktopById(Integer id) {
        log.debug("getDesktopById — id={}", id);
        return DesktopDto.from(desktopRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Desktop not found: " + id)));
    }

    @Transactional
    public DesktopDto createDesktop(DesktopDto req) {
        log.debug("createDesktop(DesktopDto) — name={}", req.getName());
        Desktop d = new Desktop();
        d.setName(req.getName() != null ? req.getName() : req.getDescription());
        d.setDescription(req.getDescription());
        desktopRepository.save(d);
        log.info("createDesktop — desktop created id={} name={}", d.getId(), d.getName());
        return DesktopDto.from(d);
    }

    @Transactional
    public DesktopDto createDesktop(DesktopCreateRequest req) {
        log.debug("createDesktop(DesktopCreateRequest) — name={}", req.getName());
        Desktop d = new Desktop();
        d.setName(req.getName() != null ? req.getName() : req.getDescription());
        d.setDescription(req.getDescription());
        desktopRepository.save(d);
        if (req.getGroups() != null) {
            for (DesktopCreateRequest.SubGroupRequest sgReq : req.getGroups()) {
                DesktopGroup g = new DesktopGroup();
                g.setName(sgReq.getName() != null ? sgReq.getName() : sgReq.getDescription());
                g.setDescription(sgReq.getDescription());
                g.getDesktops().add(d);
                desktopGroupRepository.save(g);
                log.debug("createDesktop — created sub-group name={} for desktopId={}", g.getName(), d.getId());
            }
        }
        log.info("createDesktop — desktop created id={} name={} groups={}", d.getId(), d.getName(),
                req.getGroups() != null ? req.getGroups().size() : 0);
        return DesktopDto.from(d);
    }

    @Transactional
    public DesktopDto updateDesktop(Integer id, DesktopDto req) {
        log.debug("updateDesktop — id={}", id);
        Desktop d = desktopRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Desktop not found: " + id));
        if (req.getName() != null) d.setName(req.getName());
        if (req.getDescription() != null) d.setDescription(req.getDescription());
        desktopRepository.save(d);
        log.info("updateDesktop — updated id={}", id);
        return DesktopDto.from(d);
    }

    @Transactional
    public void deleteDesktop(Integer id) {
        log.debug("deleteDesktop — id={}", id);
        desktopRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Desktop not found: " + id));
        desktopRepository.deleteById(id);
        log.info("deleteDesktop — deleted id={}", id);
    }

    @Transactional(readOnly = true)
    public List<DesktopGroupDto> getAllDesktopGroups() {
        log.debug("getAllDesktopGroups");
        return desktopGroupRepository.findAll().stream().map(DesktopGroupDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DesktopGroupDto getDesktopGroupById(Integer id) {
        log.debug("getDesktopGroupById — id={}", id);
        return DesktopGroupDto.from(desktopGroupRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("DesktopGroup not found: " + id)));
    }

    @Transactional
    public DesktopGroupDto createDesktopGroup(DesktopGroupDto req) {
        log.debug("createDesktopGroup — name={}", req.getName());
        DesktopGroup g = new DesktopGroup();
        g.setName(req.getName() != null ? req.getName() : req.getDescription());
        g.setDescription(req.getDescription());
        desktopGroupRepository.save(g);
        log.info("createDesktopGroup — created id={} name={}", g.getId(), g.getName());
        return DesktopGroupDto.from(g);
    }

    @Transactional
    public DesktopGroupDto updateDesktopGroup(Integer id, DesktopGroupDto req) {
        log.debug("updateDesktopGroup — id={}", id);
        DesktopGroup g = desktopGroupRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("DesktopGroup not found: " + id));
        if (req.getName() != null) g.setName(req.getName());
        if (req.getDescription() != null) g.setDescription(req.getDescription());
        desktopGroupRepository.save(g);
        log.info("updateDesktopGroup — updated id={}", id);
        return DesktopGroupDto.from(g);
    }

    @Transactional
    public void deleteDesktopGroup(Integer id) {
        log.debug("deleteDesktopGroup — id={}", id);
        desktopGroupRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("DesktopGroup not found: " + id));
        desktopGroupRepository.deleteById(id);
        log.info("deleteDesktopGroup — deleted id={}", id);
    }

    /**
     * Checks whether the given user may access the given desktop.
     *
     * <p>Logic (mirrors PHP UserService::userHasDesktopAccess()):</p>
     * <ol>
     *   <li>Return 404 if the desktop does not exist.</li>
     *   <li>Admins always receive {@code allow}.</li>
     *   <li>Disabled users always receive {@code deny}.</li>
     *   <li>Regular users receive {@code allow} if at least one of their UserGroups
     *       is linked to at least one DesktopGroup that contains this desktop.</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkUserDesktopAccess(Integer userId, Integer desktopId) {
        log.debug("checkUserDesktopAccess — userId={} desktopId={}", userId, desktopId);

        // Step 1: desktop must exist (404 if not)
        desktopRepository.findById(desktopId)
            .orElseThrow(() -> new NoSuchElementException("Desktop not found: " + desktopId));

        // Step 2: load user (any role via BaseUser)
        var base = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        // Step 3: admins always get ALLOW (doc Section 10)
        if ("admin".equals(base.getRole())) {
            log.info("checkUserDesktopAccess — admin bypass userId={} desktopId={}", userId, desktopId);
            return accessResult("allow", userId, base.getName());
        }

        // Step 4: disabled users always get DENY
        if (!base.isEnabled()) {
            log.warn("checkUserDesktopAccess — user disabled userId={}", userId);
            return accessResult("deny", userId, base.getName());
        }

        // Step 5: group-based check via single DB query
        long count = userRepository.countDesktopAccess(userId, desktopId);
        String result = count > 0 ? "allow" : "deny";
        log.info("checkUserDesktopAccess — userId={} desktopId={} result={}", userId, desktopId, result);
        return accessResult(result, userId, base.getName());
    }

    private static Map<String, Object> accessResult(String result, Integer userId, String userName) {
        return Map.of(
            "result", result,
            "user",   Map.of("id", userId, "name", userName)
        );
    }
}
