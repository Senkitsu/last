package com.example.demo;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.model.Bus;
import com.example.demo.service.BusService;

import static org.assertj.core.api.Assertions.assertThat; // ← добавьте этот импорт

@SpringBootTest
class BusServiceTest {

    @Autowired
    private BusService busService;

    @Test
    void getAllBuses_ReturnsEmptyList_WhenNoBuses() {
        List<Bus> buses = busService.getAllBuses();
        assertThat(buses).isEmpty(); // ✅ теперь работает
    }
}