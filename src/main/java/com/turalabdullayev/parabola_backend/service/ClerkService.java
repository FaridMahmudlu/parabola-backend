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

	@Value("${clerk.secret-key:${CLERK_SECRET_KEY:sk_test_6yj8K8ql1MgpG1V4OzD2o8cPwfrjGNwsh9uL9nXdWt}}")
	private String clerkSecretKey;

	public ClerkService() {
		this.restTemplate = new RestTemplate();
	}

	private String getSecretKey() {
		String envKey = System.getenv("CLERK_SECRET_KEY");
		if (envKey != null && !envKey.isBlank()) {
			return envKey.trim();
		}
		if (clerkSecretKey != null && !clerkSecretKey.isBlank()) {
			return clerkSecretKey.trim();
		}
		return "sk_test_6yj8K8ql1MgpG1V4OzD2o8cPwfrjGNwsh9uL9nXdWt";
	}

	public String getUserRole(String clerkUserId) {
		if (clerkUserId == null || clerkUserId.isBlank()) {
			return null;
		}
		try {
			String url = "https://api.clerk.com/v1/users/" + clerkUserId;
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(getSecretKey());
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

	public boolean updateUserRoleInClerk(String clerkUserId, String newRole) {
		if (clerkUserId == null || clerkUserId.isBlank()) {
			return false;
		}
		try {
			String url = "https://api.clerk.com/v1/users/" + clerkUserId + "/metadata";
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(getSecretKey());
			headers.set("Content-Type", "application/json");

			Map<String, Object> body = Map.of(
				"public_metadata", Map.of("role", newRole)
			);
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				url,
				HttpMethod.PATCH,
				entity,
				new ParameterizedTypeReference<Map<String, Object>>() {}
			);
			return response.getStatusCode().is2xxSuccessful();
		} catch (Exception e) {
			System.err.println("Error updating user role in Clerk: " + e.getMessage());
			return false;
		}
	}

	public String getUserEmail(String clerkUserId) {
		if (clerkUserId == null || clerkUserId.isBlank()) {
			return null;
		}
		try {
			String url = "https://api.clerk.com/v1/users/" + clerkUserId;
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(getSecretKey());
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
				java.util.List<Map<String, Object>> emailAddresses = (java.util.List<Map<String, Object>>) body.get("email_addresses");
				if (emailAddresses != null && !emailAddresses.isEmpty()) {
					String primaryId = (String) body.get("primary_email_address_id");
					if (primaryId != null) {
						for (Map<String, Object> emailObj : emailAddresses) {
							if (primaryId.equals(emailObj.get("id"))) {
								return (String) emailObj.get("email_address");
							}
						}
					}
					return (String) emailAddresses.get(0).get("email_address");
				}
			}
		} catch (Exception e) {
			System.err.println("Error fetching user email from Clerk: " + e.getMessage());
		}
		return null;
	}

	public java.util.List<Map<String, Object>> getClerkUsersList() {
		try {
			String url = "https://api.clerk.com/v1/users?limit=500";
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(getSecretKey());
			HttpEntity<Void> entity = new HttpEntity<>(headers);

			ResponseEntity<java.util.List<Map<String, Object>>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				entity,
				new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {}
			);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				return response.getBody();
			}
		} catch (Exception e) {
			System.err.println("Error fetching user list from Clerk API: " + e.getMessage());
		}
		return java.util.Collections.emptyList();
	}
}
