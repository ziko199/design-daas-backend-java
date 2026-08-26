package de.frauas.design.backend.user.model;

import de.frauas.design.backend.desktop.model.DesktopGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * A named group of {@link User}s, used to grant collective access to
 * {@link DesktopGroup}s (and transitively, the desktops within them).
 */
@Entity
@Table(name = "user_groups")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"users", "desktopGroups"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_group_to_desktop_group",
            joinColumns = @JoinColumn(name = "user_group_id"),
            inverseJoinColumns = @JoinColumn(name = "desktop_group_id"))
    private List<DesktopGroup> desktopGroups = new ArrayList<>();
}
