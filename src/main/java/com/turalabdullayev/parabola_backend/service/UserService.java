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

	public String updateProfile(String email, UserProfileUpdateRequest request) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("İstifadəçi tapılmadı: " + email));

		user.setHeight(request.getHeight());
		user.setWeight(request.getWeight());
		user.setChest(request.getChest());
		user.setArmLength(request.getArmLength());
		user.setWaist(request.getWaist());
		user.setShoulder(request.getShoulder());

		userRepository.save(user);

		return "Profil məlumatlarınız və bədən ölçüləriniz uğurla yadda saxlanıldı!";
	}

	public User getProfile(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("İstifadəçi tapılmadı: " + email));
	}
}