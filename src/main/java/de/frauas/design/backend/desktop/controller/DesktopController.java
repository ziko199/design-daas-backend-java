package de.frauas.design.backend.desktop.controller;

import de.frauas.design.backend.desktop.dto.DesktopCreateRequest;
import de.frauas.design.backend.desktop.dto.DesktopDto;
import de.frauas.design.backend.desktop.service.DesktopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class DesktopController {

    private final DesktopService desktopService;

    @GetMapping("/desktops")
    public ResponseEntity<List<DesktopDto>> getAllDesktops() {
        log.info("GET /desktops — listing all desktops");
        List<DesktopDto> desktops = desktopService.getAllDesktops();
        log.debug("GET /desktops — returning {} desktops", desktops.size());
        return ResponseEntity.ok(desktops);
    }

    @PostMapping("/desktop")
    public ResponseEntity<DesktopDto> createDesktop(@RequestBody DesktopCreateRequest request) {
        log.info("POST /desktop — creating desktop name={}", request.getName());
        DesktopDto result = desktopService.createDesktop(request);
        log.info("POST /desktop — desktop created id={} name={}", result.getId(), result.getName());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/desktop/{id}")
    public ResponseEntity<DesktopDto> getDesktopById(@PathVariable Integer id) {
        log.info("GET /desktop/{} — fetching desktop", id);
        return ResponseEntity.ok(desktopService.getDesktopById(id));
    }

    @PutMapping("/desktop/{id}")
    public ResponseEntity<DesktopDto> updateDesktop(
            @PathVariable Integer id,
            @RequestBody DesktopDto request) {
        log.info("PUT /desktop/{} — updating desktop", id);
        DesktopDto result = desktopService.updateDesktop(id, request);
        log.info("PUT /desktop/{} — updated successfully", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/desktop/{id}")
    public ResponseEntity<Void> deleteDesktop(@PathVariable Integer id) {
        log.info("DELETE /desktop/{} — deleting desktop", id);
        desktopService.deleteDesktop(id);
        log.info("DELETE /desktop/{} — deleted successfully", id);
        return ResponseEntity.ok().build();
    }
}
