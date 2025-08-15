package com.javashell.danschirripa.desktopassistant.ui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.javashell.danschirripa.desktopassistant.ConfigManager;
import com.javashell.danschirripa.desktopassistant.tools.SearchEngineTool;
import com.javashell.danschirripa.desktopassistant.tools.WebPageTool;
import com.javashell.danschirripa.desktopassistant.tts.TTSService;
import com.javashell.jai.chat.ChatImageMessage;
import com.javashell.jai.chat.ChatMessage;
import com.javashell.jai.chat.ChatTool;
import com.javashell.jai.chat.tools.AdditionTool;
import com.javashell.jai.chat.tools.fileio.CheckFile;
import com.javashell.jai.chat.tools.fileio.ListFiles;
import com.javashell.jai.chat.tools.fileio.ReadFile;
import com.javashell.jai.chat.tools.fileio.WriteFile;
import com.javashell.jai.chat.tools.matching.StringContains;
import com.javashell.jai.eventmanagement.LLMEvents;
import com.javashell.jai.eventmanagement.LLMRequest;
import com.javashell.jai.eventmanagement.LLMRequestManager;
import com.javashell.jai.requests.ChatRequest;
import com.javashell.jai.requests.ChatToolsRequest;

public class ChatWindow extends JFrame {
	static {
		FlatDarculaLaf.setup();
	}

	private static final long serialVersionUID = 8398175509737608636L;
	private ChatRequest chat;
	private LLMRequest request;
	private JPanel chatHistoryPanel;
	private JTextArea inputArea;
	private JProgressBar feedbackBar;

	private String modelName = "gemma3:latest";
	private URI endpoint = URI.create("http://192.168.0.224:11434/api/chat");

	private ChatTool[] tools = { new CheckFile(), new ListFiles(), new ReadFile(), new WriteFile(),
			new StringContains(), new AdditionTool(), new SearchEngineTool(), new WebPageTool() };

	private long lastOpen = System.currentTimeMillis();

	public ChatWindow() throws AWTException {
		if (ConfigManager.isFirstLaunch()) {
			int result = JOptionPane.showConfirmDialog(null,
					"First time launch! Configure Ollama & TTS server settings now?", "First Time Launch",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				ConfigManager.openSettingsEditor(this);
			}
		}

		modelName = ConfigManager.getConfigValue("modelName");
		endpoint = URI.create(ConfigManager.getConfigValue("ollamaURI"));

		if (ConfigManager.getConfigValue("enableTools").equals("true")) {
			chat = new ChatToolsRequest(modelName, endpoint, tools);
		} else
			chat = new ChatRequest(modelName, endpoint);

		request = new LLMRequest(chat, null);

		BufferedImage icon = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);

