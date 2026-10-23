package org.hhnotifier;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Does one poll and one refresh pass, then lets the app exit. Used by the GitHub Actions
 * workflow, which starts the app on a cron schedule instead of keeping it running.
 */
@Component
@ConditionalOnProperty(name = "hh.run-once", havingValue = "true")
class RunOnceRunner implements ApplicationRunner {

	private final VacancyPoller poller;

	private final StatsRefresher refresher;

	RunOnceRunner(VacancyPoller poller, StatsRefresher refresher) {
		this.poller = poller;
		this.refresher = refresher;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// let failures escape so the process exits non-zero and the GitHub run shows red
		poller.pollOnce();
		refresher.refreshOnce();
	}

}
