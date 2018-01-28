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

import ie.clashoftheash.timetabler.authenticator.AccountUtils;
import ie.clashoftheash.timetabler.utils.NetworkUtils;
import ie.clashoftheash.timetabler.utils.Utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ACRA;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class TimetableParser {
	private static final String TAG = "TimetableParser";

	private final Context mContext;
	private final NetworkUtils network;

	public TimetableParser(Context context) {
		mContext = context;
		network = new NetworkUtils(context);
	}

	public int[] beginParsing(ContentProviderClient provider)
			throws IOException {
		if (!network.connectionAvailable())
			return new int[3];

		String programmeCode = AccountUtils.getProgrammeCode(mContext);
		String year = AccountUtils.getYear(mContext);

		Element[] timetables = getTimetableHtml(programmeCode, year);

		boolean update = !(TimetableUtils.isDatabaseEmpty(mContext));

		int[] changes1 = parseTimetableBody(provider, timetables[0], 1, update);
		int[] changes2 = parseTimetableBody(provider, timetables[1], 2, update);

		return Utils.addArrays(changes1, changes2);
	}

	private int[] parseTimetableBody(ContentProviderClient provider,
			Element body, int semester, boolean update) {
		// 9 am on first monday
		Calendar startTime = Calendar.getInstance(TimetableUtils.TIMEZONE_UTC);
		startTime.setTimeInMillis(TimetableUtils.getFirstMonday());

		startTime.setLenient(true);
		startTime.set(Calendar.HOUR_OF_DAY, 9);

		Elements tableRows = body.getElementsByAttributeValue("border", "1")
				.first().children().first().children(); // Rows within table for
														// timetable

		int[] changes = new int[3];
		HashSet<Long> eventIds = new HashSet<Long>();

		// Needs to be declared out here for when there are multiple rows per
		// day
		String day = null;

		for (Element row : tableRows) { // Loop through each row in table
			Elements cells = row.children(); // Cells per row

			// Time the class will take place at
			Calendar classTime = Calendar
					.getInstance(TimetableUtils.TIMEZONE_UTC);
			classTime.setTimeInMillis(startTime.getTimeInMillis());

			classTime.setLenient(true);

			// First row in table is times denoted by first cell being empty
			if (TextUtils.isEmpty(cells.first().html()))
				continue;

			// We now have rows for each day left only
			int repeatDay = 1;

			for (Element cell : cells) {
				// First cell contains 3 letter day code, day can span multiple
				// rows
				if (cell.html().length() == 3) {
					day = cell.html();

					repeatDay = Integer.valueOf(cell.attr("rowspan"));
					// Processing of this cell is finished continue on to next
					continue;
				}

				// Empty cells account for a 30min time span and their content
				// is &nbsp
				if (cell.html().equals("&nbsp;")) {
					classTime.add(Calendar.MINUTE, 30);
					continue;
				}

				String weeksData = getEventWeeksFromCell(cell); // Weeks in
																// calendar
																// event is on

				HashSet<Integer> weeks = new HashSet<Integer>();
				parseWeeks(weeksData, weeks);

				Set<Long> times = computeEventTimes(weeks,
						classTime.getTimeInMillis());

				String eventType = getEventTypeFromCell(cell);
				String location = getEventRoomFromCell(cell);
				String lecturer = getEventLecturerFromCell(cell);
				String module = getEventModuleFromCell(cell);

				String time = TimetableUtils.getDBTimeFormat(classTime
						.getTimeInMillis());

				int numHalfHours = Integer
						.parseInt(cell.attr("colspan").trim());

				classTime.add(Calendar.MINUTE, 30 * numHalfHours);

				String data = TimetableUtils.getDBDataFormat(eventType, module,
						lecturer, location, semester, day, time,
						(numHalfHours / 2));

				for (long t : times) {
					int week = TimetableUtils.getWeekNumber(t);

					Time temp = new Time(Time.TIMEZONE_UTC);
					temp.set(t);

					temp.minute += 30 * numHalfHours - 10;

					long end = temp.toMillis(false);

					ContentValues values = buildContentValues(eventType,
							module, lecturer, location, t, end, null, semester,
							week, day, time, data, false, update);

					if (update) {
						// Update event
						Cursor cur = getEventSyncData(provider, t, day, time,
								semester);

						if (cur != null && cur.moveToFirst()) {
							// Event at this time update event
							long id = cur.getLong(0);
							String oldData = cur.getString(1);
							cur.close();

							// true because no need to update if data is the
							// same
							boolean result = true;

							if (!TextUtils.equals(data, oldData)) {
								// Data doesn't match, update
								result = updateEvent(provider, values, id);

								if (result)
									changes[1]++;
							}

							if (result)
								eventIds.add(id);
						} else {
							if (cur != null)
								cur.close();
							// No event at this time insert event
							long id = insertEvent(provider, values);

							if (id != -1) {
								changes[0]++;
								eventIds.add(id);
							}
						}
					} else {
						// Insert event
						long id = insertEvent(provider, values);

						if (id != -1) {
							changes[0]++;
							eventIds.add(id);
						}
					}
				}
			}

			// if next row is not the same day increment time
			if (--repeatDay <= 0)
				startTime.add(Calendar.DAY_OF_YEAR, 1);
		}

		if (update) {
			Set<Long> ids = getAllSyncCreatedEventIds(provider, semester);

			// To account for times when timetable website is down only
			// delete events if number of inserted/updated events is greater
			// than or equal to a quarter of all events for this semester
			if (eventIds.size() * 4 >= ids.size()) {

				for (long id : eventIds)
					ids.remove(id);

				for (long id : ids) {
					if (!deleteEventSync(provider, id)) {
						// TODO: add error processing on failure
                        ;
					}
					changes[2]++;
				}
			}
		}

		return changes;
	}

	Element[] getTimetableHtml(String programmeCode, String year)
			throws IOException {
		String[] html = new String[2];
		Element[] body = new Element[2];

		for (int i = 0; i < 2; i++) {
			URL url = network.buildTimetableUrl(programmeCode, year, i + 1);
			HttpURLConnection conn = network.openConnection(url);

			if (network.connectionRedirected(url, conn))
				return null;

			html[i] = network.downloadURL(conn);

			body[i] = Jsoup.parse(html[i]).body();
		}

		return body;
	}

	private String getEventTypeFromCell(Element cell) {
		// Get type by navigating to 3 tables in cell, selecting 1st then
		// selecting the first row (only 1) and the first cell within it
		return Utils.capitaliseEachWord(cell.children().get(0)
				.getElementsByTag("tr").first().children().first().html()
				.trim());
	}

	private String getEventRoomFromCell(Element cell) {
		// Get room by navigating to 3 tables in cell, selecting 1st then
		// selecting the first row (only 1) and the last cell within it
		return cell.children().get(0).getElementsByTag("tr").first().children()
				.last().html().trim().toUpperCase(Locale.UK);
	}

	private String getEventLecturerFromCell(Element cell) {
		// Get lecturer by navigating to 3 tables in cell, selecting 2nd then
		// selecting the first row (only 1) and the first cell within it
		return Utils.capitaliseEachWord(cell.children().get(1)
				.getElementsByTag("tr").first().children().first().html()
				.trim());
	}

	private String getEventModuleFromCell(Element cell) {
		// Get moduleName by navigating to 3 tables in cell, selecting 2nd then
		// selecting the first row (only 1) and the last cell within it
		String moduleName = Utils.capitaliseEachWord(cell.children().get(1)
				.getElementsByTag("tr").first().children().last().html().trim()
				.toLowerCase(Locale.UK));
		// Get weeks by navigating to 3 tables in cell, selecting 3rd then
		// selecting the first row (only 1) and the first cell within it
		String moduleCode = cell.children().get(2).getElementsByTag("tr")
				.first().children().first().html().trim()
				.toUpperCase(Locale.UK);

		Matcher m = Pattern.compile("[A-z]{2}[0-9]{3}").matcher(moduleCode);

		if (m.find())
			moduleCode = m.group();

		return moduleCode + " " + moduleName;
	}

	private String getEventWeeksFromCell(Element cell) {
		// Get weeks by navigating to 3 tables in cell, selecting 3rd then
		// selecting the first row (only 1) and the last cell within it
		return cell.children().get(2).getElementsByTag("tr").first().children()
				.last().html().trim();
	}

	/**
	 * Parses weeks data from timetable and puts each week into the set
	 * 
	 * @param data
	 *            weeks data from timetable to operate on
	 * @param weeks
	 *            TreeSet to parsed weeks into
	 */
	private void parseWeeks(String data, Set<Integer> weeks) {
		if (data.contains(",")) { // Multiple ranges or values
			String[] split = data.split(",");
			for (String s : split)
				parseWeeks(s, weeks);
		} else if (data.contains("-")) { // Single range
			String[] split = data.split("-");
			int i = Integer.parseInt(split[0].trim());
			int j = Integer.parseInt(split[1].trim());

			while (i <= j)
				// loop through each int in range
				weeks.add(i++);
		} else { // Single value
			weeks.add(Integer.valueOf(data.trim()));
		}
	}

	/**
	 * Computes long values denoting time in millis from epoch for each week in
	 * set
	 * 
	 * @param weeks
	 *            weeks event takes place on
	 * @param startTime
	 *            time of event in first week of semester
	 * @return array of times event takes place at
	 */
	private Set<Long> computeEventTimes(Set<Integer> weeks, long startTime) {
		HashSet<Long> times = new HashSet<Long>();

		long millisInWeek = 7 * 24 * 3600 * 1000;

		for (int w : weeks)
			times.add(startTime + (w - 1) * millisInWeek);

		return times;
	}

	public static ContentValues buildContentValues(String eventType,
			String module, String lecturer, String location, long start,
			long end, String notes, int semester, int week, String day,
			String time, String data, boolean user, boolean update) {
		ContentValues values = new ContentValues();

		values.put(Timetable.Events.COLUMN_NAME_EVENT_TYPE,
				Utils.replaceEncodedChars(eventType));
		values.put(Timetable.Events.COLUMN_NAME_MODULE,
				Utils.replaceEncodedChars(module));
		values.put(Timetable.Events.COLUMN_NAME_LECTURER,
				Utils.replaceEncodedChars(lecturer));
		values.put(Timetable.Events.COLUMN_NAME_LOCATION,
				Utils.replaceEncodedChars(location));
		values.put(Timetable.Events.COLUMN_NAME_START, start);
		values.put(Timetable.Events.COLUMN_NAME_END, end);
		values.put(Timetable.Events.COLUMN_NAME_NOTES, notes);
		if (semester == 1 || semester == 2)
			values.put(Timetable.Events.COLUMN_NAME_SEMESTER, semester);
		if (week > 0 && week <= 52)
			values.put(Timetable.Events.COLUMN_NAME_WEEK, week);
		if (day != null)
			values.put(Timetable.Events.COLUMN_NAME_DAY, day);
		if (time != null)
			values.put(Timetable.Events.COLUMN_NAME_TIME, time);
		if (data != null)
			values.put(Timetable.Events.COLUMN_NAME_SYNC_DATA, data);

		if (user)
			values.put(Timetable.Events.COLUMN_NAME_USER_CREATED, 1 /* true */);
		else
			values.put(Timetable.Events.COLUMN_NAME_USER_CREATED, 0 /* false */);

		if (!update) {
			values.putNull(Timetable.Events._ID);
			values.put(Timetable.Events.COLUMN_NAME_USER_DELETED, 0 /* false */);
		}

		return values;
	}

	/**
	 * Inserts an event and returns its id or -1 if error
	 * 
	 * @return inserted event id or -1 if error
	 */
	public static long insertEvent(ContentProviderClient provider,
			ContentValues values) {
		try {
			Uri newUri = provider.insert(Timetable.Events.CONTENT_URI, values);
			return ContentUris.parseId(newUri);
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to insert event", e);
			ACRA.getErrorReporter().handleSilentException(e);
			return -1;
		}
	}

	/**
	 * Updates an event and returns success/fail indicator
	 * 
	 * @param id
	 *            id of event to update
	 * 
	 * @return true if update succeeded
	 */
	public static boolean updateEvent(ContentProviderClient provider,
			ContentValues values, long id) {
		String selection = Timetable.Events._ID + " = ?";
		String[] selectionArgs = { "" + id };

		try {
			return (provider.update(Timetable.Events.CONTENT_URI, values,
					selection, selectionArgs) == 1);
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to update event", e);
			ACRA.getErrorReporter().handleSilentException(e);
			return false;
		}
	}

	/**
	 * Permanently deletes an event created by the sync adapter only
	 *
	 * @param id
	 *            id of event to delete
	 * @return success/fail indicator
	 */
	private boolean deleteEventSync(ContentProviderClient provider, long id) {
		Uri uri = ContentUris.withAppendedId(Timetable.Events.CONTENT_URI, id);

		String selection = "(" + Timetable.Events.COLUMN_NAME_USER_CREATED
				+ " = 0)"; /* Only sync adapter created events can be deleted */

		try {
			int rows = provider.delete(uri, selection, null);
			return rows > 0;
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to delete event", e);
			ACRA.getErrorReporter().handleSilentException(e);
			return false;
		}
	}

	/**
	 * Deletes an event by setting its deletion flag in the database
	 * 
	 * @param id
	 *            id of event to delete
	 * @return success/fail indicator
	 */
	public static boolean deleteEventUser(ContentProviderClient provider,
			long id) {
		String selection = Timetable.Events._ID + " = ?";
		String[] selectionArgs = { "" + id };

		ContentValues values = new ContentValues();
		values.put(Timetable.Events.COLUMN_NAME_USER_DELETED, 1 /* true */);

		try {
			return provider.update(Timetable.Events.CONTENT_URI, values,
					selection, selectionArgs) == 1;
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to delete event", e);
			ACRA.getErrorReporter().handleSilentException(e);
			return false;
		}
	}

	/**
	 * Gets data for event based on start time of event
	 * 
	 * @return Cursor containing event data and id or null if none found
	 */
	private static Cursor getEventSyncData(ContentProviderClient provider,
			long start, String day, String time, int sem) {
		String[] projection = { Timetable.Events._ID,
				Timetable.Events.COLUMN_NAME_SYNC_DATA };
		String selection = "(" + Timetable.Events.COLUMN_NAME_START
				+ " = ?) AND (" + Timetable.Events.COLUMN_NAME_DAY
				+ " = ?) AND (" + Timetable.Events.COLUMN_NAME_TIME
				+ " = ?) AND (" + Timetable.Events.COLUMN_NAME_SEMESTER
				+ " = ?)";
		String[] selectionArgs = { String.valueOf(start), day, time,
				String.valueOf(sem) };

		try {
			return provider.query(Timetable.Events.CONTENT_URI, projection,
					selection, selectionArgs, null);
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to get event sync data", e);
			ACRA.getErrorReporter().handleSilentException(e);
			return null;
		}
	}

	/**
	 * Returns a list of ids for all events created by the sync adapter per
	 * semester
	 * 
	 * @param semester
	 *            semester to get event ids for
	 * @return array of event ids
	 */
	private Set<Long> getAllSyncCreatedEventIds(ContentProviderClient provider,
			int semester) {
		String[] projection = { Timetable.Events._ID };
		String selection = "(" + Timetable.Events.COLUMN_NAME_SEMESTER
				+ " = ?) AND (" + Timetable.Events.COLUMN_NAME_USER_CREATED
				+ " = '0')";

		String[] selectionArgs = { "" + semester };

		Cursor cur;
		try {
			cur = provider.query(Timetable.Events.CONTENT_URI, projection,
					selection, selectionArgs, null);
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to get all sync created event ids", e);
			ACRA.getErrorReporter().handleSilentException(e);
			return null;
		}
		if (cur == null)
			return null;

		HashSet<Long> ids = new HashSet<Long>();
		for (int i = 0; i < cur.getCount(); i++) {
			if (!cur.moveToNext())
				break;

			ids.add(cur.getLong(0));
		}

		cur.close();

		return ids;
	}

}