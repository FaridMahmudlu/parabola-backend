package com.turalabdullayev.parabola_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateRequest {

	@NotBlank(message = "Cins seçilməlidir")
	private String gender;

	@NotBlank(message = "Geyim ölçüsü seçilməlidir")
	private String clothingSize;

	@NotBlank(message = "Bədən tipi seçilməlidir")
	private String bodyType;

	private String shopName;
}