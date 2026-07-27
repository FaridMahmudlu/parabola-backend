package com.turalabdullayev.parabola_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.turalabdullayev.parabola_backend.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findByCategory(String category);

	List<Product> findBySellerEmail(String sellerEmail);

	List<Product> findBySellerNameIgnoreCaseOrderByIdDesc(String sellerName);

	@Query("SELECT p FROM Product p WHERE LOWER(TRIM(p.sellerName)) = LOWER(TRIM(:shopName)) ORDER BY p.id DESC")
	List<Product> findBySellerNameTrimmedIgnoreCase(@Param("shopName") String shopName);

	List<Product> findBySellerEmailIgnoreCaseOrderByIdDesc(String sellerEmail);
}