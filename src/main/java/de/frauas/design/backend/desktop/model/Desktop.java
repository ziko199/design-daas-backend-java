package de.frauas.design.backend.desktop.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "desktops")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "desktopGroups")
public class Desktop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    /** Inverse side of DesktopGroup.desktops — allows DesktopDto to include group IDs. */
    @ManyToMany(mappedBy = "desktops", fetch = FetchType.LAZY)
    private List<DesktopGroup> desktopGroups = new ArrayList<>();
}
