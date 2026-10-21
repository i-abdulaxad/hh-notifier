package org.hhnotifier;

import java.time.OffsetDateTime;

/** A vacancy message already posted to Telegram, kept so its counters can be refreshed. */
public record PostedMessage(long messageId, Vacancy vacancy, OffsetDateTime postedAt, String text,
		OffsetDateTime refreshedAt) {

	OffsetDateTime lastRefresh() {
		return refreshedAt != null ? refreshedAt : postedAt;
	}

	PostedMessage refreshed(String newText, OffsetDateTime at) {
		return new PostedMessage(messageId, vacancy, postedAt, newText, at);
	}

}
