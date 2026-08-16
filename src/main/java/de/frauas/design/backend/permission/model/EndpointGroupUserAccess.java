package de.frauas.design.backend.permission.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "endpoint_group_user_access")
@Getter
@Setter
@NoArgsConstructor
public class EndpointGroupUserAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endpoint_group_id", nullable = false)
    private EndpointGroup endpointGroup;

    @Column(name = "allow_access", nullable = false)
    private boolean allowAccess;
}
