package com.turalabdullayev.parabola_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

	private String username;

	@NotBlank(message = "Email boş buraxıla bilməz")
	@Email(message = "Düzgün bir email ünvanı daxil edin")
	private String email;

	@NotBlank(message = "Şifrə boş buraxıla bilməz")
	@Size(min = 6, message = "Şifrə ən azı 6 simvoldan ibarət olmalıdır")
	private String password;
}