package de.frauas.design.backend.permission.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "endpoint_groups")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "endpoints")
public class EndpointGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "endpoint_group_members",
        joinColumns = @JoinColumn(name = "endpoint_group_id"),
        inverseJoinColumns = @JoinColumn(name = "endpoint_id")
    )
    private List<Endpoint> endpoints = new ArrayList<>();
}
