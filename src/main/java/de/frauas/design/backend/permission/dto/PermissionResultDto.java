package de.frauas.design.backend.permission.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PermissionResultDto {
    private String result;
    private Map<String, Object> user;

    public static PermissionResultDto allow(Integer userId, String userName) {
        return PermissionResultDto.builder()
            .result("allow")
            .user(Map.of("id", userId, "name", userName))
            .build();
    }

    public static PermissionResultDto deny(Integer userId, String userName) {
        return PermissionResultDto.builder()
            .result("deny")
            .user(Map.of("id", userId, "name", userName))
            .build();
    }
}
