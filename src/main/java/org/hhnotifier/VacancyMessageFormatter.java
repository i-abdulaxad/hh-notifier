package org.hhnotifier;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.web.util.HtmlUtils;

/** Builds the Telegram (HTML parse mode) text for a vacancy. */
final class VacancyMessageFormatter {

	private static final Map<String, String> EXPERIENCE_LABELS = Map.of(
			"noExperience", "no experience",
			"between1And3", "1–3 years",
			"between3And6", "3–6 years",
			"moreThan6", "6+ years");

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");

	private VacancyMessageFormatter() {
	}

	/** @param statsAt when {@code stats} was read, shown so readers know how fresh the counters are */
	static String format(Vacancy v, VacancyStats stats, OffsetDateTime statsAt) {
		var sb = new StringBuilder();
		sb.append("🆕 <b>").append(esc(v.title())).append("</b>\n");
		if (!v.company().isEmpty()) {
			sb.append("🏢 ").append(esc(v.company())).append('\n');
		}
		if (!v.region().isEmpty()) {
			sb.append("📍 ").append(esc(v.region())).append('\n');
		}
		if (!v.salary().isEmpty()) {
			sb.append("💰 ").append(esc(v.salary())).append('\n');
		}
		sb.append("🧑‍💻 #").append(v.keyword()).append(" · ")
			.append(EXPERIENCE_LABELS.getOrDefault(v.experience(), v.experience())).append('\n');
		sb.append("🕒 ").append(DATE_TIME.format(v.publishedAt().atZoneSameInstant(TASHKENT))).append('\n');
		if (stats.viewingNow() != null || stats.responses() != null) {
			sb.append("👀 ").append(orUnknown(stats.viewingNow())).append(" viewing now · 📨 ")
				.append(orUnknown(stats.responses())).append(" applied")
				.append(" <i>(as of ").append(TIME.format(statsAt.atZoneSameInstant(TASHKENT))).append(")</i>\n");
		}
		sb.append("🔗 ").append(esc(v.url()));
		return sb.toString();
	}

	private static String orUnknown(Integer n) {
		return n != null ? n.toString() : "?";
	}

	private static String esc(String s) {
		return HtmlUtils.htmlEscape(s, "UTF-8");
	}

}
