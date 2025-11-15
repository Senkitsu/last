package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.SensorType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ← добавлено!
    private Long id; 

    @ManyToOne
    @JoinColumn(name = "bus_id")
    private Bus bus; 

    private SensorType type; 
    private Double value; 
    private LocalDateTime timestamp; 
    private String pdf;
}