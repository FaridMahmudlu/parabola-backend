package com.turalabdullayev.parabola_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SizeRecommendationResponse {
	private String recommendedSize;
	private Integer matchPercentage;
	private String feedbackMessage;
}