package com.turalabdullayev.parabola_backend.service;

import org.springframework.stereotype.Service;

import com.turalabdullayev.parabola_backend.dto.UserProfileUpdateRequest;
import com.turalabdullayev.parabola_backend.entity.User;
import com.turalabdullayev.parabola_backend.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
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

		if (user.getRole() != finalRole) {
			user.setRole(finalRole);
			user = userRepository.save(user);
		}

		return user;
	}

	public String updateProfileWithRole(String email, String roleName, UserProfileUpdateRequest request) {
		User user = getProfileOrOrCreate(email, roleName);

		String gender = request.getGender();
		String clothingSize = request.getClothingSize();
		String bodyType = request.getBodyType();

		user.setGender(gender);
		user.setClothingSize(clothingSize);
		user.setBodyType(bodyType);

		// Estimate physical dimensions based on Cins, Geyim Ölçüsü, Bədən Tipi
		estimateAndSetDimensions(user, gender, clothingSize, bodyType);

		userRepository.save(user);

		return "Profil məlumatlarınız və bədən ölçüləriniz uğurla yadda saxlanıldı!";
	}

	private void estimateAndSetDimensions(User user, String gender, String size, String bodyType) {
		// Normalization
		String normGender = gender.trim().toLowerCase(); // "kişi", "qadın"
		String normSize = size.trim().toUpperCase(); // "S", "M", "L", "XL"
		String normBody = bodyType.trim().toLowerCase(); // "arıq", "normal", "idmançı", "kilolu"

		double height = 170.0;
		double weight = 70.0;
		double chest = 95.0;
		double armLength = 60.0;
		double waist = 80.0;
		double shoulder = 44.0;

		boolean isMale = normGender.contains("kiş") || normGender.contains("kis") || normGender.contains("male");

		if (isMale) {
			if ("S".equals(normSize)) {
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 170; weight = 60; chest = 88; shoulder = 40; armLength = 59; waist = 76;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 172; weight = 65; chest = 92; shoulder = 41; armLength = 60; waist = 80;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 174; weight = 70; chest = 96; shoulder = 43; armLength = 60; waist = 78;
				} else { // overweight / kilolu
					height = 170; weight = 75; chest = 100; shoulder = 41; armLength = 59; waist = 88;
				}
			} else if ("M".equals(normSize)) {
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 175; weight = 68; chest = 94; shoulder = 42; armLength = 61; waist = 82;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 177; weight = 73; chest = 98; shoulder = 43; armLength = 62; waist = 86;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 179; weight = 78; chest = 102; shoulder = 45; armLength = 62; waist = 84;
				} else {
					height = 175; weight = 84; chest = 106; shoulder = 43; armLength = 61; waist = 94;
				}
			} else if ("L".equals(normSize)) {
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 180; weight = 76; chest = 100; shoulder = 44; armLength = 63; waist = 88;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 182; weight = 82; chest = 104; shoulder = 45; armLength = 64; waist = 92;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 184; weight = 88; chest = 108; shoulder = 47; armLength = 64; waist = 90;
				} else {
					height = 180; weight = 94; chest = 112; shoulder = 45; armLength = 63; waist = 100;
				}
			} else { // XL
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 185; weight = 84; chest = 106; shoulder = 46; armLength = 65; waist = 94;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 187; weight = 90; chest = 110; shoulder = 47; armLength = 66; waist = 98;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 189; weight = 96; chest = 114; shoulder = 49; armLength = 66; waist = 96;
				} else {
					height = 185; weight = 104; chest = 118; shoulder = 47; armLength = 65; waist = 108;
				}
			}
		} else { // Female (Qadın)
			if ("S".equals(normSize)) {
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 160; weight = 48; chest = 80; shoulder = 35; armLength = 55; waist = 62;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 162; weight = 53; chest = 84; shoulder = 36; armLength = 56; waist = 66;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 164; weight = 57; chest = 88; shoulder = 38; armLength = 56; waist = 64;
				} else {
					height = 160; weight = 62; chest = 92; shoulder = 36; armLength = 55; waist = 72;
				}
			} else if ("M".equals(normSize)) {
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 165; weight = 54; chest = 86; shoulder = 37; armLength = 57; waist = 68;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 167; weight = 59; chest = 90; shoulder = 38; armLength = 58; waist = 72;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 169; weight = 63; chest = 94; shoulder = 40; armLength = 58; waist = 70;
				} else {
					height = 165; weight = 69; chest = 98; shoulder = 38; armLength = 57; waist = 78;
				}
			} else if ("L".equals(normSize)) {
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 170; weight = 60; chest = 92; shoulder = 39; armLength = 59; waist = 74;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 172; weight = 65; chest = 96; shoulder = 40; armLength = 60; waist = 78;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 174; weight = 69; chest = 100; shoulder = 42; armLength = 60; waist = 76;
				} else {
					height = 170; weight = 75; chest = 104; shoulder = 40; armLength = 59; waist = 84;
				}
			} else { // XL
				if (normBody.contains("arıq") || normBody.contains("slim")) {
					height = 175; weight = 66; chest = 98; shoulder = 41; armLength = 61; waist = 80;
				} else if (normBody.contains("normal") || normBody.contains("regular")) {
					height = 177; weight = 71; chest = 102; shoulder = 42; armLength = 62; waist = 84;
				} else if (normBody.contains("idman") || normBody.contains("athletic")) {
					height = 179; weight = 75; chest = 106; shoulder = 44; armLength = 62; waist = 82;
				} else {
					height = 175; weight = 82; chest = 110; shoulder = 42; armLength = 61; waist = 90;
				}
			}
		}

		user.setHeight(height);
		user.setWeight(weight);
		user.setChest(chest);
		user.setArmLength(armLength);
		user.setWaist(waist);
		user.setShoulder(shoulder);
	}

}