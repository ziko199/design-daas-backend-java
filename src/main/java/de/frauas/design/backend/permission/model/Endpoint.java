package de.frauas.design.backend.permission.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "endpoints")
@Getter
@Setter
@NoArgsConstructor
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "function_name", nullable = false, unique = true)
    private String functionName;

    @Column
    private String description;
}
