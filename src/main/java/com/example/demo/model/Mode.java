package com.example.demo.model;

import com.example.demo.enums.ModeType;
import com.example.demo.enums.MusicType; 
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private ModeType type;
    private MusicType musicType; 

    private Integer targetTemp;
    private Integer targetHumidity;
    private Integer targetCo2;
}