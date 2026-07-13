package com.turalabdullayev.parabola_backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turalabdullayev.parabola_backend.entity.Product;
import com.turalabdullayev.parabola_backend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Controller", description = "Geyim Kataloqu və Ölçü Alqoritmi API-ları")
@SecurityRequirement(name = "BearerAuth")
public class ProductController {

	private final ProductService productService;
	private final String UPLOAD_DIR = "./uploads/";

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	private String extractEmail(Jwt jwt) {
		String email = jwt.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			email = jwt.getSubject() + "@clerk.local";
		}
		return email;
	}

	private String extractUsername(Jwt jwt) {
		String name = jwt.getClaimAsString("name");
		if (name != null && !name.isBlank()) return name;
		String email = extractEmail(jwt);
		return email.contains("@") ? email.split("@")[0] : email;
	}

	private String uploadImage(MultipartFile file) throws IOException {
		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}
		String originalFileName = file.getOriginalFilename();
		String fileExtension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf("."))
				: ".jpg";
		String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
		Path filePath = uploadPath.resolve(uniqueFileName);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		String backendUrl = System.getenv("BACKEND_URL") != null ? System.getenv("BACKEND_URL") : "http://localhost:8080";
		return backendUrl + "/uploads/" + uniqueFileName;
	}

	// --- CREATE ---
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('SELLER')")
	@Operation(summary = "Yeni geyim əlavə et (Satıcı)")
	public ResponseEntity<Product> createProduct(
			@RequestPart("product") String productJson,
			@RequestPart("image") MultipartFile file,
			@AuthenticationPrincipal Jwt jwt) throws IOException {

		ObjectMapper objectMapper = new ObjectMapper();
		Product product = objectMapper.readValue(productJson, Product.class);

		String fileUrl = uploadImage(file);
		product.setImageUrl(fileUrl);

		String sellerEmail = extractEmail(jwt);
		String sellerName = extractUsername(jwt);

		Product savedProduct = productService.saveProduct(product, sellerEmail, sellerName);
		return ResponseEntity.ok(savedProduct);
	}

	// --- READ: Satıcının öz məhsulları ---
	@GetMapping("/my")
	@PreAuthorize("hasRole('SELLER')")
	@Operation(summary = "Satıcının öz məhsullarını gətir")
	public ResponseEntity<List<Product>> getMyProducts(@AuthenticationPrincipal Jwt jwt) {
		String sellerEmail = extractEmail(jwt);
		List<Product> products = productService.getProductsBySeller(sellerEmail);
		return ResponseEntity.ok(products);
	}

	// --- UPDATE ---
	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('SELLER')")
	@Operation(summary = "Məhsulu redaktə et (Satıcı)")
	public ResponseEntity<?> updateProduct(
			@PathVariable Long id,
			@RequestPart("product") String productJson,
			@RequestPart(value = "image", required = false) MultipartFile file,
			@AuthenticationPrincipal Jwt jwt) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Product updatedData = objectMapper.readValue(productJson, Product.class);

			if (file != null && !file.isEmpty()) {
				String fileUrl = uploadImage(file);
				updatedData.setImageUrl(fileUrl);
			}

			String sellerEmail = extractEmail(jwt);
			Product updated = productService.updateProduct(id, updatedData, sellerEmail);
			return ResponseEntity.ok(updated);
		} catch (RuntimeException e) {
			return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
		} catch (IOException e) {
			return ResponseEntity.status(500).body(Map.of("message", "Şəkil yüklənərkən xəta baş verdi."));
		}
	}

	// --- DELETE ---
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('SELLER')")
	@Operation(summary = "Məhsulu sil (Satıcı)")
	public ResponseEntity<?> deleteProduct(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		try {
			String sellerEmail = extractEmail(jwt);
			productService.deleteProduct(id, sellerEmail);
			return ResponseEntity.ok(Map.of("message", "Məhsul uğurla silindi!"));
		} catch (RuntimeException e) {
			return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
		}
	}

	// --- READ ALL (sıralanmış) ---
	@GetMapping
	@Operation(summary = "Bütün geyimləri uyğunluq sırası ilə gətir")
	public ResponseEntity<List<Product>> getAllProducts(@AuthenticationPrincipal Jwt jwt) {
		String email = extractEmail(jwt);
		List<Product> products = productService.getAllProductsSorted(email);
		return ResponseEntity.ok(products);
	}

	// --- READ SINGLE ---
	@GetMapping("/{id}")
	@Operation(summary = "Geyim detalı və ağıllı ölçü tövsiyəsi")
	public ResponseEntity<Map<String, Object>> getProductDetails(@PathVariable Long id,
			@AuthenticationPrincipal Jwt jwt) {
		String email = extractEmail(jwt);
		Map<String, Object> details = productService.getProductDetailsWithRecommendation(id, email);
		return ResponseEntity.ok(details);
	}
}