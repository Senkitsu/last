package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.enums.SensorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Bus {
    @Id
    private Long id; 
    private String model;
    @OneToMany
    private List<Sensor> sensors; 
    private String location; 
    @OneToOne
    private Mode mode;
}
