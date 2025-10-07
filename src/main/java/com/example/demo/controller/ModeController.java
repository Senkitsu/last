package com.example.demo.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Mode;
import com.example.demo.service.ModeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/modes")
public class ModeController {

    private final ModeService modeService;

    public ModeController(ModeService modeService) {
        this.modeService = modeService;
    }

    @PostMapping
    public ResponseEntity<Mode> createMode(@Valid @RequestBody Mode mode) {
        Mode createdMode = modeService.create(mode);
        return new ResponseEntity<>(createdMode, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Mode> getAllModes(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type) {
        return modeService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mode> getModeById(@PathVariable Long id) {
        Mode mode = modeService.getById(id);
        if (mode != null) {
            return ResponseEntity.ok(mode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Mode> updateMode(@PathVariable Long id, @Valid @RequestBody Mode updatedMode) {
        Mode mode = modeService.updateById(id, updatedMode);
        if (mode != null) {
            return ResponseEntity.ok(mode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMode(@PathVariable Long id) {
        boolean deleted = modeService.deleteMode(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}