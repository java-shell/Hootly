package com.javashell.danschirripa.desktopassistant;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class ConfigManager {

	public static HashMap<String, String> configTable = new HashMap<String, String>();
	private static String configPath = System.getProperty("user.home") + "/.chatwindow.cfg";
	private static File configFile = new File(configPath);

	private static boolean isFirstLaunch = false;

	static {
		configTable.put("modelName", "gemma3:latest");
		configTable.put("ollamaURI", "http://localhost:11434/api/chat");
		configTable.put("enableTools", "false");
		configTable.put("tts", "koko");
		configTable.put("ttsEndpoint", "http://localhost:5000/");
		configTable.put("ttsKey", "");
		configTable.put("autoPlay", "false");
		configTable.put("braveKey", "");

		if (!configFile.exists()) {
			isFirstLaunch = true;
			try {
				FileOutputStream fout = new FileOutputStream(configFile);
				PrintStream out = new PrintStream(fout);
				for (var configKey : configTable.keySet()) {
					out.println(configKey + "\033" + configTable.get(configKey));
				}
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("FAILED TO CREATE CONFIG FILE, RUNNING WITH ONLY DEFAULTS");
			}
		}

		try {
			FileInputStream fin = new FileInputStream(configFile);
			Scanner sc = new Scanner(fin);
			while (sc.hasNextLine()) {
				String[] kv = sc.nextLine().split("\033");
				if (kv.length != 2) {
					continue;
				}
				configTable.put(kv[0], kv[1]);
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("FAILED TO READ FROM CONFIG FILE, RUNNING WITH ONLY DEFAULTS");
			JOptionPane.showMessageDialog(null, "Failed to read from config file, running with only defaults");
		}
	}

	public static boolean isFirstLaunch() {
		return isFirstLaunch;
	}

	public static String getConfigValue(String configKey) {
		return configTable.get(configKey);
	}

	public static void saveConfig() {
		try {
			FileOutputStream fout = new FileOutputStream(configFile);
			PrintStream out = new PrintStream(fout);
			for (var configKey : configTable.keySet()) {
				out.println(configKey + "\033" + configTable.get(configKey));
			}
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("FAILED TO SAVE CONFIG FILE");
			JOptionPane.showMessageDialog(null, "Failed to save config file, check console for more information");
		}
	}

	public static void openSettingsEditor(JFrame parent) {
		JDialog settingsEditor = new JDialog(parent, "Settings");
		settingsEditor.setModal(true);
		JPanel settingsListPanel = new JPanel();
		var layout = new BoxLayout(settingsListPanel, BoxLayout.PAGE_AXIS);
		settingsListPanel.setLayout(layout);
		JScrollPane settingsScroller = new JScrollPane(settingsListPanel);
		settingsEditor.setLayout(new BorderLayout());
		settingsEditor.setSize(500, 500);

		HashMap<String, JTextField> fieldKVMap = new HashMap<String, JTextField>();

		for (var configKey : configTable.keySet()) {
			JPanel configKVPanel = new JPanel();
			JLabel key = new JLabel(configKey);
			JTextField value = new JTextField(configTable.get(configKey));

			configKVPanel.add(key);
			configKVPanel.add(value);

			fieldKVMap.put(configKey, value);

			settingsListPanel.add(configKVPanel);
		}

		JButton applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (var configKey : configTable.keySet()) {
					configTable.put(configKey, fieldKVMap.get(configKey).getText());
				}
				saveConfig();
				settingsEditor.setVisible(false);
			}
		});

		settingsEditor.add(settingsScroller, BorderLayout.CENTER);
		settingsEditor.add(applyButton, BorderLayout.SOUTH);
		settingsEditor.pack();

		Point centerPoint = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();

		settingsEditor.setLocation(centerPoint.x - (settingsEditor.getWidth() / 2),
				centerPoint.y - (settingsEditor.getHeight() / 2));
		settingsEditor.setVisible(true);
	}

}
