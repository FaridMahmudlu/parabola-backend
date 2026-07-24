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
	private final ClerkService clerkService;

	public UserService(UserRepository userRepository, ProductRepository productRepository, ClerkService clerkService) {
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.clerkService = clerkService;
	}

	public User getProfileOrOrCreate(String email, String clerkUserId, String roleName) {
		com.turalabdullayev.parabola_backend.entity.Role parsedRole = null;
		if (roleName != null && !roleName.isBlank() && !"undefined".equalsIgnoreCase(roleName) && !"null".equalsIgnoreCase(roleName)) {
			try {
				parsedRole = com.turalabdullayev.parabola_backend.entity.Role.valueOf(roleName.toUpperCase());
			} catch (IllegalArgumentException e) {
				if ("SELLER".equalsIgnoreCase(roleName)) {
					parsedRole = com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER;
				} else if ("ADMIN".equalsIgnoreCase(roleName)) {
					parsedRole = com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN;
				}
			}
		}

		final com.turalabdullayev.parabola_backend.entity.Role finalRole = parsedRole != null ? parsedRole : com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER;
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

		// Upgrade role if explicitly ROLE_SELLER or ROLE_ADMIN is provided
		if (parsedRole == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER || parsedRole == com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
			if (user.getRole() != parsedRole) {
				user.setRole(parsedRole);
				user = userRepository.save(user);
			}
		}
		// If user has a shopName set in database, ensure their role is ROLE_SELLER (unless they are ADMIN)
		else if (user.getShopName() != null && !user.getShopName().isBlank()) {
			if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER && user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
				user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER);
				user = userRepository.save(user);
			}
		}
		// If no explicit role in JWT, but user is not ROLE_SELLER in DB, check Clerk API
		else if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER && clerkUserId != null) {
			String clerkRole = clerkService.getUserRole(clerkUserId);
			if ("ROLE_SELLER".equalsIgnoreCase(clerkRole) || "SELLER".equalsIgnoreCase(clerkRole)) {
				user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER);
				user = userRepository.save(user);
			}
		}

		return user;
	}

	public String updateProfileWithRole(String email, String roleName, UserProfileUpdateRequest request) {
		User user = getProfileOrOrCreate(email, null, roleName);

		String gender = request.getGender();
		String clothingSize = request.getClothingSize();
		String bodyType = request.getBodyType();
		String shopName = request.getShopName();

		boolean isUserSeller = user.getRole() == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER 
				|| "ROLE_SELLER".equalsIgnoreCase(roleName) 
				|| "SELLER".equalsIgnoreCase(roleName);

		if (isUserSeller && (shopName == null || shopName.isBlank())) {
			throw new IllegalArgumentException("Satıcılar üçün Mağaza adı mütləq daxil edilməlidir!");
		}

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

	public List<User> getAllUsers() {
		return userRepository.findAllByOrderByIdDesc();
	}

	public User updateUserRoleByAdmin(Long targetUserId, String newRoleStr, String clerkUserId) {
		User targetUser = userRepository.findById(targetUserId)
				.orElseThrow(() -> new IllegalArgumentException("İstifadəçi tapılmadı! ID: " + targetUserId));

		com.turalabdullayev.parabola_backend.entity.Role targetRole;
		try {
			targetRole = com.turalabdullayev.parabola_backend.entity.Role.valueOf(newRoleStr.toUpperCase());
		} catch (Exception e) {
			if ("SELLER".equalsIgnoreCase(newRoleStr)) {
				targetRole = com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER;
			} else if ("ADMIN".equalsIgnoreCase(newRoleStr)) {
				targetRole = com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN;
			} else {
				targetRole = com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER;
			}
		}

		targetUser.setRole(targetRole);

		if (targetRole == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER) {
			if (targetUser.getShopName() == null || targetUser.getShopName().isBlank()) {
				String shopDefaultName = targetUser.getUsername() != null ? targetUser.getUsername() + " Mağazası" : "Satıcı Mağazası";
				targetUser.setShopName(shopDefaultName);
			}
		}

		User saved = userRepository.save(targetUser);

		// Sync with Clerk if clerkUserId available
		if (clerkUserId != null && !clerkUserId.isBlank()) {
			clerkService.updateUserRoleInClerk(clerkUserId, targetRole.name());
		}

		return saved;
	}
}