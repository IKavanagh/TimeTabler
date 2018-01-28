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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Timetable content provider and its clients. A
 * contract defines the information that a client needs to access the provider
 * as one or more data tables. A contract is a public, non-extendable (final)
 * class that contains constants defining column names and URIs. A well-written
 * client depends only on the constants in the contract.
 */
public final class Timetable {

    public static final String AUTHORITY = "ie.clashoftheash.timetabler.provider";

    // This class cannot be instantiated
    private Timetable() {
    }

	/*
     * URI definitions
	 */

    /**
     * The scheme part for this provider's URI
     */
    private static final String SCHEME = "content://";

    /**
     * Events table contract
     */
    public static final class Events implements BaseColumns {

        private Events() {
        }

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "events";

		/*
         * URI definitions
		 */

        /**
         * Path parts for the URIs
         */

        /**
         * Path part for the Events URI
         */
        private static final String PATH_EVENTS = "/events";

        /**
         * Path part for the Event ID URI
         */
        private static final String PATH_EVENT_ID = "/events/";

        /**
         * 0-relative position of an event ID segment in the path part of an
         * event ID URI
         */
        public static final int EVENT_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY
                + PATH_EVENTS);

        /**
         * The content URI base for a single event. Callers must append a
         * numeric note id to this Uri to retrieve a event
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME
                + AUTHORITY + PATH_EVENT_ID);

        /**
         * The content URI match pattern for a single event, specified by its
         * ID. Use this to match incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME
                + AUTHORITY + PATH_EVENT_ID + "/#");

		/*
		 * MIME type definitions
		 */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.clashoftheash.timetabler.event";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.clashoftheash.timetabler.event";
        
		/*
		 * Column definitions
		 */

        /**
         * Column name for the type of event
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_EVENT_TYPE = "event_type";

        /**
         * Column name for the module code and name
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_MODULE = "module";

        /**
         * Column name for the lecturer(s)
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_LECTURER = "lecturer";

        /**
         * Column name for the location(s)
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_LOCATION = "location";

        /**
         * Column name for the start timestamp
         * <p/>
         * Type: UNSIGNED INT (time since epoch in ms)
         * </P>
         */
        public static final String COLUMN_NAME_START = "start";

        /**
         * Column name for the end timestamp
         * <p/>
         * Type: UNSIGNED INT (time since epoch in ms)
         * </P>
         */
        public static final String COLUMN_NAME_END = "end";

        /**
         * Column name for additional notes for event
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_NOTES = "notes";

        /**
         * Column name for the semester
         * <p/>
         * Type: TINYINT(1)
         * </P>
         */
        public static final String COLUMN_NAME_SEMESTER = "sem";

        /**
         * Column name for the week
         * <p/>
         * Type: TINYINT(2)
         * </P>
         */
        public static final String COLUMN_NAME_WEEK = "week";

        /**
         * Column name for the day event is on
         * <p/>
         * Type: CHAR(3)
         * </P>
         */
        public static final String COLUMN_NAME_DAY = "day";

        /**
         * Column name for the time of event
         * <p/>
         * Type: CHAR(5)
         * </P>
         */
        public static final String COLUMN_NAME_TIME = "time";

        /**
         * Column name for sync data of event
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_SYNC_DATA = "sync_data";

        /**
         * Column name for flag if user created event
         * <p/>
         * Type: TINYINT(1)
         * </P>
         */
        public static final String COLUMN_NAME_USER_CREATED = "user_created";

        /**
         * Column name for flag if user deleted event
         * <p/>
         * Type: TINYINT(1)
         * </P>
         */
        public static final String COLUMN_NAME_USER_DELETED = "user_deleted";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = COLUMN_NAME_START + " ASC";

    }
}