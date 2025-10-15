package com.example.demo.model;

import java.util.List;

import com.example.demo.enums.SensorType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bus {
    @Id
    private Long id; 

    private String model;

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sensor> sensors; 

    private String location; 

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "mode_id")
    private Mode mode;
}