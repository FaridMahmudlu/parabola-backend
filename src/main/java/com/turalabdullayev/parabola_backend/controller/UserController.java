package com.turalabdullayev.parabola_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turalabdullayev.parabola_backend.dto.UserProfileUpdateRequest;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.entity.Role;
import com.turalabdullayev.parabola_backend.service.UserService;
import com.turalabdullayev.parabola_backend.service.ClerkService;

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
	private final ClerkService clerkService;

	public UserController(UserService userService, ClerkService clerkService) {
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

	@PutMapping("/profile")
	@Operation(summary = "Profil və bədən ölçülərini yenilə variantı", description = "Giriş etmiş istifadəçinin Azərbaycan dilindəki boy, çəki və digər bədən ölçülərini bazada yeniləyir.")
	public ResponseEntity<String> updateProfile(
			@AuthenticationPrincipal Jwt jwt,
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@Valid @RequestBody UserProfileUpdateRequest request) {
		String email = extractEmail(jwt, headerEmail);
		String roleName = clerkRole != null && !clerkRole.isBlank() ? clerkRole : jwt.getClaimAsString("role");
		String result = userService.updateProfileWithRole(email, roleName, request);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/profile")
	@Operation(summary = "Cari istifadəçinin profil məlumatlarını gətir", description = "Token sahibinin bütün profil və bədən ölçüsü məlumatlarını geri qaytarır.")
	public ResponseEntity<User> getMyProfile(
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		String email = extractEmail(jwt, headerEmail);
		String roleName = clerkRole != null && !clerkRole.isBlank() ? clerkRole : jwt.getClaimAsString("role");
		User user = userService.getProfileOrOrCreate(email, jwt.getSubject(), roleName);
		
		boolean isAdminOrSeller = ALLOWED_ADMIN_EMAILS.contains(email.toLowerCase().trim()) 
				|| user.getRole() == Role.ROLE_ADMIN 
				|| user.getRole() == Role.ROLE_SELLER 
				|| "ROLE_ADMIN".equalsIgnoreCase(roleName) 
				|| "ROLE_SELLER".equalsIgnoreCase(roleName);

		if (isAdminOrSeller && (user.getShopName() == null || user.getShopName().isBlank())) {
			String displayName = user.getUsername();
			if (displayName == null || displayName.isBlank() || displayName.endsWith("@clerk.local")) {
				displayName = email.contains("@") ? email.split("@")[0] : email;
			}
			user.setShopName(displayName + " Mağazası");
			if (ALLOWED_ADMIN_EMAILS.contains(email.toLowerCase().trim())) {
				user.setRole(Role.ROLE_ADMIN);
			} else if (user.getRole() != Role.ROLE_ADMIN) {
				user.setRole(Role.ROLE_SELLER);
			}
			userService.updateUserRoleByAdmin(user.getId(), user.getRole().name(), jwt.getSubject());
		}
		
		return ResponseEntity.ok(user);
	}
}