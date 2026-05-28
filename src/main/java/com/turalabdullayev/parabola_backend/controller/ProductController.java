package com.turalabdullayev.parabola_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	@PreAuthorize("hasRole('SELLER')")
	@Operation(summary = "Yeni geyim əlavə et (Satıcı)", description = "Sistemə satıcı rolu ilə daxil olmuş butiklər tərəfindən yeni geyim və onun S, M, L ölçü santimetrlərini yükləyir.")
	public ResponseEntity<Product> createProduct(@RequestBody Product product) {
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