package com.turalabdullayev.parabola_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateRequest {

	@NotNull(message = "Boy sahəsi boş buraxıla bilməz")
	@Min(value = 50, message = "Boy minimum 50 sm ola bilər")
	private Double height;

	@NotNull(message = "Çəki sahəsi boş buraxıla bilməz")
	@Min(value = 30, message = "Çəki minimum 30 kq ola bilər")
	private Double weight;

	@NotNull(message = "Sinə genişliyi boş buraxıla bilməz")
	@Min(value = 40, message = "Sinə genişliyi minimum 40 sm ola bilər")
	private Double chest;

	@NotNull(message = "Qol uzunluğu boş buraxıla bilməz")
	@Min(value = 30, message = "Qol uzunluğu minimum 30 sm ola bilər")
	private Double armLength;

	@NotNull(message = "Bel ölçüsü boş buraxıla bilməz")
	@Min(value = 40, message = "Bel ölçüsü minimum 40 sm ola bilər")
	private Double waist;

	@NotNull(message = "Çiyin genişliyi boş buraxıla bilməz")
	@Min(value = 30, message = "Çiyin genişliyi minimum 30 sm ola bilər")
	private Double shoulder;
}