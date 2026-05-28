package com.turalabdullayev.parabola_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
	private String token;
	private String username;
	private String email;
	private String role;

}
