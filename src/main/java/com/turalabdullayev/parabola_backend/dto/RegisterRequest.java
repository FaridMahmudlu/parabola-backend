package com.turalabdullayev.parabola_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
	@NotBlank(message = "İstifadəçi adı boş buraxıla bilməz")
	@Size(min = 3, max = 20, message = "İstifadəçi adı minimum 3, maksimum 20 simvoldan ibarət olmalıdır")
	private String username;

	@NotBlank(message = "Email boş buraxıla bilməz")
	@Email(message = "Düzgün bir email ünvanı daxil edin")
	private String email;

	@NotBlank(message = "Şifrə boş buraxıla bilməz")
	@Size(min = 6, message = "Şifrə ən azı 6 simvoldan ibarət olmalıdır")
	private String password;

}
