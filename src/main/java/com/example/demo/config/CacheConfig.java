package com.example.demo.config;

import java.util.ArrayList;
import java.util.Arrays;

import org.aspectj.weaver.tools.cache.SimpleCache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public org.springframework.cache.CacheManager cacheManager() {
        SimpleCacheManager scm = new SimpleCacheManager();
        scm.setCaches(Arrays.asList(new ConcurrentMapCache("products"), 
        new ConcurrentMapCache("product")));
        return scm;
    }
}