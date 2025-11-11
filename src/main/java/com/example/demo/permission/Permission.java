package com.example.demo.permission;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity @Data
public class Permission {
    @Id @GeneratedValue
    private Long id;
    private String title;
}