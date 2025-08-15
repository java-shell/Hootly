package com.javashell.danschirripa.desktopassistant.tools;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.javashell.danschirripa.desktopassistant.ConfigManager;
import com.javashell.jai.chat.ChatTool;
import com.javashell.jai.chat.ChatToolMessage;
import com.javashell.jai.requests.ChatToolsEventHandler;
import com.javashell.jai.requests.ChatToolsRequest;

public class SearchEngineTool extends ChatTool {
	final static Property queryProperty = new Property();
	private final static String baseURI = "https://api.search.brave.com/res/v1/web/search";

	static {
		queryProperty.propName = "Query";
		queryProperty.description = "Word or phrase to search on for. Will return a JSON formatted list of results";
		queryProperty.propType = "string";
	}

	public SearchEngineTool() {
		super("WebSearch", "Search the web for results on a given prompt", queryProperty);
		registerHandler(new ChatToolsEventHandler() {
			public void triggerEvent(ResultantProperty resultant, ChatToolsRequest toolsRequest) {
				try {

					String popURI = baseURI + "?q=" + URLEncoder.encode(resultant.result, "UTF-8");
					final URI endpoint = URI.create(popURI);

					HttpClient client = HttpClient.newHttpClient();

					HttpRequest request = HttpRequest.newBuilder(endpoint).GET().header("X-Subscription-Token", ConfigManager.getConfigValue("braveKey"))
							.header("Accept", "application/json").build();

					HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
					JSONParser parser = new JSONParser();
					JSONObject responseJSON = (JSONObject) parser.parse(response.body());
					JSONObject queriesJSON = (JSONObject) responseJSON.get("web");
					JSONArray resultsJSON = (JSONArray) queriesJSON.get("results");
					HashMap<String, HashMap<String, String>> responseMap = new HashMap<String, HashMap<String, String>>();
					for (var next : resultsJSON) {
						JSONObject nextObj = (JSONObject) next;
						String title = nextObj.get("title").toString();
						String url = nextObj.get("url").toString();
						String desc = nextObj.get("description").toString();
						var map = new HashMap<String, String>();
						map.put("url", url);
						map.put("description", desc);
						responseMap.put(title, map);
					}
					String responseString = JSONObject.toJSONString(responseMap);
					System.out.println(responseString);
					toolsRequest.updateMessageHistory(new ChatToolMessage("tool", responseString, "WebSearch"));
				} catch (Exception e) {
					e.printStackTrace();
					toolsRequest.updateMessageHistory(
							new ChatToolMessage("tool", "Failed to get search results " + e.getMessage(), "WebSearch"));
				}
			}
		});
	}
}
