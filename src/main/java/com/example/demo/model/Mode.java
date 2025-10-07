package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.MusicType;
import com.example.demo.enums.SensorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Mode {
    @Id
    private Long id; 
    private MusicType musicType; 
    private Double targetTemp; 
    private Double targetHumidity; 
    private Double targetCo2; 
}
