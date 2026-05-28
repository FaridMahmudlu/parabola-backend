package com.turalabdullayev.parabola_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turalabdullayev.parabola_backend.dto.AuthResponse;
import com.turalabdullayev.parabola_backend.dto.LoginRequest;
import com.turalabdullayev.parabola_backend.dto.RegisterRequest;
import com.turalabdullayev.parabola_backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication Controller", description = "İstifadəçi Qeydiyyatı və Giriş Əməliyyatları API-ları")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@Operation(summary = "Yeni istifadəçi qeydiyyatı", description = "İstifadəçi adı, email və şifrə ilə sistemdə yeni hesab açır.")
	public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
		String result = authService.register(request);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/login")
	@Operation(summary = "İstifadəçi girişi (Login)", description = "Email və şifrə ilə sistemə giriş edir və geriyə JWT Token qaytarır.")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request);
		return ResponseEntity.ok(response);
	}
}