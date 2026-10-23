package org.hhnotifier;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically edits recent vacancy messages so their viewer/applicant counts stay current. */
@Component
public class StatsRefresher {

	private static final Logger log = LoggerFactory.getLogger(StatsRefresher.class);

	private final HhProperties hh;

	private final TelegramProperties telegram;

	private final PostedMessageStore store;

	private final HhVacancyPageClient pageClient;

	private final TelegramClient telegramClient;

	public StatsRefresher(HhProperties hh, TelegramProperties telegram, PostedMessageStore store,
			HhVacancyPageClient pageClient, TelegramClient telegramClient) {
		this.hh = hh;
		this.telegram = telegram;
		this.store = store;
		this.pageClient = pageClient;
		this.telegramClient = telegramClient;
	}

	@Scheduled(initialDelayString = "${hh.refresh-interval}", fixedDelayString = "${hh.refresh-interval}")
	public void refresh() {
		try {
			refreshOnce();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			log.error("Refresh failed", e);
		}
	}

	/** One pass; exceptions propagate so run-once mode can exit with an error. */
	public void refreshOnce() throws Exception {
		if (!telegram.hasChatId()) {
			return;
		}
		int edited = 0;
		OffsetDateTime now = OffsetDateTime.now();
		for (PostedMessage message : store.pruneAndList(now.minus(hh.refreshWindow()))) {
			// in run-once mode this is called every few minutes, so respect the interval per message
			if (message.lastRefresh().plus(hh.refreshInterval()).isAfter(now.plusMinutes(1))) {
				continue;
			}
			VacancyStats stats = pageClient.fetchStats(message.vacancy());
			if (stats.equals(VacancyStats.UNKNOWN)) {
				// page unavailable (vacancy closed, or hh hiccup): keep the last known numbers
				continue;
			}
			String text = VacancyMessageFormatter.format(message.vacancy(), stats, now);
			telegramClient.edit(message.messageId(), text);
			store.replace(message, message.refreshed(text, now));
			edited++;
			Thread.sleep(300);
		}
		log.info("Refreshed counters on {} messages", edited);
	}

}
