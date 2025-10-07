package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Sensor;
import com.example.demo.repository.SensorRepository;



@Service
public class SensorService {
    private final SensorRepository sensorRepository;

    public SensorService(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }
    @Cacheable(value = "sensors", key = "#root.methodName")
    public List<Sensor> getAll() {
        return sensorRepository.findAll();
    }

    @Cacheable(value = "sensor", key = "#id")
    public Sensor getById(Long id) {
        return sensorRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"sensors"}, allEntries = true)
    public Sensor create(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    @Transactional
    @CacheEvict(value = {"sensors", "sensor"}, allEntries = true)
    public Sensor updateById(Long id, Sensor updatedSensor) {
        return sensorRepository.findById(id)
                .map(sensor -> {
                    sensor.setId(updatedSensor.getId());
                    sensor.setBus(updatedSensor.getBus());
                    sensor.setType(updatedSensor.getType());
                    sensor.setValue(updatedSensor.getValue());
                    sensor.setTimestamp(updatedSensor.getTimestamp());
                    return sensorRepository.save(sensor);
                })
                .orElse(null);
    }
}
