package com.turalabdullayev.parabola_backend;

import com.turalabdullayev.parabola_backend.repository.ProductRepository;
import com.turalabdullayev.parabola_backend.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DeleteProductTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void deleteTargetProduct() {
        // We will find by name and delete
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            if ("Klassik Köynək".equals(p.getName()) && "ZARA".equalsIgnoreCase(p.getBrand())) {
                System.out.println("Deleting product: " + p.getName() + " ID: " + p.getId());
                productRepository.delete(p);
            }
        }
    }
}
