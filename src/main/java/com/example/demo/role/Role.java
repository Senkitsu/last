package com.example.demo.role;

import java.util.HashSet;
import java.util.Set;

import com.example.demo.model.Permission;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Data;

@Entity @Data
public class Role {
    @Id @GeneratedValue
    private Long id;
    private String title;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Permission> permissions = new HashSet<>();
}