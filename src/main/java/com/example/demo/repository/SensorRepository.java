// com.example.demo.repository.SensorRepository
package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.example.demo.enums.SensorType;
import com.example.demo.model.Sensor;

@Repository
public interface SensorRepository 
        extends JpaRepository<Sensor, Long>, JpaSpecificationExecutor<Sensor> {
    
    List<Sensor> findByType(SensorType type);
}