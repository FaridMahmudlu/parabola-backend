package com.turalabdullayev.parabola_backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				populateDimensionsFromFitAndManken(size);
			}
		}
		return productRepository.save(product);
	}

	private void populateDimensionsFromFitAndManken(ProductSize pSize) {
		String size = pSize.getSizeName() != null ? pSize.getSizeName().trim().toUpperCase() : "M";
		String fit = pSize.getClothingFit() != null ? pSize.getClothingFit().trim() : "Orta";
		String manken = pSize.getModelBodyType() != null ? pSize.getModelBodyType().trim() : "Orta";

		double chest = 95.0;
		double shoulder = 44.0;
		double armLength = 60.0;
		double totalLength = 70.0;

		// 1. Base sizes mapping
		if ("S".equals(size)) {
			chest = 90.0; shoulder = 40.0; armLength = 59.0; totalLength = 68.0;
		} else if ("M".equals(size)) {
			chest = 98.0; shoulder = 43.0; armLength = 61.0; totalLength = 70.0;
		} else if ("L".equals(size)) {
			chest = 106.0; shoulder = 45.0; armLength = 63.0; totalLength = 72.0;
		} else if ("XL".equals(size)) {
			chest = 114.0; shoulder = 47.0; armLength = 65.0; totalLength = 74.0;
		} else if ("XXL".equals(size)) {
			chest = 122.0; shoulder = 49.0; armLength = 67.0; totalLength = 76.0;
		}

		// 2. Adjust based on Manken - Ölçü tipi
		if (manken.contains("Daha arıq")) {
			chest -= 4.0; shoulder -= 2.0;
		} else if (manken.contains("Arıq")) {
			chest -= 2.0; shoulder -= 1.0;
		} else if (manken.contains("Orta iri")) {
			chest += 2.0; shoulder += 1.0;
		} else if (manken.contains("İri")) {
			chest += 4.0; shoulder += 2.0;
		}

		// 3. Adjust based on Geyim (Fit)
		if (fit.contains("Kiçik")) {
			chest -= 3.0;
		} else if (fit.contains("Orta kiçik")) {
			chest -= 1.5;
		} else if (fit.contains("Orta geniş")) {
			chest += 2.0;
		} else if (fit.contains("Geniş")) {
			chest += 5.0;
		}

		pSize.setChest(chest);
		pSize.setShoulder(shoulder);
		pSize.setArmLength(armLength);
		pSize.setTotalLength(totalLength);
	}

	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	public Map<String, Object> getProductDetailsWithRecommendation(Long productId, String userEmail) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new RuntimeException("Məhsul tapılmadı!"));

		User user = userRepository.findByEmail(userEmail)
				.orElseGet(() -> User.builder()
						.email(userEmail)
						.username(userEmail.split("@")[0])
						.password("")
						.role(com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER)
						.build());

		SizeRecommendationResponse recommendation = sizeEngineService.calculateBestSize(user, product);

		Map<String, Object> response = new HashMap<>();
		response.put("product", product);
		response.put("sizeRecommendation", recommendation);

		return response;
	}
}