package com.turalabdullayev.parabola_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turalabdullayev.parabola_backend.entity.Role;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.service.UserService;
import com.turalabdullayev.parabola_backend.service.ClerkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Controller", description = "Gizli İdarəetmə Paneli və Rol Təyinatı API-ları")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {

	private final UserService userService;
	private final ClerkService clerkService;

	public AdminController(UserService userService, ClerkService clerkService) {
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

	private boolean isAdmin(User user, Jwt jwt, String clerkRole, String headerEmail) {
		String email = extractEmail(jwt, headerEmail);
		if (email != null && ALLOWED_ADMIN_EMAILS.contains(email.toLowerCase().trim())) {
			return true;
		}
		if (user != null && user.getEmail() != null && ALLOWED_ADMIN_EMAILS.contains(user.getEmail().toLowerCase().trim())) {
			return true;
		}
		return false;
	}

	@GetMapping("/check")
	@Operation(summary = "Admin icazəsini yoxla")
	public ResponseEntity<?> checkAdmin(
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			return ResponseEntity.status(401).body(Map.of("isAdmin", false, "message", "Giriş edilməyib."));
		}
		String email = extractEmail(jwt, headerEmail);
		User user = userService.getProfileOrOrCreate(email, jwt.getSubject(), clerkRole);
		boolean adminAccess = isAdmin(user, jwt, clerkRole, headerEmail);

		if (adminAccess && user.getRole() != Role.ROLE_ADMIN) {
			user.setRole(Role.ROLE_ADMIN);
			userService.updateUserRoleByAdmin(user.getId(), "ROLE_ADMIN", jwt.getSubject());
		}

		return ResponseEntity.ok(Map.of("isAdmin", adminAccess, "role", user.getRole().name()));
	}

	@GetMapping("/users")
	@Operation(summary = "Bütün istifadəçilərin siyahısını gətir (Admin)")
	public ResponseEntity<?> getAllUsers(
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			return ResponseEntity.status(401).body(Map.of("message", "Giriş edilməyib."));
		}
		String email = extractEmail(jwt, headerEmail);
		User user = userService.getProfileOrOrCreate(email, jwt.getSubject(), clerkRole);
		if (!isAdmin(user, jwt, clerkRole, headerEmail)) {
			return ResponseEntity.status(403).body(Map.of("message", "Giriş qadağandır! Yalnız idarəçilər istifadəçi siyahısına baxa bilər."));
		}

		List<User> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@PutMapping("/users/{userId}/role")
	@Operation(summary = "İstifadəçinin rolunu dəyişdir (Admin)")
	public ResponseEntity<?> updateUserRole(
			@PathVariable Long userId,
			@RequestBody Map<String, String> body,
			@RequestHeader(value = "X-Clerk-Role", required = false) String clerkRole,
			@RequestHeader(value = "X-Clerk-User-Email", required = false) String headerEmail,
			@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			return ResponseEntity.status(401).body(Map.of("message", "Giriş edilməyib."));
		}
		String email = extractEmail(jwt, headerEmail);
		User adminUser = userService.getProfileOrOrCreate(email, jwt.getSubject(), clerkRole);
		if (!isAdmin(adminUser, jwt, clerkRole, headerEmail)) {
			return ResponseEntity.status(403).body(Map.of("message", "Giriş qadağandır! Yalnız idarəçilər rol dəyişə bilər."));
		}

		String newRole = body.get("role");
		if (newRole == null || newRole.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Yeni rol daxil edilməlidir."));
		}

		try {
			User updatedUser = userService.updateUserRoleByAdmin(userId, newRole, jwt.getSubject());
			return ResponseEntity.ok(updatedUser);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("message", "Xəta baş verdi: " + e.getMessage()));
		}
	}
}
