package com.example.demo.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Bus;
import com.example.demo.service.BusService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/buses")
public class BusController {

    private final BusService busService;

    public BusController(BusService busService) {
        this.busService = busService;
    }

    @PostMapping
    public ResponseEntity<Bus> createBus(@Valid @RequestBody Bus bus) {
        Bus createdBus = busService.createBus(bus);
        return new ResponseEntity<>(createdBus, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Bus> getAllBuses(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type) {
        return busService.getAllBuses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBusById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body("Некорректный идентификатор автобуса: ID должен быть положительным числом.");
        }
        Bus bus = busService.getBusById(id);
        if (bus != null) {
            return ResponseEntity.ok(bus);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBus(@PathVariable Long id, @Valid @RequestBody Bus updatedBus) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body("Некорректный идентификатор автобуса: ID должен быть положительным числом.");
        }
        Bus bus = busService.updateBus(id, updatedBus);
        if (bus != null) {
            return ResponseEntity.ok(bus);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBus(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body("Некорректный идентификатор автобуса: ID должен быть положительным числом.");
        }
        boolean deleted = busService.deleteBus(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}