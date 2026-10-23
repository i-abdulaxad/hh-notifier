package org.hhnotifier;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hh")
public record HhProperties(
		String area,
		List<Keyword> keywords,
		List<String> experiences,
		List<String> professionalRoles,
		Duration maxAge,
		Duration pollInterval,
		Path seenFile,
		Path postedFile,
		boolean sendExistingOnFirstRun,
		Duration refreshInterval,
		Duration refreshWindow,
		boolean runOnce) {

	/** {@code topicId} is the forum topic (message_thread_id) to post into; null posts to the general chat. */
	public record Keyword(String label, String query, Integer topicId) {
	}

}
