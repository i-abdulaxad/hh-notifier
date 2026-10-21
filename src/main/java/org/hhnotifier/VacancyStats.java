package org.hhnotifier;

/** Live counters shown on a vacancy page; either may be null if hh didn't include it. */
public record VacancyStats(Integer viewingNow, Integer responses) {

	public static final VacancyStats UNKNOWN = new VacancyStats(null, null);

}
