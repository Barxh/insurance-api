package com.fioneer.insurance.user.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(min = 2, max = 50)
    private String firstName;
    @Size(min = 2, max = 50)
    private String lastName;
    @Column(unique = true, nullable = false, length = 50)
    @Size(min = 4, max = 50)
    private String username;
    @NotNull
    private String password;
    @Size(min = 2, max = 50)
    private String nationalId;
    @Size(min = 2, max = 50)
    private String street;
    private Integer streetNumber;
    private Integer zipCode;
    @Size(min = 2, max = 50)
    private String city;
    @Email
    private String email;

    @ManyToMany()
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
