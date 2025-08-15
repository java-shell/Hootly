package com.javashell.danschirripa.desktopassistant.tts;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.json.simple.JSONObject;

import com.javashell.danschirripa.desktopassistant.ConfigManager;

import javazoom.jl.player.Player;

public class TTSService {

	private static URI elevenEndpoint = URI.create("https://api.elevenlabs.io/v1/text-to-speech/v");

	public static void ttsElevenLabsPlayAudio(String input) throws Exception {
		input = JSONObject.escape(input);
		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder(elevenEndpoint)
				.header("xi-api-key", ConfigManager.getConfigValue("ttsKey")).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers
						.ofString("{ \"text\": \"" + input + "\", \"model_id\": \"eleven_flash_v2_5\" }"))
				.build();

		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		Player playMP3 = new Player(new ByteArrayInputStream(response.body()));
		playMP3.play();
	}

	public static void ttsPiperPlayAudio(String input) throws Exception {
		input = JSONObject.escape(input);
		HttpClient client = HttpClient.newHttpClient();

		URI piperEndpoint = URI.create(ConfigManager.getConfigValue("ttsEndpoint"));

		HttpRequest request = HttpRequest.newBuilder(piperEndpoint).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{ \"text\": \"" + input + "\"}")).build();

		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		AudioInputStream ain = AudioSystem.getAudioInputStream(new ByteArrayInputStream(response.body()));
		Clip clip = AudioSystem.getClip();
		clip.open(ain);
		clip.start();
	}

	public static void ttsKittenPlayAudio(String input) throws Exception {
		input = JSONObject.escape(input);
		String body = "{" + "  \"text\": \"" + input + "\"," + "  \"voice\": \"expr-voice-5-m\"," + "  \"speed\": 1.0,"
				+ "  \"output_format\": \"mp3\"," + "  \"split_text\": true," + "  \"chunk_size\": 300" + "}";

		HttpClient client = HttpClient.newHttpClient();

		URI kittenEndpoint = URI.create(ConfigManager.getConfigValue("ttsEndpoint"));

		HttpRequest request = HttpRequest.newBuilder(kittenEndpoint).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();

		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		Player playMP3 = new Player(new ByteArrayInputStream(response.body()));
		playMP3.play();
	}

	public static void ttsDiaPlayAudio(String input) throws Exception {
		input = JSONObject.escape(input);
		HttpClient client = HttpClient.newHttpClient();

		URI diaEndpoint = URI.create(ConfigManager.getConfigValue("ttsEndpoint"));

		HttpRequest request = HttpRequest.newBuilder(diaEndpoint).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{ \"text\": \"" + input + "\"}")).build();

		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		AudioInputStream ain = AudioSystem.getAudioInputStream(new ByteArrayInputStream(response.body()));
		Clip clip = AudioSystem.getClip();
		clip.open(ain);
		clip.start();
	}

	public static void kokoPlayAudio(String input) throws Exception {
		input = JSONObject.escape(input);
		HttpClient client = HttpClient.newHttpClient();

		String voice = "af_heart";
		String body = "{\n" + "  \"model\": \"kokoro\",\n" + "  \"input\": \"" + input + "\",\n" + "  \"voice\": \""
				+ voice + "\",\n" + "  \"response_format\": \"mp3\",\n" + "  \"download_format\": \"mp3\",\n"
				+ "  \"speed\": 1,\n" + "  \"stream\": true,\n" + "  \"return_download_link\": false,\n"
				+ "  \"lang_code\": \"a\",\n" + "  \"volume_multiplier\": 1,\n" + "  \"normalization_options\": {\n"
				+ "    \"normalize\": true,\n" + "    \"unit_normalization\": false,\n"
				+ "    \"url_normalization\": true,\n" + "    \"email_normalization\": true,\n"
				+ "    \"optional_pluralization_normalization\": true,\n" + "    \"phone_normalization\": true,\n"
				+ "    \"replace_remaining_symbols\": true\n" + "  }\n" + "}";

		URI kokoEndpoint = URI.create(ConfigManager.getConfigValue("ttsEndpoint"));

		HttpRequest request = HttpRequest.newBuilder(kokoEndpoint).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();

		HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
		Player playMP3 = new Player(response.body());
		playMP3.play();
	}

}
