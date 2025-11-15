package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.SensorType;
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
    @CacheEvict(value = { "sensors", "sensor" }, allEntries = true)
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
    @CacheEvict(value = { "sensors", "sensor" }, allEntries = true)
    public boolean deleteById(Long id) {
        if (sensorRepository.existsById(id)) {
            sensorRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Вспомогательный метод: получить сенсор по строковому типу
    private Sensor getSensorByTypeString(String typeStr) {
        try {
            SensorType type = SensorType.valueOf(typeStr.toUpperCase());
            List<Sensor> sensors = sensorRepository.findByType(type);
            return sensors.isEmpty() ? null : sensors.get(0);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Анализ климата
    public String analyzeClimate() {
        StringBuilder actions = new StringBuilder();

        Sensor co2 = getSensorByTypeString("CO2");
        Sensor humidity = getSensorByTypeString("HUMIDITY");
        Sensor temp = getSensorByTypeString("TEMP");
        Sensor light = getSensorByTypeString("LIGHT");

        if (co2 != null && co2.getValue() != null && co2.getValue() >= 1200) {
            actions.append("Добавить кислород. ");
        }

        if (humidity != null && humidity.getValue() != null) {
            double h = humidity.getValue();
            if (h < 30) {
                actions.append("Включить увлажнитель воздуха. ");
            } else if (h > 60) {
                actions.append("Включить вытяжку. ");
            }
        }

        if (temp != null && temp.getValue() != null) {
            double t = temp.getValue();
            if (t > 28) {
                actions.append("Включить кондиционер. ");
            } else if (t < 15) {
                actions.append("Включить обогреватель. ");
            }
        }

        if (light != null && light.getValue() != null && light.getValue() < 30) {
            actions.append("Включить дополнительное освещение. ");
        }

        return actions.length() == 0 ? "Все параметры в норме." : actions.toString().trim();
    }

    @Transactional
    public void saveAll(List<Sensor> sensors) {
        sensorRepository.saveAll(sensors);
    }

    public Sensor addFileToSensorData(Long id, String filePath) {
        Optional<Sensor> existingSensorOpt = sensorRepository.findById(id);
        if (existingSensorOpt.isPresent()) {
            Sensor existing = existingSensorOpt.get();
            existing.setPdf(filePath);
            return sensorRepository.save(existing);
        }
        return null;
    }
}