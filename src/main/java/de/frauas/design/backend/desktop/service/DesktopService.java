package de.frauas.design.backend.desktop.service;

import de.frauas.design.backend.desktop.dto.DesktopCreateRequest;
import de.frauas.design.backend.desktop.dto.DesktopDto;
import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.model.Desktop;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.desktop.repository.DesktopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesktopService {

    private final DesktopRepository desktopRepository;
    private final DesktopGroupRepository desktopGroupRepository;

    // ---- Desktops ----

    @Transactional(readOnly = true)
    public List<DesktopDto> getAllDesktops() {
        log.debug("getAllDesktops");
        return desktopRepository.findAll().stream().map(DesktopDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DesktopDto getDesktopById(Integer id) {
        log.debug("getDesktopById — id={}", id);
        return DesktopDto.from(desktopRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException("Desktop not found: " + id)));
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
        log.info(
                "createDesktop — desktop created id={} name={} groups={}",
                d.getId(),
                d.getName(),
                req.getGroups() != null ? req.getGroups().size() : 0);
        return DesktopDto.from(d);
    }

    @Transactional(readOnly = true)
    public List<DesktopGroupDto> getAllDesktopGroups() {
        log.debug("getAllDesktopGroups");
        return desktopGroupRepository.findAll().stream()
                .map(DesktopGroupDto::from)
                .toList();
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
}
