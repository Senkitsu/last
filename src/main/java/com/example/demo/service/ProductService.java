package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#root.methodName")
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Cacheable(value = "product", key = "#id")
    public Product getById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product updateById(Long id, Product updatedProduct) {
        Optional<Product> existingProductOpt = productRepository.findById(id);
        if (existingProductOpt.isPresent()) {
            Product existing = existingProductOpt.get();
            existing.setTitle(updatedProduct.getTitle());
            existing.setCost(updatedProduct.getCost());
            return productRepository.save(existing);
        }
        return null;
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public boolean deleteById(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Product> getByTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return productRepository.findAll();
        }
        return productRepository.findByTitleContainingIgnoreCase(title.trim());
    }

    }