		try {
			icon = ImageIO.read(ChatWindow.class.getResourceAsStream("/icon_alt1.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Dimension newSize = SystemTray.getSystemTray().getTrayIconSize();

		TrayIcon trayIcon = new TrayIcon(
				icon.getScaledInstance(newSize.width, newSize.height, BufferedImage.SCALE_SMOOTH));
		SystemTray.getSystemTray().add(trayIcon);

		trayIcon.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				var now = System.currentTimeMillis();
				if (now - lastOpen > 100) {
					setVisible(!isVisible());
					lastOpen = now;
				}
			}
		});
		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				var now = System.currentTimeMillis();
				if (now - lastOpen > 100) {
					setVisible(!isVisible());
					lastOpen = now;
				}
			}
		});

		feedbackBar = new JProgressBar();
		feedbackBar.setIndeterminate(true);
		feedbackBar.setVisible(false);

		setIconImage(icon);
		setTitle("Hootly: " + modelName);

		setResizable(true);
		setSize(300, 500);
		setLayout(new BorderLayout());
		setAlwaysOnTop(true);
		var menuBar = createMenuBar();
		menuBar.add(feedbackBar);
		setJMenuBar(menuBar);
		var chatHistScroller = createChatHistoryPanel();
		add(chatHistScroller, BorderLayout.CENTER);

		inputArea = new JTextArea();
		JScrollPane inputScroller = new JScrollPane(inputArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		inputScroller.setAutoscrolls(true);
		JPanel inputAreaPanel = new JPanel();
		inputAreaPanel.setLayout(new BorderLayout());
		inputAreaPanel.add(inputScroller, BorderLayout.CENTER);

		JButton enterButton = new JButton("\u21B5");
		enterButton.setMnemonic(KeyEvent.VK_ENTER);
		enterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chat.updateMessageHistory("user", inputArea.getText());
				chatHistoryPanel.add(generateChatPanel("User", inputArea.getText()));
				revalidate();
				inputArea.setText("");
				LLMRequestManager.addRequestToQueue(request);
			}
		});

		JButton uploadImageButton = new JButton("\u21EA");
		uploadImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					BufferedImage screenshot = new Robot()
							.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

					String encoded = encodeImage(screenshot);

					chat.updateMessageHistory(new ChatImageMessage("user", encoded));

					chatHistoryPanel.add(generateChatImagePanel("User", screenshot));
					revalidate();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		inputAreaPanel.add(enterButton, BorderLayout.EAST);
		inputAreaPanel.add(uploadImageButton, BorderLayout.WEST);
		add(inputAreaPanel, BorderLayout.SOUTH);

		LLMRequestManager.subscribeLLMEventHandler((LLMEvents event) -> {
			if (event == LLMEvents.WAITING) {
				enterButton.setEnabled(true);
				feedbackBar.setVisible(false);
			} else {
				enterButton.setEnabled(false);
				feedbackBar.setVisible(true);
			}

			if (event == LLMEvents.REQUEST_COMPLETED) {
				var result = LLMRequestManager.getResult(request);
				chatHistoryPanel.add(generateChatPanel("Assistant", result.result));

				repaint();
				revalidate();

				if (ConfigManager.getConfigValue("autoPlay").equals("true"))
					playAudio(result.result);

				SwingUtilities.invokeLater(() -> {
					final var scrollBar = chatHistScroller.getVerticalScrollBar();
					scrollBar.setValue(scrollBar.getMaximum());
				});

			}
		});

		LLMRequestManager.startRequestProcessor();

		setVisible(true);
	}

	private String encodeImage(BufferedImage image) throws IOException {
		BufferedImage preparedImage = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_3BYTE_BGR);
		preparedImage.getGraphics().drawImage(image, 0, 0, null);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ImageIO.write(preparedImage, "jpg", bout);
		bout.flush();

		byte[] jpegBytes = bout.toByteArray();

		String encoded = Base64.getEncoder().encodeToString(jpegBytes);

		if (jpegBytes.length == 0)
			throw new IOException("Got empty size from image bytes...");
		return encoded;
	}

	private JPanel generateChatPanel(String role, String response) {
		JPanel responsePanel = new JPanel();
		responsePanel.setLayout(new BorderLayout());

		JTextPane responseArea = new JTextPane();
		StyledDocument doc = responseArea.getStyledDocument();
		int offset = 0;
		SimpleAttributeSet defaultSet = new SimpleAttributeSet();
		StyleConstants.setItalic(defaultSet, false);
		StyleConstants.setBold(defaultSet, false);

		SimpleAttributeSet thinkHighlightSet = new SimpleAttributeSet();
		StyleConstants.setItalic(thinkHighlightSet, true);
		StyleConstants.setBold(thinkHighlightSet, true);

		SimpleAttributeSet codeHighlightSet = new SimpleAttributeSet();
		StyleConstants.setBackground(codeHighlightSet, Color.BLACK);
		StyleConstants.setForeground(codeHighlightSet, Color.WHITE);
		StyleConstants.setFontFamily(codeHighlightSet, "Courier New");

		String[] highlightables = { "<think>", "```" };

		// Iterate over the response string and check for starting tokens
		while (offset < response.length()) {
			try {
				String leftover = response.substring(offset);
				// Check for the next available starting token
				int nextHighlightable = leftover.indexOf(highlightables[0]) + offset;
				for (int i = 0; i < highlightables.length; i++) {
					int thisHighlightable = leftover.indexOf(highlightables[i]) + offset;
					if (leftover.indexOf(highlightables[i]) < nextHighlightable && thisHighlightable > -1)
						nextHighlightable = thisHighlightable;
				}

				// If theres no other highlightables, dump the text
				if (nextHighlightable - offset == -1 || nextHighlightable == -1) {
					break;
				}

				// Otherwise, insert all plain text up until the next higlightable
				doc.insertString(offset, response.substring(offset, nextHighlightable), defaultSet);
				offset = nextHighlightable;
				leftover = response.substring(offset);

				if (leftover.contains("<think>")) {
					offset += 7;
					leftover = response.substring(offset);
					String thinkString = response.substring(offset, leftover.indexOf("</think>") + 8 + offset);
					doc.insertString(offset - 7, "<think>" + thinkString, thinkHighlightSet);
					offset += thinkString.length();
				} else if (leftover.contains("```")) {
					offset += 3;
					leftover = response.substring(offset);
					String codeString = response.substring(offset, leftover.indexOf("```") + 3 + offset);
					doc.insertString(offset - 3, "```" + codeString, codeHighlightSet);
					offset += codeString.length();
				}
				leftover = null;
			} catch (BadLocationException e) {
				e.printStackTrace();
			}

		}

		try {
			doc.insertString(offset, response.substring(offset), defaultSet);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		responseArea.setDocument(doc);
		responseArea.setEditable(false);

		responseArea.setSize(this.getWidth(), responseArea.getHeight());

		if (!role.equalsIgnoreCase("user")) {
			JButton playAudioButton = new JButton("\u23EF");
			playAudioButton.addActionListener((ActionEvent e) -> {
				playAudio(response);
			});
			responsePanel.add(playAudioButton, BorderLayout.WEST);
		}

		responsePanel.add(responseArea, BorderLayout.CENTER);
		responsePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), role));
		responseArea.setBackground(Color.darkGray.darker());
		return responsePanel;
	}

	private void playAudio(String response) {
		if (response.contains("</think>")) {
			response = response.substring(response.indexOf("</think>") + 8);
		}
		response = response.replace('#', ' ').replace('*', ' ');
		try {
			System.out.println("Playing audio: " + response);
			feedbackBar.setVisible(true);
			final String ttsServer = ConfigManager.getConfigValue("tts");
			if (ttsServer.equals("eleven")) {
				TTSService.ttsElevenLabsPlayAudio(response);
			} else if (ttsServer.equals("piper"))
				TTSService.ttsPiperPlayAudio(response);
			else if (ttsServer.equals("koko"))
				TTSService.kokoPlayAudio(response);
			feedbackBar.setVisible(false);
		} catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to play audio: " + e1.getMessage());
		}

	}

	private JPanel generateChatImagePanel(String role, BufferedImage image) {
		JPanel responsePanel = new JPanel();
		responsePanel.setLayout(new BorderLayout());

		int widthRatio = image.getWidth() / (chatHistoryPanel.getWidth() - 20);

		JLabel screengrab = new JLabel(new ImageIcon(image.getScaledInstance(chatHistoryPanel.getWidth() - 20,
				image.getHeight() / widthRatio, BufferedImage.SCALE_SMOOTH)));

		responsePanel.add(screengrab, BorderLayout.CENTER);
		responsePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), role));

		return responsePanel;
	}

	private JScrollPane createChatHistoryPanel() {
		chatHistoryPanel = new JPanel();
		JScrollPane chatScroller = new JScrollPane(chatHistoryPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		BoxLayout layout = new BoxLayout(chatHistoryPanel, BoxLayout.PAGE_AXIS);
		chatHistoryPanel.setLayout(layout);

		return chatScroller;
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem clearChat = new JMenuItem("Clear");
		JMenuItem uploadImage = new JMenuItem("Upload Image");
		JMenuItem uploadFile = new JMenuItem("Upload File");
		JMenuItem saveChat = new JMenuItem("Save Chat");
		JMenuItem loadChat = new JMenuItem("Load Chat");
		JMenuItem config = new JMenuItem("Settings");
		JMenuItem exit = new JMenuItem("Quit");

		clearChat.addActionListener((ActionEvent e) -> {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the chat?", "Clear Chat?",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				chat.clearMessageHistory();
				chatHistoryPanel.removeAll();
				repaint();
				revalidate();
			}
		});

		uploadImage.addActionListener((ActionEvent e) -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				final String[] approvedExtensions = { ".png", ".jpg", ".jpeg", ".bmp" };

				@Override
				public boolean accept(File arg0) {
					if (arg0.isDirectory())
						return true;
					String name = arg0.getName();
					for (var extension : approvedExtensions)
						if (name.endsWith(extension))
							return true;
					return false;
				}

				@Override
				public String getDescription() {
					return "Image files";
				}

			});
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			int selectionResult = fileChooser.showOpenDialog(this);
			if (selectionResult == JFileChooser.APPROVE_OPTION) {
				File imageFile = fileChooser.getSelectedFile();
				try {
					final BufferedImage image = ImageIO.read(imageFile);

					String encoded = encodeImage(image);
					chat.updateMessageHistory(new ChatImageMessage("User", encoded));
					chatHistoryPanel.add(generateChatImagePanel("User", image));

					repaint();
					revalidate();
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(this, "Failed to load image: " + e1.getMessage());
				}
			}
		});

		uploadFile.addActionListener((ActionEvent e) -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			int selectionResult = fileChooser.showOpenDialog(this);
			if (selectionResult == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				try {
					FileInputStream fin = new FileInputStream(file);
					String fileContents = new String(fin.readAllBytes());
					fin.close();
					chat.updateMessageHistory(
							new ChatMessage("user", "Below is the contents of the file " + file.getName()));
					chat.updateMessageHistory(new ChatMessage("user", fileContents));
					chatHistoryPanel.add(generateChatPanel("User", file.getName()));

					repaint();
					revalidate();
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(this, "Failed to read file: " + e1.getMessage());
				}
			}
		});

		config.addActionListener((ActionEvent e) -> {
			ConfigManager.openSettingsEditor(this);
			if (!modelName.equals(ConfigManager.getConfigValue("modelName"))) {
				System.out.println("Model Change");
				modelName = ConfigManager.getConfigValue("modelName");
				if (ConfigManager.getConfigValue("enableTools").equals("true")) {
					var newChat = new ChatToolsRequest(ConfigManager.getConfigValue("modelName"), endpoint, tools);
					newChat.setMessageHistory(chat.getChatHistory());
					chat = newChat;
					request = new LLMRequest(chat, null);
				} else {
					var newChat = new ChatRequest(ConfigManager.getConfigValue("modelName"), endpoint);
					newChat.setMessageHistory(chat.getChatHistory());
					chat = newChat;
					request = new LLMRequest(chat, null);
				}
			}

			if (ConfigManager.getConfigValue("enableTools").equals("true") && !(chat instanceof ChatToolsRequest)) {
				var newChat = new ChatToolsRequest(ConfigManager.getConfigValue("modelName"), endpoint, tools);
				newChat.setMessageHistory(chat.getChatHistory());
				chat = newChat;
				request = new LLMRequest(chat, null);
				System.out.println("Tools Enabled");
			} else if (ConfigManager.getConfigValue("enableTools").equals("false")
					&& (chat instanceof ChatToolsRequest)) {
				var newChat = new ChatRequest(ConfigManager.getConfigValue("modelName"), endpoint);
				newChat.setMessageHistory(chat.getChatHistory());
				chat = newChat;
				request = new LLMRequest(chat, null);
				System.out.println("Tools Disabled (General Chat)");
			}
		});

		exit.addActionListener((ActionEvent e) -> {
			int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Quit?",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION)
				System.exit(0);
		});

		fileMenu.add(clearChat);
		fileMenu.add(uploadImage);
		fileMenu.add(uploadFile);
		fileMenu.add(config);
		fileMenu.add(exit);

		menuBar.add(fileMenu);
		return menuBar;
	}

}
