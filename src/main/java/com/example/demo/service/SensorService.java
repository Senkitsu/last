package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(value = "sensors", allEntries = true)
    public Sensor create(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    @Transactional
    @CacheEvict(value = {"sensors", "sensor"}, allEntries = true)
    public Sensor updateById(Long id, Sensor updatedSensor) {
        Optional<Sensor> existingSensorOpt = sensorRepository.findById(id);
        if (existingSensorOpt.isPresent()) {
            Sensor existing = existingSensorOpt.get();
            existing.setBus(updatedSensor.getBus());
            existing.setType(updatedSensor.getType());
            existing.setValue(updatedSensor.getValue());
            existing.setTimestamp(updatedSensor.getTimestamp());
            return sensorRepository.save(existing);
        }
        return null;
    }

    @Transactional
    @CacheEvict(value = {"sensors", "sensor"}, allEntries = true)
    public boolean deleteById(Long id) {
        if (sensorRepository.existsById(id)) {
            sensorRepository.deleteById(id);
            return true;
        }
        return false;
    }
}