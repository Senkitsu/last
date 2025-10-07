package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.SensorType;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
public class Sensor 
{
    @Id
    private Long id; 
    @ManyToOne
    private Bus bus; 
    private SensorType type; 
    private Double value; 
    private LocalDateTime timestamp; 
}