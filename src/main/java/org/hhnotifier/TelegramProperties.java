package org.hhnotifier;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("telegram")
public record TelegramProperties(String botToken, String chatId) {

	public boolean hasChatId() {
		return chatId != null && !chatId.isBlank();
	}

}
