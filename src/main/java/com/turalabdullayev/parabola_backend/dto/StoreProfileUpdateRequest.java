package com.turalabdullayev.parabola_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProfileUpdateRequest {
	private String shopName;
	private String shopPhone;
	private String shopLink;
	private String shopBio;
}
