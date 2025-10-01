package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

import com.example.demo.model.Product;

import jakarta.annotation.PostConstruct;

@Service

public class ProductService {
    private List<Product> products = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    public Product create(Product product)
    {
    Long id = idGenerator.getAndIncrement();
    product.setId(id);
    products.add(product);
    return product;
    }
    public List<Product> getAll(){
        return products;
    }
    @PostConstruct
    public void init(){
        if (products.isEmpty()) {
            System.out.println("Инициализация листа");
            create(new Product(null,"agusok",55));
        }
    }
}
