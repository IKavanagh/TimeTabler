/*
 * Copyright 2013 Ian Kavanagh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ie.clashoftheash.timetabler.provider;

import ie.clashoftheash.timetabler.provider.TimetableProvider.EventsDatabaseHelper;
import ie.clashoftheash.timetabler.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.text.format.Time;

public class TimetableUtils {

	public static final TimeZone TIMEZONE_UTC = TimeZone
			.getTimeZone(Time.TIMEZONE_UTC);

	/**
	 * Calculates the first Monday of semester 1 for the current academic year
	 * and returns the number of milliseconds since epoch in UTC
	 * 
	 * @return number of milliseconds since epoch representing 00:00:00 on the
	 *         first Monday of semester 1 in UTC
	 */
	public static long getFirstMonday() {
		Calendar cal = Calendar.getInstance(TIMEZONE_UTC);
		cal.setLenient(true);

		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		// Set milliseconds to 0 to avoid errors with slightly different times
		cal.set(Calendar.MILLISECOND, 0);

		// If month before october remove a year
		int m = cal.get(Calendar.MONTH);
		if (m < Calendar.OCTOBER)
			cal.set(Calendar.MONTH, m - 1);

		// Set date to 1st October
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		// If 1st Oct is Mon we want to subtract a week
		// Otherwise we want to go back to Monday
		int subtract = (5 + cal.get(Calendar.DAY_OF_WEEK)) % 7;
		if (subtract == 0)
			subtract = 7;

		cal.add(Calendar.DAY_OF_YEAR, -subtract);
		return cal.getTimeInMillis();
	}

	/**
	 * Computes the academic week number for the given time in milliseconds in
	 * UTC
	 * 
	 * @param time
	 *            time in milliseconds to get week number of
	 * @return week number, integer between 1 and 52 inclusive
	 */
	public static int getWeekNumber(long time) {
		Calendar cal = Calendar.getInstance(TIMEZONE_UTC);
		cal.setLenient(true);

		cal.setTimeInMillis(time);

		Calendar firstMonday = Calendar.getInstance(TIMEZONE_UTC);
		firstMonday.setTimeInMillis(getFirstMonday());

		int week = 1 + (cal.get(Calendar.WEEK_OF_YEAR) - firstMonday
				.get(Calendar.WEEK_OF_YEAR));
		while (week < 1)
			week += 52;

		return week;
	}

	/**
	 * Determines if both times occur on the same day
	 * 
	 * @param t1
	 *            First time to compare
	 * @param t2
	 *            Second time to compare
	 * @return true if both times occur on the same day, otherwise false
	 */
	public static boolean isSameDay(long t1, long t2) {
		Calendar c1 = Calendar.getInstance(TIMEZONE_UTC);
		c1.setTimeInMillis(t1);

		Calendar c2 = Calendar.getInstance(TIMEZONE_UTC);
		c2.setTimeInMillis(t2);

		return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
				&& c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
				&& c1.get(Calendar.DAY_OF_MONTH) == c2
						.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Determines if time t2 is the day after time t1
	 * 
	 * @param t1
	 *            First day
	 * @param t2
	 *            Day after
	 * @return true if t2 is day after t1
	 */
	public static boolean isDayAfter(long t1, long t2) {
		Calendar c1 = Calendar.getInstance(TIMEZONE_UTC);
		c1.setTimeInMillis(t1);

		Calendar c2 = Calendar.getInstance(TIMEZONE_UTC);
		c2.setTimeInMillis(t2);
		c2.add(Calendar.DAY_OF_YEAR, -1); // Go back one day

		// Compare if days are the same now
		return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
				&& c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
				&& c1.get(Calendar.DAY_OF_MONTH) == c2
						.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Determines if time t2 is the day before time t1
	 * 
	 * @param t1
	 *            First day
	 * @param t2
	 *            Day before
	 * @return true if t2 is day before t1
	 */
	public static boolean isDayBefore(long t1, long t2) {
		Calendar c1 = Calendar.getInstance(TIMEZONE_UTC);
		c1.setTimeInMillis(t1);

		Calendar c2 = Calendar.getInstance(TIMEZONE_UTC);
		c2.setTimeInMillis(t2);
		c2.add(Calendar.DAY_OF_YEAR, 1); // Go forward one day

		// Compare if days are the same now
		return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
				&& c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
				&& c1.get(Calendar.DAY_OF_MONTH) == c2
						.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Determines if time t1 is the same time and date as time t2
	 * 
	 * @param t1
	 *            First time to check
	 * @param t2
	 *            Second time to check
	 * @return true if t1 and t2 are the same date and time
	 */
	public static boolean isSameDateTime(long t1, long t2) {
		// Divide by 1000 to remove millisecond error
		return t1 / 1000 == t2 / 1000;
	}

	/**
	 * Takes a time in milliseconds and returns a string representation of the
	 * time for use in the database
	 * 
	 * @param time
	 *            time in milliseconds to get time string for
	 * @return String representation of the time as given by the time in
	 *         milliseconds passed
	 */
	public static String getDBTimeFormat(long time) {
		Calendar cal = Calendar.getInstance(TIMEZONE_UTC);
		cal.setTimeInMillis(time);

		return new SimpleDateFormat("kk:mm", Locale.ENGLISH).format(cal
				.getTime());
	}

	/**
	 * Helper method to concatenate the event data into a string seperated by |
	 */
	public static String getDBDataFormat(String eventType, String module,
			String lecturer, String location, int semester, String day,
			String time, int hours) {
		return Utils.replaceEncodedChars(eventType) + "|"
				+ Utils.replaceEncodedChars(module) + "|"
				+ Utils.replaceEncodedChars(lecturer) + "|"
				+ Utils.replaceEncodedChars(location) + "|" + semester + "|"
				+ day + "|" + time + "|" + hours;
	}

	/**
	 * Converts the given Calendar object into another Calendar object with the
	 * same day, month, year, hour, minute and second but a different timezone
	 * 
	 * @param cal
	 *            Calendar object to be converted
	 * @param timezone
	 *            the new timezone for the returned calendar
	 * @return Calendar object for the same time as passed Calendar object but
	 *         in a different timezone
	 */
	public static Calendar switchTimeZone(Calendar cal, TimeZone timezone) {
		Calendar newCal = Calendar.getInstance(timezone);

		// Time
		newCal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
		newCal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
		newCal.set(Calendar.SECOND, cal.get(Calendar.SECOND));

		// Set milliseconds to 0 to avoid errors with slightly different times
		cal.set(Calendar.MILLISECOND, 0);

		// Date
		newCal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR));

		return newCal;
	}

	/**
	 * Helper method to determine if the table is empty or not
	 * 
	 * @return true if the table is empty
	 */
	public static boolean isDatabaseEmpty(Context context) {
		return !eventsExistAfter(context,
					new Time(Time.TIMEZONE_UTC).normalize(true));
	}

	/**
	 * Helper method to determine if an event exists after a given time
	 * 
	 * @param time
	 *            number of milliseconds since epoch to check for event
	 * @return true if an event exists in the database
	 */
	private static boolean eventsExistAfter(Context context, long time) {
		String sql = "SELECT COUNT(*) FROM " + Timetable.Events.TABLE_NAME
				+ " WHERE " + Timetable.Events.COLUMN_NAME_START + " > '"
				+ time + "'";

		SQLiteDatabase db = new EventsDatabaseHelper(context)
				.getReadableDatabase();

		SQLiteStatement statement = db.compileStatement(sql);
		long count = statement.simpleQueryForLong();
		db.close();

		return count > 0;
	}

	/**
	 * Helper method to get the start of the next event in the database
	 * 
	 * @param time
	 *            in UTC to be checked for an event occuring after
	 * 
	 * @return milliseconds since epoch to start of next event
	 */
	public static long startOfNextEvent(Context context, long time) {
		String[] projection = { Timetable.Events.COLUMN_NAME_START };
		String selection = "(" + Timetable.Events.COLUMN_NAME_START + " > ?)";
		String[] selectionArgs = { "" + time };

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Timetable.Events.TABLE_NAME);
		Cursor cur = qb.query(
				new EventsDatabaseHelper(context).getReadableDatabase(),
				projection, selection, selectionArgs, null, null,
				Timetable.Events.DEFAULT_SORT_ORDER, "1");

		// If the query fails or the cursor is empty, stop
		if (cur == null || !cur.moveToFirst()) {

			// If the cursor is empty, simply close the cursor and return
			if (cur != null) {
				cur.close();
			}

			// If the cursor is null, return value indicating this
			return -1;
		}

		long t = cur.getLong(0);
		cur.close();
		return t;
	}

	/**
	 * Removes all data from database within a new thread
	 */
	public static void deleteDatabase(final Context context) {
		new Thread() {
			public void run() {
				// Delete all events in database
				context.getContentResolver().delete(
						Timetable.Events.CONTENT_URI, null, null);
			}
		}.start();
	}

}
