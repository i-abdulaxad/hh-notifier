package org.hhnotifier;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VacancyPoller {

	private static final Logger log = LoggerFactory.getLogger(VacancyPoller.class);

	private final HhProperties hh;

	private final TelegramProperties telegram;

	private final HhRssClient hhClient;

	private final HhVacancyPageClient pageClient;

	private final TelegramClient telegramClient;

	private final SeenVacancyStore seen;

	private final PostedMessageStore posted;

	private boolean firstPoll;

	public VacancyPoller(HhProperties hh, TelegramProperties telegram, HhRssClient hhClient,
			HhVacancyPageClient pageClient, TelegramClient telegramClient, SeenVacancyStore seen, PostedMessageStore posted) {
		this.hh = hh;
		this.telegram = telegram;
		this.hhClient = hhClient;
		this.pageClient = pageClient;
		this.telegramClient = telegramClient;
		this.seen = seen;
		this.posted = posted;
		this.firstPoll = seen.isFirstRun();
	}

	@Scheduled(initialDelay = 0, fixedDelayString = "${hh.poll-interval}")
	public void poll() {
		try {
			pollOnce();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error("Poll failed", e);
		}
	}

	/** One pass; exceptions propagate so run-once mode can exit with an error. */
	public void pollOnce() throws Exception {
		if (!telegram.hasChatId()) {
			telegramClient.logKnownChats();
			return;
		}
		List<Vacancy> fresh = fetchUnseen();
		// e.g. after widening the search, don't flood the group with month-old vacancies
		OffsetDateTime cutoff = OffsetDateTime.now().minus(hh.maxAge());
		List<Vacancy> stale = fresh.stream().filter(v -> v.publishedAt().isBefore(cutoff)).toList();
		if (!stale.isEmpty()) {
			seen.addAll(stale.stream().map(VacancyPoller::seenKey).toList());
			log.info("Skipped {} vacancies published more than {} ago", stale.size(), hh.maxAge());
			fresh = fresh.stream().filter(v -> !v.publishedAt().isBefore(cutoff)).toList();
		}
		if (firstPoll && !hh.sendExistingOnFirstRun()) {
			seen.addAll(fresh.stream().map(VacancyPoller::seenKey).toList());
			log.info("First run: remembered {} existing vacancies without posting them", fresh.size());
		}
		else {
			for (Vacancy vacancy : fresh) {
				OffsetDateTime now = OffsetDateTime.now();
				String text = VacancyMessageFormatter.format(vacancy, pageClient.fetchStats(vacancy), now);
				long messageId = telegramClient.send(text, vacancy.topicId());
				seen.addAll(List.of(seenKey(vacancy)));
				posted.add(new PostedMessage(messageId, vacancy, now, text, now));
				// Telegram allows ~20 messages per minute in a group
				Thread.sleep(300);
			}
			log.info("Posted {} new vacancies", fresh.size());
		}
		firstPoll = false;
	}

	/** Queries every keyword/experience combination and returns unseen (keyword, vacancy) pairs, oldest first. */
	private List<Vacancy> fetchUnseen() throws Exception {
		Map<String, Vacancy> byKey = new LinkedHashMap<>();
		for (HhProperties.Keyword keyword : hh.keywords()) {
			for (String experience : hh.experiences()) {
				for (Vacancy v : hhClient.search(hh.area(), keyword, experience, hh.professionalRoles())) {
					if (!seen.contains(seenKey(v))) {
						byKey.putIfAbsent(seenKey(v), v);
					}
				}
			}
		}
		return byKey.values().stream().sorted(Comparator.comparing(Vacancy::publishedAt)).toList();
	}

	/** Dedupe per keyword, so a vacancy matching both Java and Golang reaches both topics. */
	private static String seenKey(Vacancy v) {
		return v.keyword() + ":" + v.id();
	}

}
