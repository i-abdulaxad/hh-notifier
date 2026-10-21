package org.hhnotifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

/** Recently posted messages, persisted as JSON so refreshes survive restarts. */
@Component
public class PostedMessageStore {

	private final Path file;

	private final JsonMapper jsonMapper;

	private final List<PostedMessage> messages = new ArrayList<>();

	public PostedMessageStore(HhProperties properties, JsonMapper jsonMapper) throws IOException {
		this.file = properties.postedFile();
		this.jsonMapper = jsonMapper;
		if (Files.exists(file)) {
			messages.addAll(jsonMapper.readValue(file.toFile(), new TypeReference<List<PostedMessage>>() {
			}));
		}
	}

	public synchronized void add(PostedMessage message) throws IOException {
		messages.add(message);
		save();
	}

	public synchronized void replace(PostedMessage old, PostedMessage updated) throws IOException {
		int i = messages.indexOf(old);
		if (i >= 0) {
			messages.set(i, updated);
			save();
		}
	}

	/** Drops messages posted before {@code cutoff} and returns the remaining ones. */
	public synchronized List<PostedMessage> pruneAndList(OffsetDateTime cutoff) throws IOException {
		if (messages.removeIf(m -> m.postedAt().isBefore(cutoff))) {
			save();
		}
		return List.copyOf(messages);
	}

	private void save() throws IOException {
		if (file.getParent() != null) {
			Files.createDirectories(file.getParent());
		}
		jsonMapper.writeValue(file.toFile(), messages);
	}

}
