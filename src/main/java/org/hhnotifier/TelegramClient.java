package org.hhnotifier;

import java.util.HashMap;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TelegramClient {

	private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

	private final TelegramProperties properties;

	private final RestClient restClient;

	public TelegramClient(TelegramProperties properties) {
		this.properties = properties;
		this.restClient = RestClient.builder()
			.baseUrl("https://api.telegram.org/bot" + properties.botToken())
			.build();
	}

	/** Sends an HTML-formatted message to the configured chat and returns its message id. */
	public long send(String html, Integer topicId) throws InterruptedException {
		Map<String, Object> body = textBody(html);
		if (topicId != null) {
			body.put("message_thread_id", topicId);
		}
		return call("/sendMessage", body).path("result").path("message_id").asLong();
	}

	public void edit(long messageId, String html) throws InterruptedException {
		Map<String, Object> body = textBody(html);
		body.put("message_id", messageId);
		try {
			call("/editMessageText", body);
		}
		catch (RestClientResponseException e) {
			// the message was deleted by someone, or is identical; nothing to do either way
			log.warn("Could not edit message {}: {}", messageId, e.getResponseBodyAsString());
		}
	}

	private Map<String, Object> textBody(String html) {
		return new HashMap<>(Map.of(
				"chat_id", properties.chatId(),
				"text", html,
				"parse_mode", "HTML",
				"link_preview_options", Map.of("is_disabled", true)));
	}

	/** Calls a Bot API method, retrying once if Telegram rate-limits us (429). */
	private JsonNode call(String method, Map<String, Object> body) throws InterruptedException {
		for (int attempt = 0; ; attempt++) {
			try {
				return restClient.post().uri(method).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
			}
			catch (RestClientResponseException e) {
				if (e.getStatusCode().value() == 429 && attempt == 0) {
					Thread.sleep(30_000);
					continue;
				}
				throw e;
			}
		}
	}

	/** Logs the chats the bot has recently seen, to help find the group's chat id. */
	public void logKnownChats() {
		JsonNode updates = restClient.get().uri("/getUpdates").retrieve().body(JsonNode.class);
		if (updates == null || updates.path("result").isEmpty()) {
			log.warn("telegram.chat-id is not set and the bot has no recent updates. "
					+ "Send a message like /start@<bot_username> in the group, then wait for the next poll.");
			return;
		}
		for (JsonNode update : updates.path("result")) {
			for (String key : new String[] { "message", "my_chat_member", "channel_post" }) {
				JsonNode chat = update.path(key).path("chat");
				if (!chat.isMissingNode()) {
					log.warn("Found chat: id={} type={} title={}", chat.path("id").asString(),
							chat.path("type").asString(), chat.path("title").asString());
				}
			}
		}
		log.warn("Put the group's id into secrets.properties as telegram.chat-id and restart.");
	}

}
