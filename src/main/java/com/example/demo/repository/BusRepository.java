package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Bus;
import com.example.demo.model.Product;

@Repository
public interface BusRepository
            extends JpaRepository<Bus, Long> {
}
