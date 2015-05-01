/**
 * Copyright(C) 2013 Patrik Dufresne Service Logiciel <info@patrikdufresne.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.patrikdufresne.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class to display localized string.
 * 
 * @author Patrik Dufresne
 * 
 */
public class Localized {
	/**
	 * Define the default bundle name containing the localized resource.
	 */
	private static final String BUNDLE_NAME = "messages"; //$NON-NLS-1$

	// Get the locale once
	private static final Locale locale = Locale.getDefault();

	/**
	 * Utility function to localize a string.
	 * 
	 * @return
	 */
	public static String _(Class<?> clazz, String key) {
		return get(clazz, key);
	}

	public static String _(Class<?> clazz, String key, Object... args) {
		return Localized.load(clazz).format(key, args);
	}

	/**
	 * Get one localized string. Prefer way is to call load() and then multiple
	 * call to get().
	 * 
	 * @param cls
	 * @param key
	 */
	public static String format(Class<?> clazz, String key, Object... args) {
		return Localized.load(clazz).format(key, args);
	}

	/**
	 * Get one localized string. Prefer way is to call load() and then multiple
	 * call to get().
	 * 
	 * @param cls
	 * @param key
	 */
	public static String get(Class<?> clazz, String key) {
		return Localized.load(clazz).get(key);
	}

	/**
	 * Create a new Localized instance for the given class.
	 * 
	 * @return
	 */
	public static Localized load(Class<?> cls) {
		return load(cls, BUNDLE_NAME);
	}

	public static Localized load(Class<?> cls, String resName) {
		// With the class name, create a new resource name to find the
		// .properties file
		String name = resName;
		String baseName = cls.getName();
		int index = baseName.lastIndexOf('.');
		if (index != -1) {
			name = baseName.substring(0, index).replace('.', '/') + '/'
					+ resName;
		}

		try {
			return new Localized(ResourceBundle.getBundle(name, locale,
					cls.getClassLoader()));
		} catch (MissingResourceException e) {
			e.printStackTrace();
			return new Localized(null);
		}
	}

	/**
	 * The resource bundle.
	 */
	private ResourceBundle res;

	/**
	 * Private constructor to avoid creating a utility class.
	 */
	private Localized(ResourceBundle res) {
		this.res = res;
	}

	/**
	 * Returns a localized formated string using the specified format string and
	 * arguments.
	 * 
	 * @param key
	 *            the key to the localized string
	 * @param args
	 *            the arguments
	 * @return the string
	 */
	public String format(String key, Object... args) {
		return String.format(locale, get(key), args);
	}

	/**
	 * Return the required localized string
	 * 
	 * @param key
	 *            a key to the localized string
	 * @return the localized key
	 */
	public String get(String key) {
		if (this.res != null) {
			try {
				return this.res.getString(key);
			} catch (MissingResourceException e) {
				// Print the exception into the console
				e.printStackTrace();
			}
		}
		return "LOCALIZE " + key; //$NON-NLS-1$
	}
}
