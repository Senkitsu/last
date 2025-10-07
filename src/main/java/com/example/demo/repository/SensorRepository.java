package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Bus;
import com.example.demo.model.Mode;
import com.example.demo.model.Product;
import com.example.demo.model.Sensor;

@Repository
public interface SensorRepository
            extends JpaRepository<Sensor, Long> {
}
