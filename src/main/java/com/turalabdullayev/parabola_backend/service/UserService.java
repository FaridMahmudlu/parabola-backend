package com.turalabdullayev.parabola_backend.service;

import org.springframework.stereotype.Service;

import com.turalabdullayev.parabola_backend.dto.UserProfileUpdateRequest;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.entity.Product;
import com.turalabdullayev.parabola_backend.repository.UserRepository;
import com.turalabdullayev.parabola_backend.repository.ProductRepository;
import java.util.List;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final ProductRepository productRepository;

	public UserService(UserRepository userRepository, ProductRepository productRepository) {
		this.userRepository = userRepository;
		this.productRepository = productRepository;
	}

	public User getProfileOrOrCreate(String email, String roleName) {
		com.turalabdullayev.parabola_backend.entity.Role dbRole = com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER;
		if (roleName != null) {
			try {
				dbRole = com.turalabdullayev.parabola_backend.entity.Role.valueOf(roleName);
			} catch (IllegalArgumentException e) {
				// Fallback to ROLE_USER if roleName doesn't match db enum
			}
		}

		final com.turalabdullayev.parabola_backend.entity.Role finalRole = dbRole;
		User user = userRepository.findByEmail(email)
				.orElseGet(() -> {
					String username = email != null ? email : "user";
					if (email != null && email.contains("@")) {
						username = email.split("@")[0];
					}
					User newUser = User.builder()
							.email(email)
							.username(username)
							.password("") // oauth/sso users have no passwords in local db
							.role(finalRole)
							.build();
					return userRepository.save(newUser);
				});

		// Only update role if explicitly provided in JWT token
		if (roleName != null) {
			if (user.getRole() != finalRole) {
				user.setRole(finalRole);
				user = userRepository.save(user);
			}
		}

		return user;
	}

	public String updateProfileWithRole(String email, String roleName, UserProfileUpdateRequest request) {
		User user = getProfileOrOrCreate(email, roleName);

		String gender = request.getGender();
		String clothingSize = request.getClothingSize();
		String bodyType = request.getBodyType();
		String shopName = request.getShopName();

		user.setGender(gender);
		user.setClothingSize(clothingSize);
		user.setBodyType(bodyType);
		user.setShopName(shopName);

		// If user has updated their shop name, they are a seller
		if (shopName != null && !shopName.isBlank()) {
			user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER);
		} else if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
			user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER);
		}

		userRepository.save(user);

		// If user is a seller and has updated their shop name, sync all their products
		if (user.getRole() == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER && shopName != null && !shopName.isBlank()) {
			List<Product> products = productRepository.findBySellerEmail(email);
			if (products != null && !products.isEmpty()) {
				for (Product p : products) {
					p.setSellerName(shopName);
				}
				productRepository.saveAll(products);
			}
		}

		return "Profil məlumatlarınız uğurla yadda saxlanıldı!";
	}
}