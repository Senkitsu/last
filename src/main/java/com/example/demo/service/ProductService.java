package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.specifications.ProductSpecification;

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
    @CacheEvict(value = {"products"}, allEntries = true)
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product updateById(Long id, Product updatedProduct) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setTitle(updatedProduct.getTitle());
                    product.setCost(updatedProduct.getCost());
                    return productRepository.save(product);
                })
                .orElse(null);
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
        if (title != null) {
            return productRepository.findByTitleContainingIgnoreCase(title);
        }
        return productRepository.findAll();
    }

    public Page<Product> getByFilter(String title, Integer min, Integer max, Pageable pageable) {
        return productRepository.findAll(
            ProductSpecification.filter(title, min, max),
            pageable);
    }
}