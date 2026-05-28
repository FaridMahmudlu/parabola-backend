package com.turalabdullayev.parabola_backend.service;

import org.springframework.stereotype.Service;

import com.turalabdullayev.parabola_backend.dto.SizeRecommendationResponse;
import com.turalabdullayev.parabola_backend.entity.Product;
import com.turalabdullayev.parabola_backend.entity.ProductSize;
import com.turalabdullayev.parabola_backend.entity.User;

@Service
public class SizeEngineService {

	public SizeRecommendationResponse calculateBestSize(User user, Product product) {
		if (user.getChest() == null || user.getShoulder() == null || user.getArmLength() == null) {
			return new SizeRecommendationResponse("Təyin edilmədi", 0,
					"Zəhmət olmasa əvvəlcə profilinizdə bədən ölçülərinizi tamamlayın.");
		}

		ProductSize bestSize = null;
		double highestScore = -1.0;

		for (ProductSize pSize : product.getSizes()) {
			double currentScore = evaluateSizeMatch(user, pSize);

			if (currentScore > highestScore) {
				highestScore = currentScore;
				bestSize = pSize;
			}
		}

		if (bestSize == null) {
			return new SizeRecommendationResponse("Tapılmadı", 0,
					"Təəssüf ki, bu geyimin ölçüləri sizin bədən quruluşunuza uyğun gəlmir.");
		}

		int finalPercentage = (int) Math.round(highestScore * 100);

		String message = generateAzerbaijaniFeedback(user, bestSize, product.getCategory());

		return new SizeRecommendationResponse(bestSize.getSizeName(), finalPercentage, message);
	}

	private double evaluateSizeMatch(User user, ProductSize size) {
		if (size.getChest() < user.getChest() || size.getShoulder() < user.getShoulder()) {
			return 0.0;
		}

		double chestDiff = Math.abs((size.getChest() - user.getChest()) - 5.0);
		double shoulderDiff = Math.abs((size.getShoulder() - user.getShoulder()) - 2.0);
		double armDiff = Math.abs((size.getArmLength() - user.getArmLength()) - 1.0);

		double totalPenalty = (chestDiff * 0.5) + (shoulderDiff * 0.3) + (armDiff * 0.2);

		double score = 1.0 - (totalPenalty / 20.0);
		return Math.max(0.0, Math.min(1.0, score));
	}

	private String generateAzerbaijaniFeedback(User user, ProductSize size, String category) {
		StringBuilder sb = new StringBuilder();
		sb.append(size.getSizeName()).append(" ölçüsü bu ").append(category.toLowerCase())
				.append(" üçün sizə ən uyğun variantdır. ");

		double chestDiff = size.getChest() - user.getChest();
		if (chestDiff <= 3) {
			sb.append("Sinə hissəsi bədəninizi tam saracaq (Slim-fit). ");
		} else if (chestDiff > 8) {
			sb.append("Sinə hissəsi olduqca rahat və boş (Oversize) qalacaq. ");
		} else {
			sb.append("Sinə hissəsi ideal rahatlıqda oturacaq. ");
		}

		return sb.toString();
	}
}