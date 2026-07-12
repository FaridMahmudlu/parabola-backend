package com.turalabdullayev.parabola_backend.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
					User newUser = User.builder()
							.email(email)
							.username(email.split("@")[0])
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

		user.setHeight(request.getHeight());
		user.setWeight(request.getWeight());
		user.setChest(request.getChest());
		user.setArmLength(request.getArmLength());
		user.setWaist(request.getWaist());
		user.setShoulder(request.getShoulder());

		userRepository.save(user);

		return "Profil məlumatlarınız və bədən ölçüləriniz uğurla yadda saxlanıldı!";
	}

}