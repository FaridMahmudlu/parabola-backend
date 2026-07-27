package com.turalabdullayev.parabola_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.turalabdullayev.parabola_backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	Boolean existsByEmail(String email);

	Boolean existsByUsername(String username);

	java.util.List<User> findAllByOrderByIdDesc();

	Optional<User> findFirstByShopNameIgnoreCase(String shopName);

	@org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE LOWER(TRIM(u.shopName)) = LOWER(TRIM(:shopName))")
	Optional<User> findFirstByShopNameTrimmedIgnoreCase(@org.springframework.data.repository.query.Param("shopName") String shopName);
}
