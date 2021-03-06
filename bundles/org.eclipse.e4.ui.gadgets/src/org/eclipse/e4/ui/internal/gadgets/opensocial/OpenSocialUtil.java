/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Boris Bokowski, IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.internal.gadgets.opensocial;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OpenSocialUtil {

	private static int moduleId = 0;

	public static OSGModule loadModule(String uriString) {
		java.net.URI uri;
		OSGModule module;
		try {
			uri = new java.net.URI(uriString);
			module = parseModuleXML(uri);
		} catch (Exception e) {
			throw new RuntimeException("Could not load module", e);
		}
		OSGLocale locale = determineLocale(module);

		Map<String, String> hangmanMap = new HashMap<String, String>();
		hangmanMap.put("__MODULE_ID__", String.valueOf(moduleId++));
		hangmanMap.put("__ENV_google_apps_auth_path__", "render#");

		populateHangmanMapFromLocale(hangmanMap, locale, uri);
		populateHangmanMapFromUserPrefs(hangmanMap, module.userPrefs);

		module.title = hangmanExpand(module.title, hangmanMap);
		module.description = hangmanExpand(module.description, hangmanMap);
		module.author = hangmanExpand(module.author, hangmanMap);

		// System.out.println("Gadget: " + module.title);
		// System.out.println("Author: " + module.author);
		// System.out.println("Description: " + module.description);

		for (OSGContent content : new HashSet<OSGContent>(module.contents
				.values())) {
			if ("url".equalsIgnoreCase(content.type)) {
				content.href = hangmanExpand(content.href, hangmanMap);
				// System.out.println("URL: " + content.href);
			} else if ("html".equalsIgnoreCase(content.type)) {
				content.value = hangmanExpand(content.value, hangmanMap);
				// System.out.println("HTML: ...");
			} else {
				// System.out.println("unhandled type: " + content.type);
			}
			// System.out.println("Views: " + content.view);
		}
		return module;
	}

	private static void populateHangmanMapFromUserPrefs(
			Map<String, String> hangmanMap, List<OSGUserPref> userPrefs) {
		for (OSGUserPref pref : userPrefs) {
			hangmanMap.put("__UP_" + pref.name + "__", pref.defaultValue);
		}
	}

	private static void populateHangmanMapFromLocale(
			Map<String, String> hangmanMap, OSGLocale locale, java.net.URI uri) {
		hangmanMap.put("__BIDI_START_EDGE__", "left");
		hangmanMap.put("__BIDI_END_EDGE__", "right");
		hangmanMap.put("__BIDI_DIR__", "ltr");
		hangmanMap.put("__BIDI_REVERSE_DIR__", "rtl");
		if (locale != null) {
			if ("rtl".equals(locale.languageDirection)) {
				hangmanMap.put("__BIDI_START_EDGE__", "right");
				hangmanMap.put("__BIDI_END_EDGE__", "left");
				hangmanMap.put("__BIDI_DIR__", "rtl");
				hangmanMap.put("__BIDI_REVERSE_DIR__", "ltr");
			}
			String messagesURIString = locale.messagesURI;
			if (messagesURIString != null) {
				if (!messagesURIString.startsWith("http")) {
					if (!messagesURIString.startsWith("/")) {
						String uriString = uri.toString();
						messagesURIString = uriString.substring(0, uriString
								.lastIndexOf('/'))
								+ "/" + messagesURIString;
					} else {
						messagesURIString = "http://" + uri.getHost()
								+ messagesURIString;
					}
				}
				try {
					addMessagesToMap(hangmanMap, messagesURIString);
				} catch (Exception e) {
					throw new RuntimeException("Could not parse messages", e);
				}
			} else {
				addMessagesToMap(hangmanMap, locale.messages);
			}
		}
	}

	private static OSGLocale determineLocale(OSGModule module) {
		OSGLocale locale = module.locales.get(new LocaleKey(System
				.getProperty("osgi.nl"), System.getProperty("user.country")));
		if (locale == null) {
			locale = module.locales.get(new LocaleKey(System
					.getProperty("user.language"), System
					.getProperty("user.country")));
		}
		if (locale == null) {
			locale = module.locales.get(new LocaleKey(System
					.getProperty("osgi.nl"), null));
		}
		if (locale == null) {
			locale = module.locales.get(new LocaleKey(System
					.getProperty("user.language"), null));
		}
		if (locale == null) {
			locale = module.locales.get(new LocaleKey(null, System
					.getProperty("user.country")));
		}
		if (locale == null) {
			locale = module.locales.get(new LocaleKey(null, null));
		}
		return locale;
	}

	private static OSGModule parseModuleXML(java.net.URI uri) throws Exception {
		final OSGModule[] result = { null };
		parseXML(uri, new DefaultHandler() {
			OSGModule module;
			OSGContent content;
			StringBuffer contentBuffer;
			OSGLocale locale;
			String currentMsgName = null;
			StringBuffer currentMsgValue = null;

			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				if (contentBuffer != null) {
					contentBuffer.append("<" + qName);
					for (int i = 0; i < attributes.getLength(); i++) {
						contentBuffer.append(" " + attributes.getQName(i)
								+ "=\"" + attributes.getValue(i) + "\"");
					}
					contentBuffer.append(">");
				} else if ("ModulePrefs".equalsIgnoreCase(qName)) {
					module = new OSGModule(attributes.getValue("title"),
							attributes.getValue("author"), attributes
									.getValue("description"));
					result[0] = module;
				} else if ("Locale".equalsIgnoreCase(qName)) {
					locale = new OSGLocale(attributes.getValue("lang"),
							attributes.getValue("country"), attributes
									.getValue("language_direction"), attributes
									.getValue("messages"));
					module.locales.put(new LocaleKey(locale.lang,
							locale.country), locale);
				} else if ("msg".equalsIgnoreCase(qName)) {
					currentMsgName = attributes.getValue("name");
					currentMsgValue = new StringBuffer();
				} else if ("UserPref".equalsIgnoreCase(qName)) {
					module.userPrefs.add(new OSGUserPref(attributes
							.getValue("name"), attributes
							.getValue("default_value")));
				} else if ("Content".equalsIgnoreCase(qName)) {
					content = new OSGContent(attributes.getValue("type"),
							attributes.getValue("view"), attributes
									.getValue("href"));
					contentBuffer = new StringBuffer();
				}
			}

			public void characters(char[] ch, int start, int length)
					throws SAXException {
				if (contentBuffer != null) {
					contentBuffer.append(ch, start, length);
				}
				if (currentMsgName != null) {
					currentMsgValue.append(ch, start, length);
				}
			}

			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				if ("Content".equalsIgnoreCase(qName) && content != null
						&& contentBuffer != null) {
					content.value = contentBuffer.toString();
					StringTokenizer t = new StringTokenizer(content.view, ",");
					if (!t.hasMoreTokens()) {
						module.contents.put("default", content);
					} else
						while (t.hasMoreTokens()) {
							module.contents.put(t.nextToken(), content);
						}
					contentBuffer = null;
				} else if ("Locale".equalsIgnoreCase(qName)) {
					locale = null;
				} else if ("msg".equalsIgnoreCase(qName)) {
					locale.messages.put(currentMsgName, currentMsgValue
							.toString());
					currentMsgName = null;
				} else if (contentBuffer != null) {
					contentBuffer.append("</" + qName);
					contentBuffer.append(">");
				}
				content = null;
			}
		});
		return result[0];
	}

	private static void addMessagesToMap(final Map<String, String> hangmanMap,
			String messagesURIString) throws Exception {
		// System.out.println("Loading message bundle from: " +
		// messagesURIString);
		DefaultHandler handler = new DefaultHandler() {
			String name;
			String value;

			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				name = attributes.getValue("name");
			}

			public void characters(char[] ch, int start, int length)
					throws SAXException {
				value = new String(ch, start, length);
			}

			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				if ("msg".equalsIgnoreCase(qName) && name != null
						&& value != null) {
					hangmanMap.put("__MSG_" + name + "__", value);
				}
			}
		};
		parseXML(new java.net.URI(messagesURIString), handler);
	}

	private static void addMessagesToMap(Map<String, String> hangmanMap,
			Map<String, String> messages) {
		for (Map.Entry<String, String> entry : messages.entrySet()) {
			hangmanMap.put("__MSG_" + entry.getKey() + "__", entry.getValue());
		}
	}

	private static void parseXML(java.net.URI uri, DefaultHandler handler)
			throws MalformedURLException, IOException,
			ParserConfigurationException, SAXException {
		URL messagesURL = uri.toURL();
		URLConnection connection = messagesURL.openConnection();
		String encoding = connection.getContentEncoding();
		if (encoding == null) {
			String contentType = connection.getHeaderField("Content-Type");
			int charsetIndex;
			if (contentType != null
					&& (charsetIndex = contentType.indexOf("charset=")) != -1) {
				int charsetBegin = charsetIndex + "charset=".length();
				int charsetEnd = charsetBegin;
				while (contentType.length() > charsetEnd
						&& " ,;".indexOf(contentType.charAt(charsetEnd)) == -1) {
					charsetEnd++;
				}
				if (charsetEnd > charsetBegin) {
					encoding = contentType.substring(charsetBegin, charsetEnd);
				}
			}
		}
		InputStream is = connection.getInputStream();
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser = spf.newSAXParser();
			InputSource source = new InputSource(is);
			if (encoding != null) {
				source.setEncoding(encoding);
			}
			parser.parse(source, handler);
		} finally {
			is.close();
		}
	}

	private static String hangmanExpand(String s, Map<String, String> hangmanMap) {
		if (s == null) {
			return null;
		}
		StringBuffer result = new StringBuffer(s.length());
		int currentIndex = 0;
		int indexOfUnderscores;
		while ((indexOfUnderscores = s.indexOf("__", currentIndex)) != -1) {
			result.append(s.substring(currentIndex, indexOfUnderscores));
			int indexOfNextUnderscores = s
					.indexOf("__", indexOfUnderscores + 2);
			if (indexOfNextUnderscores != -1) {
				String key = s.substring(indexOfUnderscores,
						indexOfNextUnderscores + 2);
				String value = hangmanMap.get(key);
				if (value != null) {
					result.append(value);
				} else {
					result.append(key);
				}
				currentIndex = indexOfNextUnderscores + 2;
			} else {
				result.append("__");
				currentIndex = indexOfUnderscores + 2;
			}
		}
		result.append(s.substring(currentIndex));
		return result.toString();
	}

}
