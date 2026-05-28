package com.turalabdullayev.parabola_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSize {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String sizeName;

	@Column(name = "chest_cm")
	private Double chest;

	@Column(name = "arm_length_cm")
	private Double armLength;

	@Column(name = "shoulder_cm")
	private Double shoulder;

	@Column(name = "total_length_cm")
	private Double totalLength;

	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	@JsonBackReference
	private Product product;
}