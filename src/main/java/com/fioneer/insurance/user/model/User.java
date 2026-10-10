package com.fioneer.insurance.user.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "_user", schema = "public")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    @Size(min = 2, max = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    @Size(min = 2, max = 50)
    private String lastName;

    @Column(unique = true, nullable = false, length = 50)
    @Size(min = 4, max = 50)
    private String username;

    @Column(nullable = false)
    @NotNull
    private String password;

    @Column(name = "national_id", unique = true, length = 50)
    private String nationalId;

    @Column(length = 100)
    @Size(min = 2, max = 100)
    private String street;

    @Column(name = "street_number", length = 10)
    private String streetNumber;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(length = 50)
    @Size(min = 2, max = 50)
    private String city;

    @Email
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
