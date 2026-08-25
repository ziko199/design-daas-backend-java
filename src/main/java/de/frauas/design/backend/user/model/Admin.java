package de.frauas.design.backend.user.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** An administrator account; enabled by default and not subject to email verification. */
@Entity
@DiscriminatorValue("admin")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class Admin extends BaseUser {

    @Override
    public String getRole() {
        return "admin";
    }
}
