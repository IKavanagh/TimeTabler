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

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class TimetableProvider extends ContentProvider {
	private static final String TAG = "TimetableProvider";

	/**
	 * The database that the provider uses as its underlying data store
	 */
	private static final String DATABASE_NAME = "timetabler.db";

	/**
	 * Standard projection for the interesting columns of a normal event
	 */
	public static final String[] READ_EVENT_PROJECTION = new String[] {
			Timetable.Events._ID, Timetable.Events.COLUMN_NAME_EVENT_TYPE,
			Timetable.Events.COLUMN_NAME_MODULE,
			Timetable.Events.COLUMN_NAME_LECTURER,
			Timetable.Events.COLUMN_NAME_LOCATION,
			Timetable.Events.COLUMN_NAME_START,
			Timetable.Events.COLUMN_NAME_END,
			Timetable.Events.COLUMN_NAME_NOTES };
	public static final int READ_EVENT_ID_INDEX = 0;
	public static final int READ_EVENT_EVENT_TYPE_INDEX = 1;
	public static final int READ_EVENT_MODULE_INDEX = 2;
	public static final int READ_EVENT_LECTURER_INDEX = 3;
	public static final int READ_EVENT_LOCATION_INDEX = 4;
	public static final int READ_EVENT_START_INDEX = 5;
	public static final int READ_EVENT_END_INDEX = 6;
	public static final int READ_EVENT_NOTES_INDEX = 7;

	/**
	 * The database version
	 */
	private static final int DATABASE_VERSION = 1;

	/**
	 * A projection map used to select columns from the database
	 */
	private static final HashMap<String, String> sEventsProjectionMap;

	/*
	 * Constants used by the Uri matcher to choose an action based on the
	 * pattern of the incoming URI
	 */
	// The incoming URI matches the Events URI pattern
	private static final int EVENTS = 1;

	// The incoming URI matches the Event ID URI pattern
	private static final int EVENT_ID = 2;

	/**
	 * A UriMatcher instance
	 */
	private static final UriMatcher sUriMatcher;

	// Handle to a new DatabaseHelper.
	private EventsDatabaseHelper mDBHelper;

	/**
	 * A block that instantiates and sets static objects
	 */
	static {

		/*
		 * Creates and initializes the URI matcher
		 */
		// Create a new instance
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		// Add a pattern that routes URIs terminated with "events" to a EVENTS
		// operation
		sUriMatcher.addURI(Timetable.AUTHORITY, "events", EVENTS);

		// Add a pattern that routes URIs terminated with "events" plus an
		// integer
		// to a event ID operation
		sUriMatcher.addURI(Timetable.AUTHORITY, "events/#", EVENT_ID);

		/*
		 * Creates and initializes a projection map that returns all columns
		 */

		// Creates a new projection map instance. The map returns a column name
		// given a string. The two are usually equal.
		sEventsProjectionMap = new HashMap<String, String>();

		// Maps the string "_ID" to the column name "_ID"
		sEventsProjectionMap.put(Timetable.Events._ID, Timetable.Events._ID);

		// Maps the string "type" to the column name "type"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_EVENT_TYPE,
				Timetable.Events.COLUMN_NAME_EVENT_TYPE);

		// Maps the string "module" to the column name "module"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_MODULE,
				Timetable.Events.COLUMN_NAME_MODULE);

		// Maps the string "lecturers" to the column name "lecturers"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_LECTURER,
				Timetable.Events.COLUMN_NAME_LECTURER);

		// Maps the string "location" to the column name "location"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_LOCATION,
				Timetable.Events.COLUMN_NAME_LOCATION);

		// Maps the string "start" to the column name "start"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_START,
				Timetable.Events.COLUMN_NAME_START);

		// Maps the string "end" to the column name "end"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_END,
				Timetable.Events.COLUMN_NAME_END);

		// Maps the string "notes" to the column name "notes"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_NOTES,
				Timetable.Events.COLUMN_NAME_NOTES);

		// Maps the string "sem" to the column name "sem"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_SEMESTER,
				Timetable.Events.COLUMN_NAME_SEMESTER);

		// Maps the string "week" to the column name "week"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_WEEK,
				Timetable.Events.COLUMN_NAME_WEEK);

		// Maps the string "day" to the column name "day"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_DAY,
				Timetable.Events.COLUMN_NAME_DAY);

		// Maps the string "time" to the column name "time"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_TIME,
				Timetable.Events.COLUMN_NAME_TIME);

		// Maps the string "data" to the column name "data"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_SYNC_DATA,
				Timetable.Events.COLUMN_NAME_SYNC_DATA);

		// Maps the string "created" to the column name "created"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_USER_CREATED,
				Timetable.Events.COLUMN_NAME_USER_CREATED);

		// Maps the string "deleted" to the column name "deleted"
		sEventsProjectionMap.put(Timetable.Events.COLUMN_NAME_USER_DELETED,
				Timetable.Events.COLUMN_NAME_USER_DELETED);
	}

	/**
	 * 
	 * This class helps open, create, and upgrade the database file. Set to
	 * package visibility for testing purposes.
	 */
	static class EventsDatabaseHelper extends SQLiteOpenHelper {

		private static final String SQL_CREATE_TABLE = "CREATE TABLE "
				+ Timetable.Events.TABLE_NAME + " (" + Timetable.Events._ID
				+ " INTEGER PRIMARY KEY,"
				+ Timetable.Events.COLUMN_NAME_EVENT_TYPE
				+ " TEXT DEFAULT NULL," + Timetable.Events.COLUMN_NAME_MODULE
				+ " TEXT DEFAULT NULL," + Timetable.Events.COLUMN_NAME_LECTURER
				+ " TEXT DEFAULT NULL," + Timetable.Events.COLUMN_NAME_LOCATION
				+ " TEXT DEFAULT NULL," + Timetable.Events.COLUMN_NAME_START
				+ " UNSIGNED INT NOT NULL," + Timetable.Events.COLUMN_NAME_END
				+ " UNSIGNED INT NOT NULL,"
				+ Timetable.Events.COLUMN_NAME_NOTES + " TEXT DEFAULT NULL,"
				+ Timetable.Events.COLUMN_NAME_SEMESTER
				+ " TINYINT(1) DEFAULT '0',"
				+ Timetable.Events.COLUMN_NAME_WEEK
				+ " TINYINT(2) DEFAULT '0'," + Timetable.Events.COLUMN_NAME_DAY
				+ " CHAR(3) DEFAULT NULL," + Timetable.Events.COLUMN_NAME_TIME
				+ " VARCHAR(5) DEFAULT NULL,"
				+ Timetable.Events.COLUMN_NAME_SYNC_DATA
				+ " TEXT DEFAULT NULL,"
				+ Timetable.Events.COLUMN_NAME_USER_CREATED
				+ " TINYINT(1) DEFAULT '0',"
				+ Timetable.Events.COLUMN_NAME_USER_DELETED
				+ " TINYINT(1) DEFAULT '0'" + ");";

		EventsDatabaseHelper(Context context) {
			// calls the super constructor, requesting the default cursor
			// factory.
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * 
		 * Creates the underlying database with table name and column names
		 * taken from the Timetable class.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(SQL_CREATE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			// Logs that the database is being upgraded
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which may destroy all old data");

			// Opening db transaction
			db.beginTransactionNonExclusive();

			try {
				// Renaming old table to _temp
				db.execSQL("ALTER TABLE " + Timetable.Events.TABLE_NAME
						+ " RENAME TO " + Timetable.Events.TABLE_NAME + "_temp");

				// Different action depending on version
				if (oldVersion == 1 && newVersion == 2) {
					db.execSQL(SQL_CREATE_TABLE);
				}

				// Copy contents across
				Cursor cur = db.query(Timetable.Events.TABLE_NAME + "_temp",
						new String[] { Timetable.Events._ID,
								Timetable.Events.COLUMN_NAME_EVENT_TYPE,
								Timetable.Events.COLUMN_NAME_MODULE,
								Timetable.Events.COLUMN_NAME_LECTURER,
								Timetable.Events.COLUMN_NAME_LOCATION,
								Timetable.Events.COLUMN_NAME_START,
								Timetable.Events.COLUMN_NAME_END,
								Timetable.Events.COLUMN_NAME_NOTES,
								Timetable.Events.COLUMN_NAME_SEMESTER,
								Timetable.Events.COLUMN_NAME_WEEK,
								Timetable.Events.COLUMN_NAME_DAY,
								Timetable.Events.COLUMN_NAME_TIME,
								Timetable.Events.COLUMN_NAME_SYNC_DATA,
								Timetable.Events.COLUMN_NAME_USER_CREATED,
								Timetable.Events.COLUMN_NAME_USER_DELETED },
						null, null, null, null, Timetable.Events._ID + " ASC");

				ContentValues values = new ContentValues();

				if (cur.moveToFirst()) {
					do {

						values.put(Timetable.Events._ID, cur.getLong(0));
						values.put(Timetable.Events.COLUMN_NAME_EVENT_TYPE,
								cur.getString(1));
						values.put(Timetable.Events.COLUMN_NAME_MODULE,
								cur.getString(2));
						values.put(Timetable.Events.COLUMN_NAME_LECTURER,
								cur.getString(3));
						values.put(Timetable.Events.COLUMN_NAME_LOCATION,
								cur.getString(4));
						values.put(Timetable.Events.COLUMN_NAME_START,
								cur.getLong(5));
						values.put(Timetable.Events.COLUMN_NAME_END,
								cur.getLong(6));
						values.put(Timetable.Events.COLUMN_NAME_NOTES,
								cur.getString(7));
						values.put(Timetable.Events.COLUMN_NAME_SEMESTER,
								cur.getLong(8));
						values.put(Timetable.Events.COLUMN_NAME_WEEK,
								cur.getLong(9));
						values.put(Timetable.Events.COLUMN_NAME_DAY,
								cur.getString(10));
						values.put(Timetable.Events.COLUMN_NAME_TIME,
								cur.getString(11));
						values.put(Timetable.Events.COLUMN_NAME_SYNC_DATA,
								cur.getString(12));
						values.put(Timetable.Events.COLUMN_NAME_USER_CREATED,
								cur.getLong(13));
						values.put(Timetable.Events.COLUMN_NAME_USER_DELETED,
								cur.getLong(14));

					} while (cur.moveToNext());
				}

				db.insert(Timetable.Events.TABLE_NAME, null, values);

				db.execSQL("DROP TABLE IF EXISTS "
						+ Timetable.Events.TABLE_NAME + "_temp");

				db.setTransactionSuccessful();
			} catch (Exception e) {
				// Drop old table
				db.execSQL("DROP TABLE IF EXISTS "
						+ Timetable.Events.TABLE_NAME);

				// Create new table
				db.execSQL(SQL_CREATE_TABLE);

				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		}
	}

	/**
	 * 
	 * Initializes the provider by creating a new DatabaseHelper
	 */
	@Override
	public boolean onCreate() {

		// Creates a new helper object. Note that the database itself isn't
		// opened until
		// something tries to access it, and it's only created if it doesn't
		// already exist.
		mDBHelper = new EventsDatabaseHelper(getContext());

		// Assumes that any failures will be reported by a thrown exception.
		return true;
	}

	/**
	 * This method is called when a client calls
	 * {@link android.content.ContentResolver#query(Uri, String[], String, String[], String)}
	 * . Queries the database and returns a cursor containing the results.
	 * 
	 * @return A cursor containing the results of the query. The cursor exists
	 *         but is empty if the query returns no results or an exception
	 *         occurs.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		// Constructs a new query builder and sets its table name
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setDistinct(true);
		qb.setTables(Timetable.Events.TABLE_NAME);

		/**
		 * Choose the projection and adjust the "where" clause based on URI
		 * pattern-matching.
		 */
		switch (sUriMatcher.match(uri)) {
		// If the incoming URI is for events, choose the Events projection
		case EVENTS:
			qb.setProjectionMap(sEventsProjectionMap);
			break;

		/*
		 * If the incoming URI is for a single event identified by its ID,
		 * chooses the event ID projection, and appends "_ID = <eventID>" to the
		 * where clause, so that it selects that single event
		 */
		case EVENT_ID:
			qb.setProjectionMap(sEventsProjectionMap);
			qb.appendWhere(Timetable.Events._ID + // the name of the ID column
					"=" +
					// the position of the event ID itself in the incoming URI
					uri.getPathSegments().get(
							Timetable.Events.EVENT_ID_PATH_POSITION));
			break;

		default:
			// If the URI doesn't match any of the known patterns, throw an
			// exception.
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		String orderBy;
		// If no sort order is specified, uses the default
		if (TextUtils.isEmpty(sortOrder))
			orderBy = Timetable.Events.DEFAULT_SORT_ORDER;
		else
			// otherwise, uses the incoming sort order
			orderBy = sortOrder;

		// Opens the database object in "read" mode, since no writes need to be
		// done.
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		// Create groupBy string from projection
		// Used to remove duplicates
		StringBuilder groupBy = new StringBuilder();
		for (String s : projection)
			if (!s.equals(BaseColumns._ID))
				groupBy.append(s).append(", ");

		if (groupBy.length() > 0)
			groupBy.delete(groupBy.lastIndexOf(", "), groupBy.length());

		// Check for start or end in selection, if present set last 3 digits to
		// 0 to remove millisecond error
		if (selection != null
				&& (selection.contains(Timetable.Events.COLUMN_NAME_START) || selection
						.contains(Timetable.Events.COLUMN_NAME_END))) {
			String[] split = selection.split("\\?");

			if (split.length == selectionArgs.length) {
				int i = 0;
				for (String s : split) {
					if (s.contains(Timetable.Events.COLUMN_NAME_START)
							|| s.contains(Timetable.Events.COLUMN_NAME_END)) {
						selectionArgs[i] = String.valueOf((Long
								.valueOf(selectionArgs[i]) / 1000) * 1000);
					}
					i++;
				}
			}
		}

		/*
		 * Performs the query. If no problems occur trying to read the database,
		 * then a Cursor object is returned; otherwise, the cursor variable
		 * contains null. If no records were selected, then the Cursor object is
		 * empty, and Cursor.getCount() returns 0.
		 */
		Cursor c = qb.query(db, // The database to query
				projection, // The columns to return from the query
				selection, // The columns for the where clause
				selectionArgs, // The values for the where clause
				groupBy.toString(), // group to remove duplicates
				null, // don't filter by row groups
				orderBy // The sort order
				);

		// Tells the Cursor what URI to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#getType(Uri)}. Returns the MIME
	 * data type of the URI given as a parameter.
	 * 
	 * @param uri
	 *            The URI whose MIME type is desired.
	 * @return The MIME type of the URI.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	public String getType(Uri uri) {
		/**
		 * Chooses the MIME type based on the incoming URI pattern
		 */
		switch (sUriMatcher.match(uri)) {

		// If the pattern is for events, returns the general content type.
		case EVENTS:
			return Timetable.Events.CONTENT_TYPE;

			// If the pattern is for event IDs, returns the event ID content
			// type.
		case EVENT_ID:
			return Timetable.Events.CONTENT_ITEM_TYPE;

			// If the URI pattern doesn't match any permitted patterns, throws
			// an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#insert(Uri, ContentValues)}.
	 * Inserts a new row into the database. This method sets up default values
	 * for any columns that are not included in the incoming map. If rows were
	 * inserted, then listeners are notified of the change.
	 * 
	 * @return The row ID of the inserted row.
	 * @throws SQLException
	 *             if the insertion fails.
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {

		// Validates the incoming URI. Only the full provider URI is allowed for
		// inserts.
		if (sUriMatcher.match(uri) != EVENTS)
			throw new IllegalArgumentException("Unknown URI " + uri);

		// A map to hold the new record's values.
		ContentValues values;

		// If the incoming values map is not null, uses it for the new values.
		if (initialValues != null)
			values = new ContentValues(initialValues);
		else
			// Otherwise, create a new value map
			values = new ContentValues();

		// Check for start or end in content values, if present set last 3
		// digits to 0 to remove millisecond error
		if (values.size() > 0) {
			if (values.containsKey(Timetable.Events.COLUMN_NAME_START)) {
				long temp = values
						.getAsLong(Timetable.Events.COLUMN_NAME_START);
				temp = (temp / 1000) * 1000;
				values.put(Timetable.Events.COLUMN_NAME_START, temp);
			}
			if (values.containsKey(Timetable.Events.COLUMN_NAME_END)) {
				long temp = values.getAsLong(Timetable.Events.COLUMN_NAME_END);
				temp = (temp / 1000) * 1000;
				values.put(Timetable.Events.COLUMN_NAME_END, temp);
			}
		}

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		// Performs the insert and returns the ID of the new event.
		long rowId = db.insert(Timetable.Events.TABLE_NAME, // The table to
															// insert into.
				Timetable.Events.COLUMN_NAME_MODULE, // A hack, SQLite sets this
														// column value to null
														// if values is empty.
				values // A map of column names, and the values to insert into
						// the columns.
				);

		// If the insert succeeded, the row ID exists.
		if (rowId > 0) {
			// Creates a URI with the event ID pattern and the new row ID
			// appended to it.
			Uri eventUri = ContentUris.withAppendedId(
					Timetable.Events.CONTENT_ID_URI_BASE, rowId);

			// Notifies observers registered against this provider that the data
			// changed.
			getContext().getContentResolver().notifyChange(eventUri, null);
			return eventUri;
		}

		// If the insert didn't succeed, then the rowID is <= 0. Throws an
		// exception.
		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#delete(Uri, String, String[])}.
	 * Deletes records from the database. If the incoming URI matches the event
	 * ID URI pattern, this method deletes the one record specified by the ID in
	 * the URI. Otherwise, it deletes a a set of records. The record or records
	 * must also match the input selection criteria specified by where and
	 * whereArgs.
	 * 
	 * If rows were deleted, then listeners are notified of the change.
	 * 
	 * @return If a "where" clause is used, the number of rows affected is
	 *         returned, otherwise 0 is returned. To delete all rows and get a
	 *         row count, use "1" as the where clause.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		String finalWhere;

		// Check for start or end in where, if present set last 3 digits to
		// 0 to remove millisecond error
		if (where != null && (where.contains(Timetable.Events.COLUMN_NAME_START)
				|| where.contains(Timetable.Events.COLUMN_NAME_END))) {
			String[] split = where.split("\\?");

			if (split.length == whereArgs.length) {
				int i = 0;
				for (String s : split) {
					if (s.contains(Timetable.Events.COLUMN_NAME_START)
							|| s.contains(Timetable.Events.COLUMN_NAME_END))
						whereArgs[i] = String.valueOf((Long
								.valueOf(whereArgs[i]) / 1000) * 1000);
					i++;
				}
			}
		}

		int count;

		// Does the delete based on the incoming URI pattern.
		switch (sUriMatcher.match(uri)) {

		// If the incoming pattern matches the general pattern for events, does
		// a delete
		// based on the incoming "where" columns and arguments.
		case EVENTS:
			count = db.delete(Timetable.Events.TABLE_NAME, // The database table
															// name
					where, // The incoming where clause column names
					whereArgs // The incoming where clause values
					);
			break;

		// If the incoming URI matches a single event ID, does the delete based
		// on the
		// incoming data, but modifies the where clause to restrict it to the
		// particular event ID.
		case EVENT_ID:
			/*
			 * Starts a final WHERE clause by restricting it to the desired
			 * event ID.
			 */
			finalWhere = Timetable.Events._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming event ID
							get(Timetable.Events.EVENT_ID_PATH_POSITION);

			// If there were additional selection criteria, append them to the
			// final
			// WHERE clause
			if (where != null)
				finalWhere = finalWhere + " AND " + where;

			// Performs the delete.
			count = db.delete(Timetable.Events.TABLE_NAME, // The database table
															// name.
					finalWhere, // The final WHERE clause
					whereArgs // The incoming where clause values.
					);
			break;

		// If the incoming pattern is invalid, throws an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		/*
		 * Gets a handle to the content resolver object for the current context,
		 * and notifies it that the incoming URI changed. The object passes this
		 * along to the resolver framework, and observers that have registered
		 * themselves for the provider are notified.
		 */
		getContext().getContentResolver().notifyChange(uri, null);

		// Returns the number of rows deleted.
		return count;
	}

	/**
	 * This is called when a client calls
	 * {@link android.content.ContentResolver#update(Uri,ContentValues,String,String[])}
	 * Updates records in the database. The column names specified by the keys
	 * in the values map are updated with new data specified by the values in
	 * the map. If the incoming URI matches the event ID URI pattern, then the
	 * method updates the one record specified by the ID in the URI; otherwise,
	 * it updates a set of records. The record or records must match the input
	 * selection criteria specified by where and whereArgs. If rows were
	 * updated, then listeners are notified of the change.
	 * 
	 * @param uri
	 *            The URI pattern to match and update.
	 * @param values
	 *            A map of column names (keys) and new values (values).
	 * @param where
	 *            An SQL "WHERE" clause that selects records based on their
	 *            column values. If this is null, then all records that match
	 *            the URI pattern are selected.
	 * @param whereArgs
	 *            An array of selection criteria. If the "where" param contains
	 *            value placeholders ("?"), then each placeholder is replaced by
	 *            the corresponding element in the array.
	 * @return The number of rows updated.
	 * @throws IllegalArgumentException
	 *             if the incoming URI pattern is invalid.
	 */
	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		int count;
		String finalWhere;

		// Check for start or end in where, if present set last 3 digits to
		// 0 to remove millisecond error
		if (where != null && (where.contains(Timetable.Events.COLUMN_NAME_START)
				|| where.contains(Timetable.Events.COLUMN_NAME_END))) {
			String[] split = where.split("\\?");

			if (split.length == whereArgs.length) {
				int i = 0;
				for (String s : split) {
					if (s.contains(Timetable.Events.COLUMN_NAME_START)
							|| s.contains(Timetable.Events.COLUMN_NAME_END))
						whereArgs[i] = String.valueOf((Long
								.valueOf(whereArgs[i]) / 1000) * 1000);
					i++;
				}
			}
		}

		// Check for start or end in content values, if present set last 3
		// digits to 0 to remove millisecond error
		if (values.containsKey(Timetable.Events.COLUMN_NAME_START)) {
			long temp = values.getAsLong(Timetable.Events.COLUMN_NAME_START);
			temp = (temp / 1000) * 1000;
			values.put(Timetable.Events.COLUMN_NAME_START, temp);
		}
		if (values.containsKey(Timetable.Events.COLUMN_NAME_END)) {
			long temp = values.getAsLong(Timetable.Events.COLUMN_NAME_END);
			temp = (temp / 1000) * 1000;
			values.put(Timetable.Events.COLUMN_NAME_END, temp);
		}

		// Does the update based on the incoming URI pattern
		switch (sUriMatcher.match(uri)) {

		// If the incoming URI matches the general events pattern, does the
		// update based on
		// the incoming data.
		case EVENTS:

			// Does the update and returns the number of rows updated.
			count = db.update(Timetable.Events.TABLE_NAME, // The database table
															// name.
					values, // A map of column names and new values to use.
					where, // The where clause column names.
					whereArgs // The where clause column values to select on.
					);
			break;

		// If the incoming URI matches a single event ID, does the update based
		// on the incoming
		// data, but modifies the where clause to restrict it to the particular
		// event ID.
		case EVENT_ID:
			/*
			 * Starts creating the final WHERE clause by restricting it to the
			 * incoming event ID.
			 */
			finalWhere = Timetable.Events._ID + // The ID column name
					" = " + // test for equality
					uri.getPathSegments(). // the incoming event ID
							get(Timetable.Events.EVENT_ID_PATH_POSITION);

			// If there were additional selection criteria, append them to the
			// final WHERE
			// clause
			if (where != null)
				finalWhere = finalWhere + " AND " + where;

			// Does the update and returns the number of rows updated.
			count = db.update(Timetable.Events.TABLE_NAME, // The database table
															// name.
					values, // A map of column names and new values to use.
					finalWhere, // The final WHERE clause to use
								// placeholders for whereArgs
					whereArgs // The where clause column values to select on, or
								// null if the values are in the where argument.
					);
			break;
		// If the incoming pattern is invalid, throws an exception.
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		/*
		 * Gets a handle to the content resolver object for the current context,
		 * and notifies it that the incoming URI changed. The object passes this
		 * along to the resolver framework, and observers that have registered
		 * themselves for the provider are notified.
		 */
		getContext().getContentResolver().notifyChange(uri, null);

		// Returns the number of rows updated.
		return count;
	}

}
