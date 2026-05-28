package com.turalabdullayev.parabola_backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.turalabdullayev.parabola_backend.dto.SizeRecommendationResponse;
import com.turalabdullayev.parabola_backend.entity.Product;
import com.turalabdullayev.parabola_backend.entity.ProductSize;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.repository.ProductRepository;
import com.turalabdullayev.parabola_backend.repository.UserRepository;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final SizeEngineService sizeEngineService;

	public ProductService(ProductRepository productRepository, UserRepository userRepository,
			SizeEngineService sizeEngineService) {
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.sizeEngineService = sizeEngineService;
	}

	public Product saveProduct(Product product) {
		if (product.getSizes() != null) {
			for (ProductSize size : product.getSizes()) {
				size.setProduct(product);
			}
		}
		return productRepository.save(product);
	}

	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	public Map<String, Object> getProductDetailsWithRecommendation(Long productId, String userEmail) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Məhsul tapılmadı!"));

		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UsernameNotFoundException("İstifadəçi tapılmadı!"));

		SizeRecommendationResponse recommendation = sizeEngineService.calculateBestSize(user, product);

		Map<String, Object> response = new HashMap<>();
		response.put("product", product);
		response.put("sizeRecommendation", recommendation);

		return response;
	}
}