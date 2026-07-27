package com.turalabdullayev.parabola_backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turalabdullayev.parabola_backend.entity.Product;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.entity.Role;
import com.turalabdullayev.parabola_backend.service.ProductService;
import com.turalabdullayev.parabola_backend.service.SupabaseStorageService;
import com.turalabdullayev.parabola_backend.service.UserService;
import com.turalabdullayev.parabola_backend.service.ClerkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Controller", description = "Geyim Kataloqu və Ölçü Alqoritmi API-ları")
@SecurityRequirement(name = "BearerAuth")
public class ProductController {

	private final ProductService productService;
	private final SupabaseStorageService supabaseStorageService;
	private final UserService userService;
	private final ClerkService clerkService;

	public ProductController(ProductService productService, SupabaseStorageService supabaseStorageService, UserService userService, ClerkService clerkService) {
		this.productService = productService;
		this.supabaseStorageService = supabaseStorageService;
		this.userService = userService;
		this.clerkService = clerkService;
	}

	private static final java.util.Set<String> ALLOWED_ADMIN_EMAILS = java.util.Set.of(
		"mleykmahmudlu@gmail.com",
		"fariddmahmudlu2008@gmail.com",
		"qeyisovli@gmail.com"
	);

	private String extractEmail(Jwt jwt, String headerEmail) {
		if (headerEmail != null && !headerEmail.isBlank() && headerEmail.contains("@") && !headerEmail.endsWith("@clerk.local")) {
			return headerEmail.toLowerCase().trim();
		}
		if (jwt != null) {
			String email = jwt.getClaimAsString("email");
			if (email == null || email.isBlank()) {
				email = jwt.getClaimAsString("email_address");
			}
			if (email != null && !email.isBlank() && email.contains("@")) {
				return email.toLowerCase().trim();
			}
			String clerkUserId = jwt.getSubject();
			if (clerkUserId != null && !clerkUserId.isBlank()) {
				String realEmail = clerkService.getUserEmail(clerkUserId);
				if (realEmail != null && !realEmail.isBlank()) {
					return realEmail.toLowerCase().trim();
				}
			}
			return jwt.getSubject() + "@clerk.local";
		}
		return null;
	}

	private boolean isSellerOrAdmin(User user, String sellerEmail, String clerkRole) {
		if (sellerEmail != null && ALLOWED_ADMIN_EMAILS.contains(sellerEmail.toLowerCase().trim())) {
			return true;
		}
		if ("ROLE_SELLER".equalsIgnoreCase(clerkRole) || "ROLE_ADMIN".equalsIgnoreCase(clerkRole)) {
			return true;
		}
		if (user != null) {
			if (user.getRole() == Role.ROLE_SELLER || user.getRole() == Role.ROLE_ADMIN) {
				return true;
			}
			if (user.getShopName() != null && !user.getShopName().isBlank()) {
				return true;
			}
		}
		return false;
	}

	private String extractUsername(Jwt jwt, String email) {
		if (jwt != null) {
			String name = jwt.getClaimAsString("name");
			if (name != null && !name.isBlank()) return name;
		}
		if (email != null && email.contains("@")) {
			return email.split("@")[0];
		}
		return "Satıcı";
	}

	private List<String> uploadImages(List<MultipartFile> files) throws IOException {
		List<String> urls = new ArrayList<>();
		if (files == null) return urls;
		for (MultipartFile file : files) {
			if (file.isEmpty()) continue;
			
			// Validate file size (max 5MB)
			if (file.getSize() > 5 * 1024 * 1024) {
				throw new IllegalArgumentException("Şəkil faylının ölçüsü 5MB-dan çox ola bilməz!");
			}
			
			String imageUrl = supabaseStorageService.uploadFile(file);
			urls.add(imageUrl);
		}
		return urls;
	}

