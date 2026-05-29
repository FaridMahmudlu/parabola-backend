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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('SELLER')")
	@Operation(summary = "Yeni geyim və Real Şəkil əlavə et (Satıcı)", description = "Sistemə satıcı rolu ilə daxil olmuş butiklər tərəfindən yeni geyim məlumatlarını və kompüterdən seçilən real şəkil faylını yükləyir.")
	public ResponseEntity<Product> createProduct(@RequestPart("product") String productJson,
			@RequestPart("image") MultipartFile file) throws IOException {

		ObjectMapper objectMapper = new ObjectMapper();
		Product product = objectMapper.readValue(productJson, Product.class);

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

		String fileUrl = "http://localhost:8080/uploads/" + uniqueFileName;
		product.setImageUrl(fileUrl);

		Product savedProduct = productService.saveProduct(product);
		return ResponseEntity.ok(savedProduct);
	}

	@GetMapping
	@Operation(summary = "Bütün geyimləri siyahıla", description = "Sistemdəki bütün butiklərə aid geyimlərin ümumi siyahısını gətirir.")
	public ResponseEntity<List<Product>> getAllProducts() {
		List<Product> products = productService.getAllProducts();
		return ResponseEntity.ok(products);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Geyim detalı və ağıllı ölçü tövsiyəsi", description = "Kliklənən geyimin bütün detallarını, butik DM linkini və istifadəçinin bədəninə uyğun gələn ağıllı ölçü tövsiyəsini tək paketdə qaytarır.")
	public ResponseEntity<Map<String, Object>> getProductDetails(@PathVariable Long id,
			@AuthenticationPrincipal UserDetails userDetails) {
		Map<String, Object> details = productService.getProductDetailsWithRecommendation(id, userDetails.getUsername());
		return ResponseEntity.ok(details);
	}
}