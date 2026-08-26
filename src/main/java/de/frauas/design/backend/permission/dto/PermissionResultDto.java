package de.frauas.design.backend.permission.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Response payload for permission checks (see {@code PermissionController} and
 * {@code PermissionService}).
 *
 * <p>Mirrors the JSON shape expected by design-daas:
 * <pre>{@code
 * {
 *   "result": "allow" | "deny",
 *   "user": { "id": 1, "name": "john" }
 * }
 * }</pre>
 */
@Data
@Builder
public class PermissionResultDto {

    private static final String RESULT_ALLOW = "allow";
    private static final String RESULT_DENY = "deny";

    /** Either {@code "allow"} or {@code "deny"}. */
    private String result;

    /** Basic info about the user the check was performed for: {@code {id, name}}. */
    private Map<String, Object> user;

    /**
     * Builds an "allow" result for the given user.
     *
     * @param userId   the user's ID
     * @param userName the user's display name
     */
    public static PermissionResultDto allow(Integer userId, String userName) {
        return of(RESULT_ALLOW, userId, userName);
    }

    /**
     * Builds a "deny" result for the given user.
     *
     * @param userId   the user's ID
     * @param userName the user's display name
     */
    public static PermissionResultDto deny(Integer userId, String userName) {
        return of(RESULT_DENY, userId, userName);
    }

    private static PermissionResultDto of(String result, Integer userId, String userName) {
        return PermissionResultDto.builder()
                .result(result)
                .user(Map.of("id", userId, "name", userName))
                .build();
    }
}
