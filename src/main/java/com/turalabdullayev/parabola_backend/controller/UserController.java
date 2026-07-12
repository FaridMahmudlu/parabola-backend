package com.turalabdullayev.parabola_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turalabdullayev.parabola_backend.dto.UserProfileUpdateRequest;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Controller", description = "İstifadəçi Profili və Bədən Ölçülərinin İdarə Edilməsi API-ları")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

	private final UserService userService;

	@jakarta.persistence.PersistenceContext
	private jakarta.persistence.EntityManager entityManager;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/debug-db")
	@io.swagger.v3.oas.annotations.Operation(hidden = true)
	public ResponseEntity<?> debugDb() {
		try {
			java.util.List<?> result = entityManager.createNativeQuery(
				"SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'users'"
			).getResultList();
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(e.getMessage());
		}
	}

	@PutMapping("/profile")
	@Operation(summary = "Profil və bədən ölçülərini yenilə variantı", description = "Giriş etmiş istifadəçinin Azərbaycan dilindəki boy, çəki və digər bədən ölçülərini bazada yeniləyir.")
	public ResponseEntity<String> updateProfile(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UserProfileUpdateRequest request) {
		String email = jwt.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			email = jwt.getSubject() + "@clerk.local";
		}
		String roleName = jwt.getClaimAsString("role");
		String result = userService.updateProfileWithRole(email, roleName, request);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/profile")
	@Operation(summary = "Cari istifadəçinin profil məlumatlarını gətir", description = "Token sahibinin bütün profil və bədən ölçüsü məlumatlarını geri qaytarır.")
	public ResponseEntity<User> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
		String email = jwt.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			email = jwt.getSubject() + "@clerk.local";
		}
		String roleName = jwt.getClaimAsString("role");
		User user = userService.getProfileOrOrCreate(email, roleName);
		return ResponseEntity.ok(user);
	}
}