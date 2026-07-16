package com.turalabdullayev.parabola_backend.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ClerkService {

	private final RestTemplate restTemplate;

	@Value("${clerk.secret-key:sk_test_6yj8K8ql1MgpG1V4OzD2o8cPwfrjGNwsh9uL9nXdWt}")
	private String clerkSecretKey;

	public ClerkService() {
		this.restTemplate = new RestTemplate();
	}

	public String getUserRole(String clerkUserId) {
		if (clerkUserId == null || clerkUserId.isBlank()) {
			return null;
		}
		try {
			String url = "https://api.clerk.com/v1/users/" + clerkUserId;
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(clerkSecretKey);
			HttpEntity<Void> entity = new HttpEntity<>(headers);

			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<Map<String, Object>>() {}
			);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Map<String, Object> body = response.getBody();
				@SuppressWarnings("unchecked")
				Map<String, Object> publicMetadata = (Map<String, Object>) body.get("public_metadata");
				if (publicMetadata != null) {
					return (String) publicMetadata.get("role");
				}
			}
		} catch (Exception e) {
			System.err.println("Error fetching user role from Clerk: " + e.getMessage());
		}
		return null;
	}
}
