package com.turalabdullayev.parabola_backend.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackRequest {

	@NotNull(message = "Qiymətləndirmə seçilməlidir")
	@Min(value = 1, message = "Qiymətləndirmə ən azı 1 olmalıdır")
	@Max(value = 5, message = "Qiymətləndirmə ən çoxu 5 ola bilər")
	private Integer rating;

	@Size(max = 7, message = "Ən çoxu 7 mövzu seçilə bilər")
	private List<@Pattern(
			regexp = "accurate|easy|design|inaccurate|slow|catalog|technical",
			message = "Yanlış feedback mövzusu"
	) String> topics;

	@Size(max = 250, message = "Rəy 250 simvoldan uzun ola bilməz")
	private String comment;

	@Size(max = 255, message = "Səhifə yolu 255 simvoldan uzun ola bilməz")
	private String pagePath;

	private Boolean signedIn;
}
