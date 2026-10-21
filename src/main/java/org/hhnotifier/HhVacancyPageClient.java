package org.hhnotifier;

import java.net.http.HttpClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

/**
 * Reads the "people viewing now" and "responses" counters from a vacancy's public page.
 * The RSS feed doesn't carry them, but the page embeds them in its initial state JSON.
 */
@Component
public class HhVacancyPageClient {

	private static final Logger log = LoggerFactory.getLogger(HhVacancyPageClient.class);

	private static final Pattern RESPONSES = Pattern.compile("\"totalResponsesCount\":(\\d+)");

	private final RestClient restClient = RestClient.builder()
		// hh.uz redirects to the regional domain (e.g. tashkent.hh.uz)
		.requestFactory(new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
		.defaultHeader("User-Agent", "Mozilla/5.0 (compatible; hh-notifier/1.0)")
		.build();

	public VacancyStats fetchStats(Vacancy vacancy) {
		try {
			String page = restClient.get().uri(vacancy.url()).retrieve().body(String.class);
			if (page == null) {
				return VacancyStats.UNKNOWN;
			}
			// the state JSON is embedded HTML-escaped (&#34; instead of ")
			String state = HtmlUtils.htmlUnescape(page);
			// excludingCurrent: our own page load shouldn't count as a viewer
			Matcher viewing = Pattern.compile("\"vacancyOnlineUsersCounters\":\\{\"" + vacancy.id()
					+ "\":\\{\"includingCurrent\":\\d+,\"excludingCurrent\":(\\d+)").matcher(state);
			Matcher responses = RESPONSES.matcher(state);
			return new VacancyStats(
					viewing.find() ? Integer.valueOf(viewing.group(1)) : null,
					responses.find() ? Integer.valueOf(responses.group(1)) : null);
		}
		catch (Exception e) {
			log.warn("Could not read stats for vacancy {}: {}", vacancy.id(), e.getMessage());
			return VacancyStats.UNKNOWN;
		}
	}

}
