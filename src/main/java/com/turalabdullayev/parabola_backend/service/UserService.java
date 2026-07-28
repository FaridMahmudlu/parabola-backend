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
	private final SupabaseStorageService supabaseStorageService;

	public UserService(UserRepository userRepository, ProductRepository productRepository, ClerkService clerkService,
			SupabaseStorageService supabaseStorageService) {
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.clerkService = clerkService;
		this.supabaseStorageService = supabaseStorageService;
	}

	public User getProfileOrOrCreate(String email, String clerkUserId, String roleName) {
		com.turalabdullayev.parabola_backend.entity.Role parsedRole = null;
		if (roleName != null && !roleName.isBlank() && !"undefined".equalsIgnoreCase(roleName)
				&& !"null".equalsIgnoreCase(roleName)) {
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

		final com.turalabdullayev.parabola_backend.entity.Role finalRole = parsedRole != null ? parsedRole
				: com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER;
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
		if (parsedRole == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER
				|| parsedRole == com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
			if (user.getRole() != parsedRole) {
				user.setRole(parsedRole);
				user = userRepository.save(user);
			}
		}
		// If user has a shopName set in database, ensure their role is ROLE_SELLER
		// (unless they are ADMIN)
		else if (user.getShopName() != null && !user.getShopName().isBlank()) {
			if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER
					&& user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
				user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER);
				user = userRepository.save(user);
			}
		}
		// If no explicit role in JWT, but user is not ROLE_SELLER in DB, check Clerk
		// API
		else if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER
				&& clerkUserId != null) {
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

		// If user has updated their shop name, set role to ROLE_SELLER (unless they are
		// an admin)
		if (shopName != null && !shopName.isBlank()) {
			if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
				user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER);
			}
		} else if (user.getRole() != com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN) {
			user.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_USER);
		}

		userRepository.save(user);

		// If user is a seller and has updated their shop name, sync all their products
		if (user.getRole() == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER && shopName != null
				&& !shopName.isBlank()) {
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

	private static final java.util.Set<String> ALLOWED_ADMIN_EMAILS = java.util.Set.of(
		"mleykmahmudlu@gmail.com",
		"fariddmahmudlu2008@gmail.com",
		"qeyisovli@gmail.com"
	);

	public User updateStoreProfile(
			String callerEmail,
			String roleName,
			String originalShopName,
			com.turalabdullayev.parabola_backend.dto.StoreProfileUpdateRequest request,
			org.springframework.web.multipart.MultipartFile avatarFile,
			org.springframework.web.multipart.MultipartFile bannerFile) {

		if (callerEmail == null || callerEmail.isBlank()) {
			throw new IllegalArgumentException("İstifadəçi identifikasiyası tapılmadı!");
		}

		User callerUser = getProfileOrOrCreate(callerEmail, null, roleName);

		boolean isCallerAdmin = callerUser.getRole() == com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN
				|| "ROLE_ADMIN".equalsIgnoreCase(roleName)
				|| ALLOWED_ADMIN_EMAILS.contains(callerEmail.toLowerCase().trim());

		User targetUser = callerUser;

		// Try finding the target store's User record by originalShopName or shopName
		if (originalShopName != null && !originalShopName.isBlank()) {
			String cleanOrig = originalShopName.trim();
			java.util.Optional<User> foundUserOpt = userRepository.findFirstByShopNameIgnoreCase(cleanOrig);
			if (foundUserOpt.isEmpty()) {
				foundUserOpt = userRepository.findFirstByShopNameTrimmedIgnoreCase(cleanOrig);
			}
			if (foundUserOpt.isPresent()) {
				User foundUser = foundUserOpt.get();
				boolean isOwner = foundUser.getEmail() != null && foundUser.getEmail().equalsIgnoreCase(callerEmail);
				if (isCallerAdmin || isOwner) {
					targetUser = foundUser;
				} else {
					throw new IllegalArgumentException("İcazə verilmədi! Yalnız mağazanın sahibi və idarəçilər profili yeniləyə bilər.");
				}
			}
		}

		boolean isSellerOrAdmin = isCallerAdmin
				|| targetUser.getRole() == com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER
				|| targetUser.getRole() == com.turalabdullayev.parabola_backend.entity.Role.ROLE_ADMIN
				|| "ROLE_SELLER".equalsIgnoreCase(roleName);

		if (!isSellerOrAdmin) {
			targetUser.setRole(com.turalabdullayev.parabola_backend.entity.Role.ROLE_SELLER);
		}

		if (request.getShopName() != null && !request.getShopName().isBlank()) {
			String cleanShopName = request.getShopName().trim();
			if (cleanShopName.length() > 60) {
				throw new IllegalArgumentException("Mağaza adı maksimum 60 simvol ola bilər!");
			}

			// If shopName is changing, check uniqueness among other sellers
			if (!cleanShopName.equalsIgnoreCase(targetUser.getShopName())) {
				final Long targetId = targetUser.getId();
				userRepository.findFirstByShopNameIgnoreCase(cleanShopName).ifPresent(otherUser -> {
					if (!otherUser.getId().equals(targetId)) {
						throw new IllegalArgumentException(
								"Bu mağaza adı artıq başqa satıcı tərəfindən istifadə olunur!");
					}
				});
				targetUser.setShopName(cleanShopName);
			}
		}

		if (request.getShopPhone() != null) {
			targetUser.setShopPhone(request.getShopPhone().trim());
		}

		if (request.getShopLink() != null) {
			targetUser.setShopLink(request.getShopLink().trim());
		}

		if (request.getShopBio() != null) {
			String bio = request.getShopBio().trim();
			if (bio.length() > 1000) {
				throw new IllegalArgumentException("Açıqlama mətni maksimum 1000 simvol ola bilər!");
			}
			targetUser.setShopBio(bio);
		}

		// Handle Avatar Upload
		if (avatarFile != null && !avatarFile.isEmpty()) {
			if (avatarFile.getSize() > 5 * 1024 * 1024) {
				throw new IllegalArgumentException("Profil şəklinin ölçüsü 5MB-dan çox ola bilməz!");
			}
			try {
				String avatarUrl = supabaseStorageService.uploadFile(avatarFile);
				targetUser.setShopAvatarUrl(avatarUrl);
			} catch (Exception e) {
				throw new RuntimeException("Profil şəkli yüklənərkən xəta baş verdi: " + e.getMessage());
			}
		}

		// Handle Banner Upload
		if (bannerFile != null && !bannerFile.isEmpty()) {
			if (bannerFile.getSize() > 5 * 1024 * 1024) {
				throw new IllegalArgumentException("Baner şəklinin ölçüsü 5MB-dan çox ola bilməz!");
			}
			try {
				String bannerUrl = supabaseStorageService.uploadFile(bannerFile);
				targetUser.setShopBannerUrl(bannerUrl);
			} catch (Exception e) {
				throw new RuntimeException("Baner şəkli yüklənərkən xəta baş verdi: " + e.getMessage());
			}
		}

		User savedUser = userRepository.save(targetUser);

		// Sync shopName and contacts to all existing products of this target seller
		String sellerEmailToSync = savedUser.getEmail();
		if (sellerEmailToSync != null && !sellerEmailToSync.isBlank()) {
			List<Product> products = productRepository.findBySellerEmailIgnoreCaseOrderByIdDesc(sellerEmailToSync);
			if (products != null && !products.isEmpty()) {
				for (Product p : products) {
					if (savedUser.getShopName() != null && !savedUser.getShopName().isBlank()) {
						p.setSellerName(savedUser.getShopName());
					}
					if (savedUser.getShopPhone() != null && !savedUser.getShopPhone().isBlank()) {
						p.setContactPhone(savedUser.getShopPhone());
					}
					if (savedUser.getShopLink() != null && !savedUser.getShopLink().isBlank()) {
						p.setContactLink(savedUser.getShopLink());
					}
				}
				productRepository.saveAll(products);
			}
		}

		return savedUser;
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
				String shopDefaultName = targetUser.getUsername() != null ? targetUser.getUsername() + " Mağazası"
						: "Satıcı Mağazası";
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