	// --- CREATE ---
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Yeni geyim əlavə et (Satıcı)")
	public ResponseEntity<?> createProduct(
			@RequestPart("product") String productJson,
			@RequestPart("images") List<MultipartFile> files,
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		try {
			if (jwt == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Giriş edilməyib."));
			}
			String sellerEmail = extractEmail(jwt, headerEmail);
			
			User user = userService.getProfileOrOrCreate(sellerEmail, jwt.getSubject(), clerkRole);
			if (!isSellerOrAdmin(user, sellerEmail, clerkRole)) {
				return ResponseEntity.status(403).body(Map.of("message", "Giriş qadağandır! Yalnız satıcılar və idarəçilər məhsul əlavə edə bilər."));
			}

			ObjectMapper objectMapper = new ObjectMapper();
			Product product = objectMapper.readValue(productJson, Product.class);

			List<String> fileUrls = uploadImages(files);
			product.setImageUrls(fileUrls);

			String sellerName = (user != null && user.getShopName() != null && !user.getShopName().isBlank()) 
					? user.getShopName() 
					: extractUsername(jwt, sellerEmail);

			Product savedProduct = productService.saveProduct(product, sellerEmail, sellerName);
			return ResponseEntity.ok(savedProduct);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "Xəta baş verdi: " + e.getMessage()));
		}
	}

	// --- READ: Satıcının öz məhsulları ---
	@GetMapping("/my")
	@Operation(summary = "Satıcının öz məhsullarını gətir")
	public ResponseEntity<?> getMyProducts(
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			return ResponseEntity.status(401).body(Map.of("message", "Giriş edilməyib."));
		}
		String sellerEmail = extractEmail(jwt, headerEmail);
		
		User user = userService.getProfileOrOrCreate(sellerEmail, jwt.getSubject(), clerkRole);
		if (!isSellerOrAdmin(user, sellerEmail, clerkRole)) {
			return ResponseEntity.status(403).body(Map.of("message", "Giriş qadağandır! Yalnız satıcılar və idarəçilər məhsullarını görə bilər."));
		}

		List<Product> products = productService.getProductsBySeller(sellerEmail);
		return ResponseEntity.ok(products);
	}

	// --- UPDATE ---
	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Məhsulu redaktə et (Satıcı)")
	public ResponseEntity<?> updateProduct(
			@PathVariable Long id,
			@RequestPart("product") String productJson,
			@RequestPart(value = "images", required = false) List<MultipartFile> files,
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		try {
			if (jwt == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Giriş edilməyib."));
			}
			String sellerEmail = extractEmail(jwt, headerEmail);
			
			User user = userService.getProfileOrOrCreate(sellerEmail, jwt.getSubject(), clerkRole);
			if (!isSellerOrAdmin(user, sellerEmail, clerkRole)) {
				return ResponseEntity.status(403).body(Map.of("message", "Giriş qadağandır! Yalnız satıcılar və idarəçilər məhsul yeniləyə bilər."));
			}

			ObjectMapper objectMapper = new ObjectMapper();
			Product updatedData = objectMapper.readValue(productJson, Product.class);

			if (files != null && !files.isEmpty() && !(files.size() == 1 && files.get(0).getOriginalFilename() != null && files.get(0).getOriginalFilename().equals("empty"))) {
				List<String> fileUrls = uploadImages(files);
				updatedData.setImageUrls(fileUrls);
			}

			Product updated = productService.updateProduct(id, updatedData, sellerEmail);
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (RuntimeException e) {
			return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", "Şəkillər yenilənərkən xəta baş verdi: " + e.getMessage()));
		}
	}

	// --- DELETE ---
	@DeleteMapping("/{id}")
	@Operation(summary = "Məhsulu sil (Satıcı)")
	public ResponseEntity<?> deleteProduct(
			@PathVariable Long id,
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		try {
			if (jwt == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Giriş edilməyib."));
			}
			String sellerEmail = extractEmail(jwt, headerEmail);
			
			User user = userService.getProfileOrOrCreate(sellerEmail, jwt.getSubject(), clerkRole);
			if (!isSellerOrAdmin(user, sellerEmail, clerkRole)) {
				return ResponseEntity.status(403).body(Map.of("message", "Giriş qadağandır! Yalnız satıcılar və idarəçilər məhsul silə bilər."));
			}

			productService.deleteProduct(id, sellerEmail);
			return ResponseEntity.ok(Map.of("message", "Məhsul uğurla silindi!"));
		} catch (RuntimeException e) {
			return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
		}
	}

	// --- READ ALL (sıralanmış) ---
	@GetMapping
	@Operation(summary = "Bütün geyimləri uyğunluq sırası ilə gətir")
	public ResponseEntity<List<Product>> getAllProducts(
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		String email = jwt != null ? extractEmail(jwt, headerEmail) : null;
		List<Product> products = productService.getAllProductsSorted(email);
		return ResponseEntity.ok(products);
	}

	// --- READ SINGLE ---
	@GetMapping("/{id}")
	@Operation(summary = "Geyim detalı və ağıllı ölçü tövsiyəsi")
	public ResponseEntity<Map<String, Object>> getProductDetails(@PathVariable Long id,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		String email = jwt != null ? extractEmail(jwt, headerEmail) : null;
		Map<String, Object> details = productService.getProductDetailsWithRecommendation(id, email);
		return ResponseEntity.ok(details);
	}

	// --- READ STORE ---
	@GetMapping("/store/{shopName}")
	@Operation(summary = "Butik Mağaza detallarını və geyimlərini gətir")
	public ResponseEntity<Map<String, Object>> getStoreDetails(
			@PathVariable String shopName,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		String email = jwt != null ? extractEmail(jwt, headerEmail) : null;
		Map<String, Object> storeData = productService.getStoreDetails(shopName, email);
		return ResponseEntity.ok(storeData);
	}
}