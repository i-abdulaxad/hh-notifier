package org.hhnotifier;

import java.time.OffsetDateTime;

public record Vacancy(
		String id,
		String title,
		String url,
		String company,
		String region,
		String salary,
		OffsetDateTime publishedAt,
		String keyword,
		Integer topicId,
		String experience) {
}
