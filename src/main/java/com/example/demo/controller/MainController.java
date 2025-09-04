package com.example.demo.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Product;

@RestController
public class MainController {
    @GetMapping("/products")
    private List<Product> getProducts(){
        return products;
    }
    private List<Product> products = new ArrayList<>(
        Arrays.asList(new Product(1l,"Agusha",120), new Product(2l,"Chudo",50)));
    @PostMapping("/products")
    private ResponseEntity<Product> addProduct(@RequestBody Product product){
    product.setId(3l);
    products.add(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    }