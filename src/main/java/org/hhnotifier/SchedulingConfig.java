package org.hhnotifier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Timers run only in the long-lived mode; with hh.run-once=true, {@link RunOnceRunner} drives a single pass. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "hh.run-once", havingValue = "false", matchIfMissing = true)
class SchedulingConfig {

}
