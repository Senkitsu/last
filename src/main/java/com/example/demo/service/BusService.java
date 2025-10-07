package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
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


    public List<Bus> getAllBuses(String type, Pageable pageable) {
        return busRepository.findAll(pageable).getContent();
    }

    @Cacheable(value = "bus", key = "#id")
    public Bus getBusById(Long id) {
        return busRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "buses", allEntries = true)
    public Bus createBus(Bus bus) {
        return busRepository.save(bus);
    }

    @Transactional
    @CacheEvict(value = {"buses", "bus"}, allEntries = true)
    public Bus updateBus(Long id, Bus updatedBus) {
        Optional<Bus> existingBusOpt = busRepository.findById(id);
        if (existingBusOpt.isPresent()) {
            Bus existingBus = existingBusOpt.get();
            existingBus.setModel(updatedBus.getModel());
            existingBus.setLocation(updatedBus.getLocation());
            existingBus.setMode(updatedBus.getMode());
            existingBus.setSensors(updatedBus.getSensors());
            return busRepository.save(existingBus);
        }
        return null;
    }

    @Transactional
    @CacheEvict(value = {"buses", "bus"}, allEntries = true)
    public boolean deleteBus(Long id) {
        if (busRepository.existsById(id)) {
            busRepository.deleteById(id);
            return true;
        }
        return false;
    }
}