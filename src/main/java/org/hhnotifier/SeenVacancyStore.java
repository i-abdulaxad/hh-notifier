package org.hhnotifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Component;

/** Remembers vacancy ids that were already processed, persisted to a plain text file. */
@Component
public class SeenVacancyStore {

	private static final int MAX_ENTRIES = 10_000;

	private final Path file;

	private final LinkedHashSet<String> ids = new LinkedHashSet<>();

	private final boolean existedOnStartup;

	public SeenVacancyStore(HhProperties properties) throws IOException {
		this.file = properties.seenFile();
		this.existedOnStartup = Files.exists(file);
		if (existedOnStartup) {
			ids.addAll(Files.readAllLines(file));
		}
	}

	public boolean isFirstRun() {
		return !existedOnStartup;
	}

	public boolean contains(String id) {
		return ids.contains(id);
	}

	public void addAll(List<String> newIds) throws IOException {
		ids.addAll(newIds);
		while (ids.size() > MAX_ENTRIES) {
			ids.remove(ids.iterator().next());
		}
		if (file.getParent() != null) {
			Files.createDirectories(file.getParent());
		}
		Files.write(file, new ArrayList<>(ids));
	}

}
