package com.turalabdullayev.parabola_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turalabdullayev.parabola_backend.dto.FeedbackRequest;
import com.turalabdullayev.parabola_backend.entity.Feedback;
import com.turalabdullayev.parabola_backend.repository.FeedbackRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "Feedback Controller", description = "İstifadəçi rəylərinin qəbulu")
public class FeedbackController {

	private final FeedbackRepository feedbackRepository;

	public FeedbackController(FeedbackRepository feedbackRepository) {
		this.feedbackRepository = feedbackRepository;
	}

	@PostMapping
	@Operation(summary = "İstifadəçi rəyini saxla")
	public ResponseEntity<Map<String, Object>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
		List<String> topics = request.getTopics() == null ? List.of() : request.getTopics().stream().distinct().toList();
		String comment = request.getComment() == null ? "" : request.getComment().trim();

		Feedback feedback = Feedback.builder()
				.rating(request.getRating())
				.topics(String.join(",", topics))
				.comment(comment)
				.pagePath(request.getPagePath() == null ? "" : request.getPagePath().trim())
				.signedIn(Boolean.TRUE.equals(request.getSignedIn()))
				.build();

		Feedback savedFeedback = feedbackRepository.save(feedback);

		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
				"id", savedFeedback.getId(),
				"message", "Rəyiniz uğurla qeydə alındı."
		));
	}
}
