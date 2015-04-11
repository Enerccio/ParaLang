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

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Utility class used to keep reference on DateFormat. It' also provide a single
 * way to get different kind of DateFormat using styles : DATE, TIME, DATETIME
 * and MONTH.
 * 
 * @author Patrik Dufresne
 * 
 */
public class DateFormatRegistry {

    static ResourceBundle bundle = ResourceBundle.getBundle("com.patrikdufresne.util.messages"); //$NON-NLS-1$

    static Map<String, DateFormat> table;

    /**
     * Short style:
     * <ul>
     * <li>Date: 10 Sep 2012</li>
     * <li>Time: 9:02</li>
     * </ul>
     */
    public static final int SHORT = 1 << 0;

    /**
     * Long style.
     * <ul>
     * <li>Date: Tuesday 30 October 2012</li>
     * <li>Time: 9:02:01</li>
     * </ul>
     */
    public static final int LONG = 1 << 1;

    /**
     * Medium style.
     * <ul>
     * <li>Date: 30 October 2012</li>
     * <li>Time: 9:02:01</li>
     * </ul>
     */
    public static final int MEDIUM = 1 << 5;

    /**
     * Used to format the date part of the date object. e.g: Mon 2 January 2011
     */
    public static final int DATE = 1 << 2;
    /**
     * Used to format the time part of the date object. e.g.: 9:30
     */
    public static final int TIME = 1 << 3;

    /**
     * Used to format only the month part of the date. e.g.: January 2011
     */
    public static final int MONTH = 1 << 4;

    /**
     * Style to create a date & time. e.g.: Tue Oct 30, 10:04
     */
    public static final int DATETIME = 1 << 6;

    /**
     * Style to create ISO date.
     */
    public static final int ISO = 1 << 7;

    /**
     * Style to create day of week format. e.g.: Tue, Tuesday
     */
    public static final int DAY_OF_WEEK = 1 << 8;

    /**
     * Dot value (.)
     */
    private static final String DOT = "."; //$NON-NLS-1$

    private static final String PART1 = "part1"; //$NON-NLS-1$

    private static final String PART2 = "part2"; //$NON-NLS-1$

    private static final String PART3 = "part3"; //$NON-NLS-1$

    private static final String PART4 = "part4"; //$NON-NLS-1$

    private static final String RANGE = "range"; //$NON-NLS-1$

    private static final String SEPARATOR = "@sep"; //$NON-NLS-1$

    /**
     * Return a formated date string.
     * <p>
     * The style value is either one of the style constants defined in this
     * class, or must be built by <em>bitwise OR</em>'ing together (that is,
     * using the <code>int</code> "|" operator) two or more of those style
     * constants.
     * 
     * @param date
     *            the date object to format
     * @param style
     *            the date format style.
     * @return the formated string.
     */
    public static String format(Date date, int style) {
        return format(date, style, false);
    }

    /**
     * Return a formated date string.
     * <p>
     * The style value is either one of the style constants defined in this
     * class, or must be built by <em>bitwise OR</em>'ing together (that is,
     * using the <code>int</code> "|" operator) two or more of those style
     * constants.
     * 
     * @param date
     *            the date object to format
     * @param style
     *            the date format style
     * @param uppercase
     *            True to upper case the first letter of the string
     * @return the formated date
     */
    public static String format(Date date, int style, boolean uppercase) {
        return getFormat(style, uppercase).format(date);
    }

    /**
     * This function is used to format a date range into a string. The year,
     * month, day, hour and minute are included.
     * 
     * @param start
     * @param end
     * @return
     */
    public static String formatRange(Calendar start, Calendar end, int style) {
        // Since we want to generate a date format with inclusive date, if the
        // end date is ending on 00:00, then we must format the date with
        // previous value.
        Date endDate = end.getTime();
        // if (end.get(Calendar.MILLISECOND) == 0 && end.get(Calendar.SECOND) ==
        // 0
        // && end.get(Calendar.MINUTE) == 0) {
        // endDate = new Date(end.getTime().getTime() - 1);
        // }

        DateFormat[] formats;
        if (start.get(Calendar.YEAR) != end.get(Calendar.YEAR)) {
            formats = getFormatRange(style, PART4);
        } else if (start.get(Calendar.MONTH) != end.get(Calendar.MONTH)) {
            formats = getFormatRange(style, PART3);
        } else if (start.get(Calendar.DAY_OF_MONTH) != end.get(Calendar.DAY_OF_MONTH)) {
            formats = getFormatRange(style, PART2);
        } else {
            formats = getFormatRange(style, PART1);
        }
        return formats[0].format(start.getTime()) + formats[1].format(endDate);
    }

