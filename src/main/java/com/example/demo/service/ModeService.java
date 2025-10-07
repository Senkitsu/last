package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Mode;
import com.example.demo.repository.ModeRepository;



@Service
public class ModeService {
    private final ModeRepository modeRepository;

    public ModeService(ModeRepository modeRepository) {
        this.modeRepository = modeRepository;
    }
    @Cacheable(value = "modes", key = "#root.methodName")
    public List<Mode> getAll() {
        return modeRepository.findAll();
    }

    @Cacheable(value = "mode", key = "#id")
    public Mode getById(Long id) {
        return modeRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"modes"}, allEntries = true)
    public Mode create(Mode mode) {
        return modeRepository.save(mode);
    }

    @Transactional
    @CacheEvict(value = {"modes", "mode"}, allEntries = true)
    public Mode updateById(Long id, Mode updatedMode) {
        return modeRepository.findById(id)
                .map(mode -> {
                    mode.setId(updatedMode.getId());
                    mode.setMusicType(updatedMode.getMusicType());
                    mode.setTargetTemp(updatedMode.getTargetTemp());
                    mode.setTargetHumidity(updatedMode.getTargetHumidity());
                    mode.setTargetCo2(updatedMode.getTargetCo2());
                    return modeRepository.save(mode);
                })
                .orElse(null);
    }
}
