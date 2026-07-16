package com.turalabdullayev.parabola_backend.service;

import org.springframework.stereotype.Service;

import com.turalabdullayev.parabola_backend.dto.SizeRecommendationResponse;
import com.turalabdullayev.parabola_backend.entity.Product;
import com.turalabdullayev.parabola_backend.entity.ProductSize;
import com.turalabdullayev.parabola_backend.entity.User;

@Service
public class SizeEngineService {

	public SizeRecommendationResponse calculateBestSize(User user, Product product) {
		if (user.getGender() == null || user.getGender().isBlank()
				|| user.getClothingSize() == null || user.getClothingSize().isBlank()
				|| user.getBodyType() == null || user.getBodyType().isBlank()) {
			return new SizeRecommendationResponse("Təyin edilmədi", 0,
					"Zəhmət olmasa əvvəlcə profilinizdə Cins, Ölçü və Bədən Tipi seçimlərinizi tamamlayın.");
		}

		if (product.getSizes() == null || product.getSizes().isEmpty()) {
			return new SizeRecommendationResponse("Tapılmadı", 0,
					"Bu geyim üçün ölçü məlumatları əlavə edilməyib.");
		}

		// Gender check
		String userGender = user.getGender().trim().toLowerCase();
		String prodGender = product.getGender() != null ? product.getGender().trim().toLowerCase() : "unisex";
		
		boolean genderMatch = userGender.contains("kiş") && prodGender.contains("kiş")
				|| userGender.contains("qad") && prodGender.contains("qad")
				|| prodGender.contains("unisex") || prodGender.contains("uni");

		if (!genderMatch) {
			return new SizeRecommendationResponse("Uyğun deyil", 0,
					"Bu geyimin cinsi sizin profil seçimlərinizə uyğun gəlmir.");
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
					"Bədən quruluşunuza uyğun geyim ölçüsü tapılmadı.");
		}

		int finalPercentage = (int) Math.round(highestScore);
		String message = generateFeedback(user, bestSize, product.getCategory());

		return new SizeRecommendationResponse(bestSize.getSizeName(), finalPercentage, message);
	}

	private double evaluateSizeMatch(User user, ProductSize ps) {
		String userSize = user.getClothingSize().trim().toUpperCase();
		String prodSize = ps.getSizeName().trim().toUpperCase();
		String userBody = user.getBodyType().trim().toLowerCase();
		String fit = ps.getClothingFit() != null ? ps.getClothingFit().trim().toLowerCase() : "orta";

		// Perfect size name match
		if (userSize.equals(prodSize)) {
			if (userBody.contains("arıq") || userBody.contains("slim")) {
				if (fit.contains("kiçik") || fit.contains("orta kiçik")) return 100.0;
				if (fit.contains("orta")) return 95.0;
				if (fit.contains("orta geniş")) return 85.0;
				return 75.0; // geniş
			} else if (userBody.contains("normal") || userBody.contains("regular")) {
				if (fit.contains("orta")) return 100.0;
				if (fit.contains("orta kiçik") || fit.contains("orta geniş")) return 92.0;
				return 80.0; // kiçik or geniş
			} else if (userBody.contains("idman") || userBody.contains("athletic")) {
				if (fit.contains("orta") || fit.contains("orta geniş")) return 100.0;
				if (fit.contains("geniş")) return 90.0;
				return 75.0; // kiçik or orta kiçik
			} else { // kilolu / heavy
				if (fit.contains("geniş")) return 100.0;
				if (fit.contains("orta geniş")) return 85.0;
				if (fit.contains("orta")) return 70.0;
				return 40.0; // kiçik/orta kiçik
			}
		}

		// Adjacent size check (e.g. User wants M, product offers S or L)
		int userIdx = getSizeIndex(userSize);
		int prodIdx = getSizeIndex(prodSize);
		
		if (Math.abs(userIdx - prodIdx) == 1) {
			// User is M, product is S
			if (userIdx > prodIdx) {
				if (fit.contains("geniş") || fit.contains("orta geniş")) return 80.0;
				return 50.0;
			}
			// User is M, product is L
			if (userIdx < prodIdx) {
				if (fit.contains("kiçik") || fit.contains("orta kiçik")) return 80.0;
				return 55.0;
			}
		}

		// Far sizes (e.g. S vs XL)
		return 20.0;
	}

	private int getSizeIndex(String size) {
		switch (size) {
			case "XS": return 0;
			case "S": return 1;
			case "M": return 2;
			case "L": return 3;
			case "XL": return 4;
			case "XXL": return 5;
			case "3XL": return 6;
			default: return 2;
		}
	}

	private String generateFeedback(User user, ProductSize ps, String category) {
		String userSize = user.getClothingSize().trim().toUpperCase();
		String prodSize = ps.getSizeName().trim().toUpperCase();
		String fit = ps.getClothingFit() != null ? ps.getClothingFit().trim().toLowerCase() : "orta";

		StringBuilder sb = new StringBuilder();
		sb.append(ps.getSizeName()).append(" ölçüsü bu ").append(category.toLowerCase()).append(" üçün sizə məsləhət görülür. ");

		if (userSize.equals(prodSize)) {
			if (fit.contains("kiçik") || fit.contains("orta kiçik")) {
				sb.append("Geyim kəsimi dar (Slim-fit) olduğu üçün bədəninizi zərif şəkildə saracaq.");
			} else if (fit.contains("geniş") || fit.contains("orta geniş")) {
				sb.append("Geyim kəsimi geniş (Oversized) olduğu üçün bədəninizdə rahat və boş qalacaq.");
			} else {
				sb.append("Standart (Regular-fit) kəsimi ilə bədəninizə ideal və tam rahat oturacaq.");
			}
		} else {
			int userIdx = getSizeIndex(userSize);
			int prodIdx = getSizeIndex(prodSize);
			
			if (userIdx > prodIdx) {
				if (fit.contains("geniş") || fit.contains("orta geniş")) {
					sb.append("Normalda ").append(userSize).append(" geyinirsiniz, lakin bu məhsulun kəsimi geniş (Oversized/Relaxed) olduğu üçün sizə bir ölçü kiçik olan ").append(prodSize).append(" ölçüsü tam uyğun gələcək.");
				} else {
					sb.append("Normalda ").append(userSize).append(" geyinirsiniz, lakin bu ölçünün kəsim detalları səbəbindən sizə ").append(prodSize).append(" ölçüsü daha yaxşı uyğun gəlir.");
				}
			} else {
				if (fit.contains("kiçik") || fit.contains("orta kiçik")) {
					sb.append("Normalda ").append(userSize).append(" geyinirsiniz, lakin bu məhsulun kəsimi dar (Slim-fit) olduğu üçün sizə bir ölçü böyük olan ").append(prodSize).append(" ölçüsü daha rahat olacaq.");
				} else {
					sb.append("Normalda ").append(userSize).append(" geyinirsiniz, lakin geyimin rahat kəsimini nəzərə alaraq, sizə ").append(prodSize).append(" ölçüsü məsləhət görülür.");
				}
			}
		}

		return sb.toString();
	}
}