package de.frauas.design.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.frauas.design.backend.user.model.UserGroup;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupDto {
    private Integer id;
    private String name;
    private String description;

    /**
     * Optional — only used in PATCH /user_group/{id} request body.
     * Full replacement of group members: supply the complete list of user IDs.
     * Omit (or set to null) to leave existing members unchanged.
     * Not included in response bodies.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Integer> userIds;

    public static UserGroupDto from(UserGroup group) {
        return UserGroupDto.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            // userIds intentionally omitted from response
            .build();
    }
}
