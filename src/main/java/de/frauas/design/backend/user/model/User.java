package de.frauas.design.backend.user.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/** A regular (non-admin) user; may belong to zero or more {@link UserGroup}s. */
@Entity
@DiscriminatorValue("user")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = "groups")
public class User extends BaseUser {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_to_user_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "user_group_id"))
    private List<UserGroup> groups = new ArrayList<>();

    @Override
    public String getRole() {
        return "user";
    }
}
