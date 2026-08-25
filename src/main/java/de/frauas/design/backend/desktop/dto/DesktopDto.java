package de.frauas.design.backend.desktop.dto;

import de.frauas.design.backend.desktop.model.Desktop;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopDto {
    private Integer id;
    private String name;
    private String description;
    /** IDs of DesktopGroups this desktop belongs to (mirrors PHP desktop_details.groups). */
    private List<Integer> groups;

    public static DesktopDto from(Desktop d) {
        return DesktopDto.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .groups(d.getDesktopGroups().stream().map(g -> g.getId()).toList())
                .build();
    }
}
