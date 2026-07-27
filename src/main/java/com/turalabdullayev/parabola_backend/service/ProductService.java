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

	@jakarta.annotation.PostConstruct
	public void cleanUpSellerNames() {
		try {
			List<Product> products = productRepository.findAll();
			boolean changed = false;
			for (Product p : products) {
				String email = p.getSellerEmail();
				String sName = p.getSellerName();
				
				// Trim all sellerNames to prevent whitespace matching issues
				if (sName != null && !sName.equals(sName.trim())) {
					p.setSellerName(sName.trim());
					sName = sName.trim();
					changed = true;
				}

				if ("mleykmahmudlu@gmail.com".equalsIgnoreCase(email) 
						|| (sName != null && sName.toLowerCase().contains("mleykmahmudlu"))) {
					if (!"Parabola Admin".equals(sName)) {
						p.setSellerName("Parabola Admin");
						changed = true;
					}
					Optional<User> uOpt = userRepository.findByEmail("mleykmahmudlu@gmail.com");
					if (uOpt.isPresent() && !"Parabola Admin".equals(uOpt.get().getShopName())) {
						User u = uOpt.get();
						u.setShopName("Parabola Admin");
						userRepository.save(u);
					}
				} else if (email != null && !email.isBlank()) {
					Optional<User> uOpt = userRepository.findByEmail(email);
					if (uOpt.isPresent() && uOpt.get().getShopName() != null && !uOpt.get().getShopName().isBlank()) {
						if (!uOpt.get().getShopName().equals(sName)) {
							p.setSellerName(uOpt.get().getShopName().trim());
							changed = true;
						}
					}
				}
			}
			if (changed) {
				productRepository.saveAll(products);
			}
			
			// Also trim shopNames in User table
			List<User> allUsers = userRepository.findAllByOrderByIdDesc();
			for (User u : allUsers) {
				if (u.getShopName() != null && !u.getShopName().equals(u.getShopName().trim())) {
					u.setShopName(u.getShopName().trim());
					userRepository.save(u);
				}
			}
		} catch (Exception e) {
			System.err.println("Məhsul satıcı adları təmizlənərkən xəta: " + e.getMessage());
		}
	}

	// --- CREATE ---
	public Product saveProduct(Product product, String sellerEmail, String sellerName) {
		if ((product.getContactLink() == null || product.getContactLink().isBlank()) && 
			(product.getContactPhone() == null || product.getContactPhone().isBlank())) {
			throw new IllegalArgumentException("Ən azı bir əlaqə vasitəsi (Telefon nömrəsi və ya İnstagram/TikTok linki) daxil edilməlidir!");
		}

		product.setSellerEmail(sellerEmail);
		
		Optional<User> sellerOpt = userRepository.findByEmail(sellerEmail);
		String finalShopName = sellerName;

		if ("mleykmahmudlu@gmail.com".equalsIgnoreCase(sellerEmail) 
				|| (finalShopName != null && finalShopName.toLowerCase().contains("mleykmahmudlu"))) {
			finalShopName = "Parabola Admin";
		} else if (sellerOpt.isPresent() && sellerOpt.get().getShopName() != null && !sellerOpt.get().getShopName().isBlank()) {
			finalShopName = sellerOpt.get().getShopName();
		} else if (finalShopName == null || finalShopName.isBlank() || finalShopName.endsWith("@clerk.local")) {
			finalShopName = (sellerEmail != null && sellerEmail.contains("@")) ? sellerEmail.split("@")[0] + " Mağazası" : "Satıcı Mağazası";
		}
		
		if (sellerOpt.isPresent()) {
			User seller = sellerOpt.get();
			if (seller.getShopName() == null || seller.getShopName().isBlank() || !seller.getShopName().equals(finalShopName)) {
				seller.setShopName(finalShopName);
				userRepository.save(seller);
			}
		}
		
		product.setSellerName(finalShopName);

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

		Product existing = productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Məhsul tapılmadı!"));

		if (!sellerEmail.equals(existing.getSellerEmail())) {
			throw new RuntimeException("Bu məhsul sizə aid deyil! Yalnız öz məhsullarınızı redaktə edə bilərsiniz.");
		}

		if ("mleykmahmudlu@gmail.com".equalsIgnoreCase(sellerEmail)) {
			existing.setSellerName("Parabola Admin");
		} else {
			Optional<User> sellerOpt = userRepository.findByEmail(sellerEmail);
			if (sellerOpt.isPresent() && sellerOpt.get().getShopName() != null && !sellerOpt.get().getShopName().isBlank()) {
				existing.setSellerName(sellerOpt.get().getShopName());
			}
		}
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

	public Map<String, Object> getStoreDetails(String shopName, String userEmail) {
		if (shopName == null || shopName.isBlank()) {
			throw new IllegalArgumentException("Mağaza adı daxil edilməlidir!");
		}

		// URL decode and trim the shopName
		String cleanShopName = shopName.trim();
		try {
			cleanShopName = java.net.URLDecoder.decode(cleanShopName, java.nio.charset.StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			// If decoding fails, continue with cleaned original
		}

		// 1. Try TRIM-based JPQL query first (handles DB values with trailing spaces)
		List<Product> products = productRepository.findBySellerNameTrimmedIgnoreCase(cleanShopName);

		// 2. Fallback: exact sellerName match
		if (products == null || products.isEmpty()) {
			products = productRepository.findBySellerNameIgnoreCaseOrderByIdDesc(cleanShopName);
		}

		// 3. Multi-stage seller User lookup
		Optional<User> sellerUserOpt = userRepository.findFirstByShopNameIgnoreCase(cleanShopName);
		if (sellerUserOpt.isEmpty()) {
			sellerUserOpt = userRepository.findFirstByShopNameTrimmedIgnoreCase(cleanShopName);
		}
		if (sellerUserOpt.isEmpty() && products != null && !products.isEmpty()) {
			for (Product p : products) {
				if (p.getSellerEmail() != null && !p.getSellerEmail().isBlank()) {
					sellerUserOpt = userRepository.findByEmail(p.getSellerEmail().trim());
					if (sellerUserOpt.isPresent()) break;
				}
			}
		}
		if (sellerUserOpt.isEmpty() && cleanShopName.contains("@")) {
			sellerUserOpt = userRepository.findByEmail(cleanShopName.trim());
		}

		// 4. Fallback: if still no products, try finding by seller email from User table
		if ((products == null || products.isEmpty()) && sellerUserOpt.isPresent()) {
			String sellerEmailFromUser = sellerUserOpt.get().getEmail();
			if (sellerEmailFromUser != null && !sellerEmailFromUser.isBlank()) {
				products = productRepository.findBySellerEmailIgnoreCaseOrderByIdDesc(sellerEmailFromUser);
			}
		}

		String contactPhone = null;
		String contactLink = null;
		String sellerEmail = null;
		String shopAvatarUrl = null;
		String shopBannerUrl = null;
		String shopBio = null;

		if (sellerUserOpt.isPresent()) {
			User sellerUser = sellerUserOpt.get();
			sellerEmail = sellerUser.getEmail();
			if (sellerUser.getShopName() != null && !sellerUser.getShopName().isBlank()) {
				cleanShopName = sellerUser.getShopName().trim();
			}
			if (sellerUser.getShopPhone() != null && !sellerUser.getShopPhone().isBlank()) {
				contactPhone = sellerUser.getShopPhone().trim();
			}
			if (sellerUser.getShopLink() != null && !sellerUser.getShopLink().isBlank()) {
				contactLink = sellerUser.getShopLink().trim();
			}
			if (sellerUser.getShopBio() != null && !sellerUser.getShopBio().isBlank()) {
				shopBio = sellerUser.getShopBio().trim();
			}
			if (sellerUser.getShopAvatarUrl() != null && !sellerUser.getShopAvatarUrl().isBlank()) {
				shopAvatarUrl = sellerUser.getShopAvatarUrl().trim();
			}
			if (sellerUser.getShopBannerUrl() != null && !sellerUser.getShopBannerUrl().isBlank()) {
				shopBannerUrl = sellerUser.getShopBannerUrl().trim();
			}
		}

		if (products != null && !products.isEmpty()) {
			for (Product p : products) {
				if (contactPhone == null && p.getContactPhone() != null && !p.getContactPhone().isBlank()) {
					contactPhone = p.getContactPhone();
				}
				if (contactLink == null && p.getContactLink() != null && !p.getContactLink().isBlank()) {
					contactLink = p.getContactLink();
				}
				if (sellerEmail == null && p.getSellerEmail() != null && !p.getSellerEmail().isBlank()) {
					sellerEmail = p.getSellerEmail();
				}
			}
		}

		// Sort products if userEmail provided
		if (userEmail != null && !userEmail.isBlank() && products != null && !products.isEmpty()) {
			Optional<User> optUser = userRepository.findByEmail(userEmail);
			if (optUser.isPresent()) {
				User user = optUser.get();
				if (user.getGender() != null || user.getClothingSize() != null) {
					List<Map.Entry<Product, Double>> scored = new ArrayList<>();
					for (Product p : products) {
						double score = calculateRelevanceScore(p, user);
						scored.add(Map.entry(p, score));
					}
					scored.sort(Comparator.<Map.Entry<Product, Double>, Double>comparing(Map.Entry::getValue).reversed());
					List<Product> sorted = new ArrayList<>();
					for (Map.Entry<Product, Double> entry : scored) {
						sorted.add(entry.getKey());
					}
					products = sorted;
				}
			}
		}

		// Extract categories from store products
		List<String> categories = new ArrayList<>();
		if (products != null) {
			for (Product p : products) {
				if (p.getCategory() != null && !p.getCategory().isBlank() && !categories.contains(p.getCategory())) {
					categories.add(p.getCategory());
				}
			}
		}

		Map<String, Object> storeData = new HashMap<>();
		storeData.put("shopName", cleanShopName);
		storeData.put("sellerEmail", sellerEmail);
		storeData.put("contactPhone", contactPhone);
		storeData.put("contactLink", contactLink);
		storeData.put("shopBio", shopBio);
		storeData.put("shopAvatarUrl", shopAvatarUrl);
		storeData.put("shopBannerUrl", shopBannerUrl);
		storeData.put("totalProducts", products != null ? products.size() : 0);
		storeData.put("products", products != null ? products : new ArrayList<>());
		storeData.put("categories", categories);
		storeData.put("storeFound", true);

		return storeData;
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