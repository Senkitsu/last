package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Bus;
import com.example.demo.repository.BusRepository;


@Service
public class BusService {
    private final BusRepository busRepository;

    public BusService(BusRepository busRepository) {
        this.busRepository = busRepository;
    }
    @Cacheable(value = "buses", key = "#root.methodName")
    public List<Bus> getAll() {
        return busRepository.findAll();
    }

    @Cacheable(value = "bus", key = "#id")
    public Bus getById(Long id) {
        return busRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"buses"}, allEntries = true)
    public Bus create(Bus bus) {
        return busRepository.save(bus);
    }

    @Transactional
    @CacheEvict(value = {"buses", "bus"}, allEntries = true)
    public Bus updateById(Long id, Bus updatedBus) {
        return busRepository.findById(id)
                .map(bus -> {
                    bus.setId(updatedBus.getId());
                    bus.setModel(updatedBus.getModel());
                    bus.setSensors(updatedBus.getSensors());
                    bus.setLocation(updatedBus.getLocation());
                    bus.setMode(updatedBus.getMode());
                    return busRepository.save(bus);
                })
                .orElse(null);
    }
}
