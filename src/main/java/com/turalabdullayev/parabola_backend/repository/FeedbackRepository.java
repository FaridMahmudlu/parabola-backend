package com.turalabdullayev.parabola_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.turalabdullayev.parabola_backend.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	List<Feedback> findTop200ByOrderByCreatedAtDesc();
}
