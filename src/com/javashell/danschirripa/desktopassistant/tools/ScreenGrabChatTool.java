package com.javashell.danschirripa.desktopassistant.tools;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.javashell.jai.chat.ChatTool;
import com.javashell.jai.chat.ChatToolMessage;
import com.javashell.jai.requests.ChatToolsEventHandler;
import com.javashell.jai.requests.ChatToolsRequest;

public class ScreenGrabChatTool extends ChatTool {
	final static Property takeScreenShotProperty = new Property();

	static {
		takeScreenShotProperty.propName = "GrabScreen";
		takeScreenShotProperty.description = "Take a screenshot of the user's screen and return it as a Base64 encoded image";
		takeScreenShotProperty.propType = "boolean";
	}

	public ScreenGrabChatTool() {
		super("ScreenGrab", "Take a screenshot of the user's screen", takeScreenShotProperty);
		registerHandler(new ChatToolsEventHandler() {
			public void triggerEvent(ResultantProperty resultant, ChatToolsRequest toolsRequest) {
				try {
					BufferedImage screenshot = new Robot().createScreenCapture(
							GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());

					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					byte[] jpegBytes = bout.toByteArray();

					String encoded = Base64.getEncoder().encodeToString(jpegBytes);

					toolsRequest.updateMessageHistory(new ChatToolMessage("tool", encoded, "GrabScreen"));
					ImageIO.write(screenshot, "JPEG", bout);
				} catch (Exception e) {
					e.printStackTrace();
					toolsRequest.updateMessageHistory(
							new ChatToolMessage("tool", "Failed to grab the screen", "GrabScreen"));

				}

			}
		});
	}

}
