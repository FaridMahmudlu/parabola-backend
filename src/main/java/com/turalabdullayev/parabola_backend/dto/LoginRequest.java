package com.turalabdullayev.parabola_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
	@NotBlank(message = "Email sahəsi boş buraxıla bilməz")
	@Email(message = "Düzgün bir email ünvanı daxil edin")
	private String email;

	@NotBlank(message = "Şifrə sahəsi boş buraxıla bilməz")
	private String password;

}
