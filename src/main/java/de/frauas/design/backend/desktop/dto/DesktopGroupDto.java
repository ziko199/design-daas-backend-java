package de.frauas.design.backend.desktop.dto;

import de.frauas.design.backend.desktop.model.Desktop;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopGroupDto {
    private Integer id;
    private String name;
    private String description;
    private List<Integer> desktopIds;

    public static DesktopGroupDto from(DesktopGroup g) {
        return DesktopGroupDto.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .desktopIds(g.getDesktops().stream().map(Desktop::getId).toList())
                .build();
    }
}
