package com.turalabdullayev.parabola_backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Integer rating;

	@Column(length = 500)
	private String topics;

	@Column(length = 1000)
	private String comment;

	@Column(name = "page_path", length = 255)
	private String pagePath;

	@Column(name = "signed_in", nullable = false)
	private Boolean signedIn;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void setCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
