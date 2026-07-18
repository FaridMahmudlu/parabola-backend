package com.turalabdullayev.parabola_backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

	// --- CREATE ---
	public Product saveProduct(Product product, String sellerEmail, String sellerName) {
		if ((product.getContactLink() == null || product.getContactLink().isBlank()) && 
			(product.getContactPhone() == null || product.getContactPhone().isBlank())) {
			throw new IllegalArgumentException("Ən azı bir əlaqə vasitəsi (Telefon nömrəsi və ya İnstagram/TikTok linki) daxil edilməlidir!");
		}

		product.setSellerEmail(sellerEmail);
		
		Optional<User> sellerOpt = userRepository.findByEmail(sellerEmail);
		if (sellerOpt.isEmpty() || sellerOpt.get().getShopName() == null || sellerOpt.get().getShopName().isBlank()) {
			throw new IllegalArgumentException("Məhsul yarada bilmək üçün profilinizdə mütləq Mağaza Adı daxil etməlisiniz!");
		}
		product.setSellerName(sellerOpt.get().getShopName());

		if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
			product.setImageUrl(product.getImageUrls().get(0));
		}

		if (product.getSizes() != null) {
			for (ProductSize size : product.getSizes()) {
				size.setProduct(product);
				setDefaultModelBodyType(size);
			}
		}
		return productRepository.save(product);
	}

	// --- READ: satıcının öz məhsulları ---
	public List<Product> getProductsBySeller(String sellerEmail) {
		return productRepository.findBySellerEmail(sellerEmail);
	}

	// --- UPDATE ---
	public Product updateProduct(Long id, Product updatedData, String sellerEmail) {
		if ((updatedData.getContactLink() == null || updatedData.getContactLink().isBlank()) && 
			(updatedData.getContactPhone() == null || updatedData.getContactPhone().isBlank())) {
			throw new IllegalArgumentException("Ən azı bir əlaqə vasitəsi (Telefon nömrəsi və ya İnstagram/TikTok linki) daxil edilməlidir!");
		}

		Optional<User> sellerOpt = userRepository.findByEmail(sellerEmail);
		if (sellerOpt.isEmpty() || sellerOpt.get().getShopName() == null || sellerOpt.get().getShopName().isBlank()) {
			throw new IllegalArgumentException("Məhsul redaktə edə bilmək üçün profilinizdə mütləq Mağaza Adı daxil etməlisiniz!");
		}

		Product existing = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Məhsul tapılmadı!"));

		if (!sellerEmail.equals(existing.getSellerEmail())) {
			throw new RuntimeException("Bu məhsul sizə aid deyil! Yalnız öz məhsullarınızı redaktə edə bilərsiniz.");
		}

		existing.setSellerName(sellerOpt.get().getShopName());
		existing.setName(updatedData.getName());
		existing.setBrand(updatedData.getBrand());
		existing.setCategory(updatedData.getCategory());
		existing.setPrice(updatedData.getPrice());
		existing.setContactLink(updatedData.getContactLink());
		existing.setContactPhone(updatedData.getContactPhone());
		existing.setGender(updatedData.getGender());
		existing.setColor(updatedData.getColor());
		existing.setStyle(updatedData.getStyle());
		existing.setDescription(updatedData.getDescription());

		if (updatedData.getImageUrls() != null && !updatedData.getImageUrls().isEmpty()) {
			existing.setImageUrls(updatedData.getImageUrls());
			existing.setImageUrl(updatedData.getImageUrls().get(0));
		}

		// Update sizes
		if (updatedData.getSizes() != null && !updatedData.getSizes().isEmpty()) {
			existing.getSizes().clear();
			for (ProductSize size : updatedData.getSizes()) {
				size.setProduct(existing);
				setDefaultModelBodyType(size);
				existing.getSizes().add(size);
			}
		}

		return productRepository.save(existing);
	}

	// --- DELETE ---
	public void deleteProduct(Long id, String sellerEmail) {
		Product existing = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Məhsul tapılmadı!"));

		if (!sellerEmail.equals(existing.getSellerEmail())) {
			throw new RuntimeException("Bu məhsul sizə aid deyil! Yalnız öz məhsullarınızı silə bilərsiniz.");
		}

		productRepository.delete(existing);
	}

	// --- READ ALL (sıralama ilə) ---
	public List<Product> getAllProductsSorted(String userEmail) {
		List<Product> all = productRepository.findAll();

		// Try to find user profile for sorting
		Optional<User> optUser = userRepository.findByEmail(userEmail);
		if (optUser.isEmpty()) {
			return all; // No profile, return default order
		}

		User user = optUser.get();
		String userGender = user.getGender();
		String userSize = user.getClothingSize();

		// If user has no profile preferences, return default
		if (userGender == null && userSize == null) {
			return all;
		}

		// Calculate relevance score for each product and sort
		List<Map.Entry<Product, Double>> scored = new ArrayList<>();
		for (Product p : all) {
			double score = calculateRelevanceScore(p, user);
			scored.add(Map.entry(p, score));
		}

		scored.sort(Comparator.<Map.Entry<Product, Double>, Double>comparing(Map.Entry::getValue).reversed());

		List<Product> sorted = new ArrayList<>();
		for (Map.Entry<Product, Double> entry : scored) {
			sorted.add(entry.getKey());
		}
		return sorted;
	}

	private double calculateRelevanceScore(Product product, User user) {
		double score = 0.0;

		// 1. Gender match (40 points)
		if (user.getGender() != null && product.getGender() != null) {
			if (user.getGender().equalsIgnoreCase(product.getGender())
					|| "Unisex".equalsIgnoreCase(product.getGender())) {
				score += 40.0;
			}
		}

		// 2. Size match (35 points)
		if (user.getClothingSize() != null && product.getSizes() != null) {
			for (ProductSize ps : product.getSizes()) {
				if (user.getClothingSize().equalsIgnoreCase(ps.getSizeName())) {
					score += 35.0;
					break;
				}
			}
		}

		// 3. Body type & Fit match (25 points)
		if (user.getBodyType() != null && product.getSizes() != null && !product.getSizes().isEmpty()) {
			String userBody = user.getBodyType().trim().toLowerCase();
			
			for (ProductSize ps : product.getSizes()) {
				String fit = ps.getClothingFit() != null ? ps.getClothingFit().trim().toLowerCase() : "orta";
				boolean isPerfect = false;
				boolean isCompatible = false;

				if (userBody.contains("arıq") || userBody.contains("slim")) {
					if (fit.contains("kiçik") || fit.contains("orta kiçik")) isPerfect = true;
					else if (fit.contains("orta")) isCompatible = true;
				} else if (userBody.contains("normal") || userBody.contains("regular")) {
					if (fit.contains("orta")) isPerfect = true;
					else if (fit.contains("orta kiçik") || fit.contains("orta geniş")) isCompatible = true;
				} else if (userBody.contains("idman") || userBody.contains("athletic")) {
					if (fit.contains("orta") || fit.contains("orta geniş")) isPerfect = true;
					else if (fit.contains("geniş")) isCompatible = true;
				} else if (userBody.contains("kilolu") || userBody.contains("heavy")) {
					if (fit.contains("geniş")) isPerfect = true;
					else if (fit.contains("orta geniş")) isCompatible = true;
				}

				if (isPerfect) {
					score += 25.0;
					break;
				} else if (isCompatible) {
					score += 18.0;
					break;
				}
			}
		}

		return score;
	}

	// --- READ ALL (no sorting, legacy) ---
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

	private void setDefaultModelBodyType(ProductSize pSize) {
		String fit = pSize.getClothingFit() != null ? pSize.getClothingFit().trim() : "Orta";
		String manken = pSize.getModelBodyType() != null ? pSize.getModelBodyType().trim() : "";

		if (manken.isEmpty() || "Orta".equals(manken)) {
			if (fit.contains("Kiçik")) {
				manken = "Arıq";
			} else if (fit.contains("Orta kiçik")) {
				manken = "Arıq";
			} else if (fit.contains("Orta geniş")) {
				manken = "Normal";
			} else if (fit.contains("Geniş")) {
				manken = "Kilolu";
			} else {
				manken = "Normal";
			}
			pSize.setModelBodyType(manken);
		}
	}
}