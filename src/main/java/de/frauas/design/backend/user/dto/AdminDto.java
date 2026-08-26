package de.frauas.design.backend.user.dto;

import de.frauas.design.backend.user.model.Admin;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDto {
    private Integer id;
    private String name;
    private String email;
    private String role;

    /** Maps an {@link Admin} entity to its API representation. */
    public static AdminDto from(Admin admin) {
        return AdminDto.builder()
                .id(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .build();
    }
}
