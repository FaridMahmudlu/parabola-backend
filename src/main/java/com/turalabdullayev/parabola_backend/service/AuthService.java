package com.turalabdullayev.parabola_backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.turalabdullayev.parabola_backend.dto.AuthResponse;
import com.turalabdullayev.parabola_backend.dto.LoginRequest;
import com.turalabdullayev.parabola_backend.dto.RegisterRequest;
import com.turalabdullayev.parabola_backend.entity.Role;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.repository.UserRepository;
import com.turalabdullayev.parabola_backend.security.JwtService;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}

	public String register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Bu email ünvanı artıq qeydiyyatdan keçib!");
		}

		String finalUsername = request.getUsername();
		if (finalUsername == null || finalUsername.trim().isEmpty()) {
			finalUsername = request.getEmail().split("@")[0];
		}

		if (userRepository.existsByUsername(finalUsername)) {
			finalUsername = finalUsername + "_" + System.currentTimeMillis() % 1000;
		}

		User user = User.builder().username(finalUsername).email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.role(request.getEmail().startsWith("zara") ? Role.ROLE_SELLER : Role.ROLE_USER).build();

		userRepository.save(user);

		return "İstifadəçi uğurla qeydiyyatdan keçdi!";
	}

	public AuthResponse login(LoginRequest request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new UsernameNotFoundException("İstifadəçi tapılmadı: " + request.getEmail()));

		String jwtToken = jwtService.generateToken(user.getEmail());

		return new AuthResponse(jwtToken, user.getUsername(), user.getEmail(), user.getRole().name());
	}
}