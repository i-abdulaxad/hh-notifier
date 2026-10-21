package org.hhnotifier;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads hh.uz's public vacancy RSS feed. The official api.hh.ru search endpoint now
 * rejects anonymous requests with 403, while the RSS feed needs no login.
 */
@Component
public class HhRssClient {

	private static final String RSS_URL = "https://hh.uz/search/vacancy/rss";

	private static final Pattern ID_PATTERN = Pattern.compile("/vacancy/(\\d+)");

	private final RestClient restClient = RestClient.builder()
		.defaultHeader("User-Agent", "Mozilla/5.0 (compatible; hh-notifier/1.0)")
		.build();

	public List<Vacancy> search(String area, HhProperties.Keyword keyword, String experience) throws Exception {
		var uri = UriComponentsBuilder.fromUriString(RSS_URL)
			.queryParam("text", keyword.query())
			.queryParam("area", area)
			.queryParam("experience", experience)
			.queryParam("search_field", "name")
			.queryParam("order_by", "publication_time")
			.queryParam("items_on_page", 100)
			.encode()
			.build()
			.toUri();
		byte[] body = restClient.get().uri(uri).retrieve().body(byte[].class);
		if (body == null) {
			return List.of();
		}
		return parse(body, keyword, experience);
	}

	private List<Vacancy> parse(byte[] xml, HhProperties.Keyword keyword, String experience) throws Exception {
		var factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
		NodeList items = doc.getElementsByTagName("item");
		List<Vacancy> result = new ArrayList<>();
		for (int i = 0; i < items.getLength(); i++) {
			Element item = (Element) items.item(i);
			String link = text(item, "link");
			Matcher m = ID_PATTERN.matcher(link);
			if (!m.find()) {
				continue;
			}
			String description = text(item, "description");
			result.add(new Vacancy(
					m.group(1),
					text(item, "title"),
					link,
					field(description, "Вакансия компании"),
					field(description, "Регион"),
					field(description, "Предполагаемый уровень месячного дохода"),
					parseDate(text(item, "pubDate")),
					keyword.label(),
					keyword.topicId(),
					experience));
		}
		return result;
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
	}

	/** Extracts "Label: value" from the description's {@code <p>} lines. */
	private static String field(String html, String label) {
		Matcher m = Pattern.compile(Pattern.quote(label) + ":\\s*(.*?)</p>").matcher(html);
		return m.find() ? unescapeHtml(m.group(1).replaceAll("<[^>]+>", "").trim()) : "";
	}

	private static String unescapeHtml(String s) {
		return s.replace("&quot;", "\"")
			.replace("&#39;", "'")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&nbsp;", " ")
			.replace("&amp;", "&");
	}

	private static OffsetDateTime parseDate(String s) {
		try {
			return OffsetDateTime.parse(s);
		}
		catch (Exception e) {
			return OffsetDateTime.now();
		}
	}

}