    /**
     * This function is used to format a date range.
     * 
     * @param start
     *            the start date
     * @param end
     *            the end date
     * @param style
     *            one of the RANGE_STYLE_* constants.
     * @return the formated date range
     */
    public static String formatRange(Date start, Date end, int style) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(start);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(end);
        return formatRange(cal1, cal2, style);
    }

    private static DateFormat[] getFormatRange(String path) {

        // Get resource
        String pattern = bundle.getString(path);
        String[] patterns = Pattern.compile(SEPARATOR).split(pattern);
        if (patterns.length < 2) {
            throw new IllegalArgumentException("missing seperator"); //$NON-NLS-1$
        }

        DateFormat[] formats = new DateFormat[2];
        formats[0] = getFormat(patterns[0], false);
        formats[1] = getFormat(patterns[1], false);
        return formats;

    }

    private static DateFormat getFormat(String pattern, final boolean capitalFirstLetter) {

        // Check the table.
        String key = pattern + capitalFirstLetter;
        if (table != null && table.containsKey(key)) {
            return table.get(key);
        }

        @SuppressWarnings("serial")
		DateFormat format = new SimpleDateFormat(pattern) {
            @Override
            public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
                StringBuffer buf = super.format(date, toAppendTo, pos);
                if (capitalFirstLetter) {
                    buf.replace(0, 1, buf.substring(0, 1).toUpperCase());
                }
                return buf;
            }
        };
        if (table == null) {
            table = new HashMap<String, DateFormat>();
            table.put(key, format);
        }
        return format;
    }

    public static DateFormat[] getFormatRange(int style, String part) {

        String key;
        if ((style & DATETIME) != 0) {
            key = "datetime"; //$NON-NLS-1$
        } else if ((style & TIME) != 0) {
            key = "time"; //$NON-NLS-1$
        } else if ((style & DATE) != 0) {
            key = "date"; //$NON-NLS-1$
        } else if ((style & MONTH) != 0) {
            key = "month"; //$NON-NLS-1$
        } else {
            throw new IllegalArgumentException("One of the constant DATE or DATETIME should be set"); //$NON-NLS-1$
        }

        String subkey = "medium"; //$NON-NLS-1$
        if ((style & SHORT) != 0) {
            subkey = "short"; //$NON-NLS-1$
        } else if ((style & LONG) != 0) {
            subkey = "long"; //$NON-NLS-1$
        }

        return getFormatRange(key + DOT + RANGE + DOT + subkey + DOT + part);

    }

    /**
     * Same as calling <code>getFormat(style, false)</code>.
     * <p>
     * The style value is either one of the style constants defined in this
     * class, or must be built by <em>bitwise OR</em>'ing together (that is,
     * using the <code>int</code> "|" operator) two or more of those style
     * constants.
     * 
     * @param style
     *            the date format style
     * @return
     */
    public static DateFormat getFormat(int style) {
        return getFormat(style, false);
    }

    /**
     * Return a date format with the specified style.
     * <p>
     * The style value is either one of the style constants defined in this
     * class, or must be built by <em>bitwise OR</em>'ing together (that is,
     * using the <code>int</code> "|" operator) two or more of those style
     * constants.
     * 
     * @param style
     *            the date format style.
     * @param capitalFirstLetter
     *            True to capitalize the first letter
     * @return the date format
     */
    public static DateFormat getFormat(int style, boolean capitalFirstLetter) {

        String key;
        if ((style & DATETIME) != 0) {
            key = "datetime"; //$NON-NLS-1$
        } else if ((style & TIME) != 0) {
            key = "time"; //$NON-NLS-1$
        } else if ((style & DATE) != 0) {
            key = "date"; //$NON-NLS-1$
        } else if ((style & MONTH) != 0) {
            key = "month"; //$NON-NLS-1$
        } else if ((style & DAY_OF_WEEK) != 0) {
            key = "day"; //$NON-NLS-1$
        } else {
            throw new IllegalArgumentException("One of the constant DATE, TIME, DATETIME or Month should be set"); //$NON-NLS-1$
        }

        String subkey = "medium"; //$NON-NLS-1$
        if ((style & SHORT) != 0) {
            subkey = "short"; //$NON-NLS-1$
        } else if ((style & LONG) != 0) {
            subkey = "long"; //$NON-NLS-1$
        } else if ((style & ISO) != 0) {
            subkey = "iso"; //$NON-NLS-1$
        }
        // Get resource
        String pattern = bundle.getString(key + DOT + subkey);
        return getFormat(pattern, capitalFirstLetter);

    }
}
