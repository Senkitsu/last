package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
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


    public List<Mode> getAll(Pageable pageable) {
        return modeRepository.findAll(pageable).getContent();
    }

    @Cacheable(value = "mode", key = "#id")
    public Mode getById(Long id) {
        return modeRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "modes", allEntries = true)
    public Mode create(Mode mode) {
        return modeRepository.save(mode);
    }

    @Transactional
    @CacheEvict(value = {"modes", "mode"}, allEntries = true)
    public Mode updateById(Long id, Mode updatedMode) {
        Optional<Mode> existingModeOpt = modeRepository.findById(id);
        if (existingModeOpt.isPresent()) {
            Mode existingMode = existingModeOpt.get();
            existingMode.setMusicType(updatedMode.getMusicType());
            existingMode.setTargetTemp(updatedMode.getTargetTemp());
            existingMode.setTargetHumidity(updatedMode.getTargetHumidity());
            existingMode.setTargetCo2(updatedMode.getTargetCo2());
            return modeRepository.save(existingMode);
        }
        return null;
    }

    @Transactional
    @CacheEvict(value = {"modes", "mode"}, allEntries = true)
    public boolean deleteMode(Long id) {
        if (modeRepository.existsById(id)) {
            modeRepository.deleteById(id);
            return true;
        }
        return false;
    }
}