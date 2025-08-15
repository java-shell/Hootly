package com.javashell.danschirripa.desktopassistant.tools;

import java.net.URI;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import com.javashell.jai.chat.ChatTool;
import com.javashell.jai.chat.ChatToolMessage;
import com.javashell.jai.requests.ChatToolsEventHandler;
import com.javashell.jai.requests.ChatToolsRequest;

public class WebPageTool extends ChatTool {
	final static Property queryProperty = new Property();

	static {
		queryProperty.propName = "Page";
		queryProperty.description = "URL of the web page to get";
		queryProperty.propType = "string";
	}

	public WebPageTool() {
		super("WebPage", "Read the contents of a web page", queryProperty);
		registerHandler(new ChatToolsEventHandler() {
			public void triggerEvent(ResultantProperty resultant, ChatToolsRequest toolsRequest) {
				try {
					URI webPage = URI.create(resultant.result);
					Connection session = Jsoup.newSession().userAgent("DJS");
					var doc = session.newRequest(webPage.toURL()).get();
					String bodyText = doc.body().text();
					toolsRequest.updateMessageHistory(new ChatToolMessage("tool", bodyText, "WebPage"));
				} catch (Exception e) {
					toolsRequest.updateMessageHistory(
							new ChatToolMessage("tool", "Failed to get page, " + e.getMessage(), "WebPage"));
				}
			}
		});
	}
}