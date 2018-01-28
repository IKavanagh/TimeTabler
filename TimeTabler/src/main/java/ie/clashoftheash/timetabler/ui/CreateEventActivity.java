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

package ie.clashoftheash.timetabler.ui;

import ie.clashoftheash.timetabler.R;
import ie.clashoftheash.timetabler.authenticator.AccountUtils;
import ie.clashoftheash.timetabler.provider.Timetable;
import ie.clashoftheash.timetabler.provider.TimetableParser;
import ie.clashoftheash.timetabler.provider.TimetableProvider;
import ie.clashoftheash.timetabler.provider.TimetableUtils;
import ie.clashoftheash.timetabler.ui.dialogs.DatePickerFragment;
import ie.clashoftheash.timetabler.ui.dialogs.TimePickerFragment;
import ie.clashoftheash.timetabler.utils.LoadEvent;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CreateEventActivity extends Activity implements
		DatePickerFragment.OnDateSetListener,
		TimePickerFragment.OnTimeSetListener, LoadEvent.EventLoadListener {

	/**
	 * The fragment argument representing the event ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * Arguments for passing data to {@link SaveEvent}
	 */
	private static final String ARG_ITEM_EVENT_TYPE = "item_event_type";
	private static final String ARG_ITEM_MODULE = "item_module";
	private static final String ARG_ITEM_LECTURER = "item_lecturer";
	private static final String ARG_ITEM_LOCATION = "item_location";
	private static final String ARG_ITEM_START = "item_start";
	private static final String ARG_ITEM_END = "item_end";
	private static final String ARG_ITEM_NOTES = "item_notes";

	/**
	 * Constant used to pass selection arguments to cursor loader
	 */
	private static final String SELECTION_ARGS = "selection_args";

	/**
	 * Constants to denote which adapter we are working with
	 */
	private static final int MODULE = 0;
	private static final int LECTURER = 1;

	/**
	 * Constants to denote start or end time is being edited
	 */
	private static final int START = 0;
	private static final int END = 1;

	// View references
	private AutoCompleteTextView moduleView, lecturerView;
	private Button startDateView, startTimeView, endDateView, endTimeView;
	private EditText customEventTypeView, locationView, notesView;
	private ProgressBar mProgressBarView;
	private Spinner eventTypeView;

	// Loader and Adapter references for AutoCompleteTextViews
	private LoaderCallbacks loaderCallbacks;
	private SimpleCursorAdapter moduleAdapter, lecturerAdapter;

	private DateFormat mDateFormat, mTimeFormat;

	// Can only edit one event at a time
	private Calendar start, end;
	private static long id;

	/**
	 * Flag to denote if event should be saved
	 */
	private boolean saveEvent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		id = -1;

		setContentView(R.layout.activity_create_event);

		// Inflate a "Done/Cancel" custom action bar view.
		LayoutInflater inflater = (LayoutInflater) getActionBar()
				.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(
				R.layout.actionbar_custom_view_done_cancel, null);
		customActionBarView.findViewById(R.id.actionbar_done)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Done"
						saveEvent = true;

						finish();
					}
				});
		customActionBarView.findViewById(R.id.actionbar_cancel)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Cancel"
						saveEvent = false;

						finish();
					}
				});

		// Show the custom action bar view and hide the normal Home icon and
		// title.

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView,
				new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));

		// Set up references to views
		eventTypeView = (Spinner) findViewById(R.id.event_type);
		customEventTypeView = (EditText) findViewById(R.id.custom_event_type);
		locationView = (EditText) findViewById(R.id.location);
		moduleView = (AutoCompleteTextView) findViewById(R.id.module);
		lecturerView = (AutoCompleteTextView) findViewById(R.id.lecturer);
		notesView = (EditText) findViewById(R.id.notes);

		// Set up spinner
		EventTypeAdapter eventAdapter = EventTypeAdapter.createFromResource(
				this, R.array.event_types,
				android.R.layout.simple_spinner_item,
				android.R.layout.simple_spinner_dropdown_item);
		eventTypeView.setAdapter(eventAdapter);
		eventTypeView
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						if (view == null)
							return;

						TextView v = (TextView) view;
						if (v.getText().equals(getString(R.string.custom))) {
							customEventTypeView.setVisibility(View.VISIBLE);
							customEventTypeView.requestFocus();
						} else
							customEventTypeView.setVisibility(View.INVISIBLE);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		// Set up to and from date and time pickers
		// From (start)
		start = Calendar.getInstance();
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		start.add(Calendar.HOUR_OF_DAY, 1);

		// Add onClick listeners for time and date
		startDateView = (Button) findViewById(R.id.start_date);
		startTimeView = (Button) findViewById(R.id.start_time);

		setStartText();

		startDateView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerFragment fragment = new DatePickerFragment();

				Bundle args = new Bundle();
				args.putInt(DatePickerFragment.WHICH, START);
				args.putInt(DatePickerFragment.YEAR, start.get(Calendar.YEAR));
				args.putInt(DatePickerFragment.MONTH, start.get(Calendar.MONTH));
				args.putInt(DatePickerFragment.DAY,
						start.get(Calendar.DAY_OF_MONTH));
				fragment.setArguments(args);

				fragment.show(getFragmentManager(), "");
			}
		});
		startTimeView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TimePickerFragment fragment = new TimePickerFragment();

				Bundle args = new Bundle();
				args.putInt(TimePickerFragment.WHICH, START);
				args.putInt(TimePickerFragment.HOUR_OF_DAY,
						start.get(Calendar.HOUR_OF_DAY));
				args.putInt(TimePickerFragment.MINUTE,
						start.get(Calendar.MINUTE));
				fragment.setArguments(args);

				fragment.show(getFragmentManager(), "");
			}
		});

		// To (end)
		end = Calendar.getInstance();
		end.setTimeInMillis(start.getTimeInMillis());
		end.add(Calendar.HOUR_OF_DAY, 1);

		// Add onClick listeners for time and date
		endDateView = (Button) findViewById(R.id.end_date);
		endTimeView = (Button) findViewById(R.id.end_time);

		setEndText();

		endDateView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerFragment fragment = new DatePickerFragment();

				Bundle args = new Bundle();
				args.putInt(DatePickerFragment.WHICH, END);
				args.putInt(DatePickerFragment.YEAR, end.get(Calendar.YEAR));
				args.putInt(DatePickerFragment.MONTH, end.get(Calendar.MONTH));
				args.putInt(DatePickerFragment.DAY,
						end.get(Calendar.DAY_OF_MONTH));
				fragment.setArguments(args);

				fragment.show(getFragmentManager(), "");
			}
		});
		endTimeView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TimePickerFragment fragment = new TimePickerFragment();

				Bundle args = new Bundle();
				args.putInt(TimePickerFragment.WHICH, END);
				args.putInt(TimePickerFragment.HOUR_OF_DAY,
						end.get(Calendar.HOUR_OF_DAY));
				args.putInt(TimePickerFragment.MINUTE, end.get(Calendar.MINUTE));
				fragment.setArguments(args);

				fragment.show(getFragmentManager(), "");
			}
		});

		// Set up AutoCompleteTextViews
		loaderCallbacks = new LoaderCallbacks();

		moduleAdapter = new AutoCompleteTextViewCursorAdapter(this,
				android.R.layout.simple_dropdown_item_1line, null,
				new String[] { Timetable.Events.COLUMN_NAME_MODULE },
				new int[] { android.R.id.text1 }, 0);

		moduleView.setAdapter(moduleAdapter);

		// Requery database as text is changed
		moduleView.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 1) {
					Bundle args = new Bundle();
					args.putStringArray(SELECTION_ARGS, new String[] { "%"
							+ moduleView.getText().toString() + "%" });

					getLoaderManager().restartLoader(MODULE, args,
							loaderCallbacks);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});

		lecturerAdapter = new AutoCompleteTextViewCursorAdapter(this,
				android.R.layout.simple_dropdown_item_1line, null,
				new String[] { Timetable.Events.COLUMN_NAME_LECTURER },
				new int[] { android.R.id.text1 }, 0);

		lecturerView.setAdapter(lecturerAdapter);

		// Requery database as text is changed
		lecturerView.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 1) {
					Bundle args = new Bundle();
					args.putStringArray(SELECTION_ARGS, new String[] { "%"
							+ lecturerView.getText().toString() + "%" });

					getLoaderManager().restartLoader(LECTURER, args,
							loaderCallbacks);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});

		locationView = (EditText) findViewById(R.id.location);
		notesView = (EditText) findViewById(R.id.notes);

		mProgressBarView = (ProgressBar) findViewById(android.R.id.progress);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(ARG_ITEM_START)) {
				start = (Calendar) savedInstanceState
						.getSerializable(ARG_ITEM_START);
				setStartText();
			}
			if (savedInstanceState.containsKey(ARG_ITEM_END)) {
				end = (Calendar) savedInstanceState
						.getSerializable(ARG_ITEM_END);
				setEndText();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		AccountUtils.checkAccountExists(this);

		saveEvent = true;

		if (getIntent() != null && getIntent().getExtras() != null) {
			mProgressBarView.setVisibility(View.VISIBLE);

			// we have data passed
			id = getIntent().getExtras().getLong(ARG_ITEM_ID);
			setIntent(null);
			new LoadEvent(this).execute(id);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (saveEvent) {
			saveEvent = false;

			if (!empty(moduleView) || !empty(locationView)
					|| !empty(lecturerView) || !empty(notesView)) {
				// Event has data entered, save
				if (empty(moduleView))
					moduleView.setText(R.string.untitled);

				Bundle data = new Bundle();
				if (id != -1)
					data.putLong(ARG_ITEM_ID, id);

				String eventTypeText = (String) eventTypeView.getSelectedItem();
				if (getString(R.string.custom).equals(eventTypeText))
					eventTypeText = customEventTypeView.getText().toString();

				data.putString(ARG_ITEM_EVENT_TYPE, eventTypeText);
				data.putString(ARG_ITEM_MODULE, moduleView.getText().toString());
				data.putString(ARG_ITEM_LECTURER, lecturerView.getText()
						.toString());
				data.putString(ARG_ITEM_LOCATION, locationView.getText()
						.toString());
				data.putSerializable(ARG_ITEM_START, start);
				data.putSerializable(ARG_ITEM_END, end);
				data.putString(ARG_ITEM_NOTES, notesView.getText().toString());

				new SaveEvent().execute(data);
			} else
				Toast.makeText(this, getString(R.string.message_empty_event),
						Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(ARG_ITEM_START, start);
		outState.putSerializable(ARG_ITEM_END, end);
	}

	@Override
	public void onDateSet(int which, int year, int month, int day) {
		switch (which) {
		case START:
			start.set(Calendar.YEAR, year);
			start.set(Calendar.MONTH, month);
			start.set(Calendar.DAY_OF_MONTH, day);

			setStartText();
			validateEndTime();
			break;
		case END:
			end.set(Calendar.YEAR, year);
			end.set(Calendar.MONTH, month);
			end.set(Calendar.DAY_OF_MONTH, day);

			setEndText();
			validateStartTime();
			break;
		}
	}

	@Override
	public void onTimeSet(int which, int hour, int minute) {
		switch (which) {
		case START:
			start.set(Calendar.HOUR_OF_DAY, hour);
			start.set(Calendar.MINUTE, minute);

			setStartText();
			validateEndTime();
			break;
		case END:
			end.set(Calendar.HOUR_OF_DAY, hour);
			end.set(Calendar.MINUTE, minute);

			setEndText();
			validateStartTime();
			break;
		}
	}

	@Override
	public void OnEventLoadFinished(Cursor c) {
		String eventTypeText = c
				.getString(TimetableProvider.READ_EVENT_EVENT_TYPE_INDEX);

		String[] eventTypes = getResources()
				.getStringArray(R.array.event_types);

		int i = 0;
		while (i < eventTypes.length && !eventTypes[i].equals(eventTypeText))
			i++;

		if (i == eventTypes.length) {// Account for custom event type
			customEventTypeView.setText(eventTypeText);
			// Custom is last event type
			eventTypeView.setSelection(eventTypes.length - 1);
		} else
			eventTypeView.setSelection(i);

		moduleView.setText(c
				.getString(TimetableProvider.READ_EVENT_MODULE_INDEX));
		lecturerView.setText(c
				.getString(TimetableProvider.READ_EVENT_LECTURER_INDEX));
		locationView.setText(c
				.getString(TimetableProvider.READ_EVENT_LOCATION_INDEX));

		notesView
				.setText(c.getString(TimetableProvider.READ_EVENT_NOTES_INDEX));

		start.setTimeZone(TimetableUtils.TIMEZONE_UTC);
		start.setTimeInMillis(c
				.getLong(TimetableProvider.READ_EVENT_START_INDEX));
		start = TimetableUtils.switchTimeZone(start, TimeZone.getDefault());
		setStartText();

		end.setTimeZone(TimetableUtils.TIMEZONE_UTC);
		end.setTimeInMillis(c.getLong(TimetableProvider.READ_EVENT_END_INDEX));
		end = TimetableUtils.switchTimeZone(end, TimeZone.getDefault());
		setEndText();

		c.close();

		mProgressBarView.setVisibility(View.INVISIBLE);
	}

	/**
	 * Ensures that start time is before end time, called after end time changed
	 */
	private void validateStartTime() {
		if (end.before(start)
				|| TimetableUtils.isSameDateTime(start.getTimeInMillis(),
						end.getTimeInMillis())) {
			start.setTimeInMillis(end.getTimeInMillis());
			start.add(Calendar.HOUR_OF_DAY, -1);
		}

		setStartText();
	}

	/**
	 * Ensures that end time is after start time, called after start time
	 * changed
	 */
	private void validateEndTime() {
		if (end.before(start)
				|| TimetableUtils.isSameDateTime(start.getTimeInMillis(),
						end.getTimeInMillis())) {
			end.setTimeInMillis(start.getTimeInMillis());
			end.add(Calendar.HOUR_OF_DAY, 1);
		}

		setEndText();
	}

	/**
	 * Sets the correct text for the from (start) date and time
	 */
	private void setStartText() {
		startDateView.setText(getDisplayDate(start.getTime()));
		startTimeView.setText(getDisplayTime(start.getTime()));
	}

	/**
	 * Sets the correct text for the to (end) date and time
	 */
	private void setEndText() {
		endDateView.setText(getDisplayDate(end.getTime()));
		endTimeView.setText(getDisplayTime(end.getTime()));
	}

	/**
	 * Returns a formatted string for displaying the date
	 * 
	 * @param d
	 *            Time object to use
	 * @return formatted date string
	 */
	private String getDisplayDate(Date d) {
		if (mDateFormat == null) // only need to initialise once
			mDateFormat = android.text.format.DateFormat
					.getMediumDateFormat(this);

		return mDateFormat.format(d);
	}

	/**
	 * Returns a formatted string for displaying the time
	 * 
	 * @param d
	 *            Time object to use
	 * @return formatted time string
	 */
	private String getDisplayTime(Date d) {
		if (mTimeFormat == null) // only need to initialise once
			mTimeFormat = android.text.format.DateFormat.getTimeFormat(this);

		return mTimeFormat.format(d);
	}

	/**
	 * Method used to determine if text has been entered into an EditText field
	 * 
	 * @param v
	 *            EditText to check for entered text
	 * @return true if no text has been entered, false otherwise
	 */
	private boolean empty(TextView v) {
		try {
			return v.getText().toString().isEmpty();
		} catch (NullPointerException e) {
			// If a NullPointerException occurs the TextView is empty
			return true;
		}
	}

	/**
	 * Alters the text color of the event types shown in the spinner dropdown to
	 * match those across the app
	 * 
	 * @author Ian Kavanagh
	 */
	private static class EventTypeAdapter extends ArrayAdapter<CharSequence> {

		private final Context mContext;

		/**
		 * The resource indicating what views to inflate to display the content
		 * of this array adapter in a drop down widget.
		 */
		private int mDropdownResource;

		public EventTypeAdapter(Context context, int textViewResourceId,
				CharSequence[] objects, int dropdownResourceId) {
			super(context, textViewResourceId, objects);
			setDropDownViewResource(dropdownResourceId);
			mContext = context;
		}

		public static EventTypeAdapter createFromResource(Context context,
				int textArrayResId, int textViewResId, int dropdownResourceId) {
			CharSequence[] strings = context.getResources().getTextArray(
					textArrayResId);
			return new EventTypeAdapter(context, textViewResId, strings,
					dropdownResourceId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			TextView text;

			if (convertView == null) {
				view = LayoutInflater.from(mContext).inflate(
						this.mDropdownResource, parent, false);
			} else {
				view = convertView;
			}

			text = (TextView) view;

			CharSequence item = getItem(position);
			text.setText(item);

			Resources res = mContext.getResources();

			// Colour item depending on event type
			if (item.equals(res.getString(R.string.lecture))) {
				text.setTextColor(res.getColor(R.color.lecture));
			} else if (item.equals(res.getString(R.string.practical))) {
				text.setTextColor(res.getColor(R.color.practical));
			} else if (item.equals(res.getString(R.string.tutorial))) {
				text.setTextColor(res.getColor(R.color.tutorial));
			} else if (item.equals(res.getString(R.string.seminar))) {
				text.setTextColor(res.getColor(R.color.seminar));
			} else {
				text.setTextColor(res
						.getColor(android.R.color.primary_text_light));
			}

			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

			return view;
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			return getView(position, convertView, parent);
		}

		@Override
		public void setDropDownViewResource(int resource) {
			this.mDropdownResource = resource;
		}

	}

	/**
	 * Custom implementation used to return correct value from convertToString
	 * instead of information about the cursor object
	 */
	private class AutoCompleteTextViewCursorAdapter extends SimpleCursorAdapter {

		public AutoCompleteTextViewCursorAdapter(Context context, int layout,
				Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(1);
		}

	}

	/**
	 * Interface to define strings for use with AutoCompleteTextView data loads
	 */
	private interface Query {

		static interface Module {
			static final String[] PROJECTION = { Timetable.Events._ID,
					Timetable.Events.COLUMN_NAME_MODULE };
			static final String SELECTION = "(("
					+ Timetable.Events.COLUMN_NAME_USER_DELETED
					+ " = '0') AND (" + Timetable.Events.COLUMN_NAME_MODULE
					+ " LIKE ?))";
			static final String SORT_ORDER = Timetable.Events.COLUMN_NAME_MODULE
					+ " ASC";
		}

		static interface Lecturer {
			static final String[] PROJECTION = { Timetable.Events._ID,
					Timetable.Events.COLUMN_NAME_LECTURER };
			static final String SELECTION = "(("
					+ Timetable.Events.COLUMN_NAME_USER_DELETED
					+ " = '0') AND (" + Timetable.Events.COLUMN_NAME_LECTURER
					+ " LIKE ?))";
			static final String SORT_ORDER = Timetable.Events.COLUMN_NAME_LECTURER
					+ " ASC";
		}

	}

	private class LoaderCallbacks implements
			LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			// Set up a cursor loader for the specific view
			switch (id) {
			case MODULE:
				return new CursorLoader(getApplicationContext(),
						Timetable.Events.CONTENT_URI, Query.Module.PROJECTION,
						Query.Module.SELECTION,
						args.getStringArray(SELECTION_ARGS),
						Query.Module.SORT_ORDER);
			case LECTURER:
				return new CursorLoader(getApplicationContext(),
						Timetable.Events.CONTENT_URI,
						Query.Lecturer.PROJECTION, Query.Lecturer.SELECTION,
						args.getStringArray(SELECTION_ARGS),
						Query.Lecturer.SORT_ORDER);
			default:
				throw new IllegalArgumentException(
						"Only 2 types of loader can exist, module and lecturer");
			}
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			// Swap old cursor out with new cursor
			switch (loader.getId()) {
			case MODULE:
				moduleAdapter.swapCursor(data);
				return;
			case LECTURER:
				lecturerAdapter.swapCursor(data);
				return;
			default:
				throw new IllegalArgumentException(
						"Only 2 types of loader can exist, module and lecturer");
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// Swap old cursor out with new cursor
			switch (loader.getId()) {
			case MODULE:
				moduleAdapter.swapCursor(null);
				return;
			case LECTURER:
				lecturerAdapter.swapCursor(null);
				return;
			default:
				throw new IllegalArgumentException(
						"Only 2 types of loader can exist, module and lecturer");
			}
		}

	}

	private class SaveEvent extends AsyncTask<Bundle, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Bundle... params) {
			ContentProviderClient provider = getContentResolver()
					.acquireContentProviderClient(Timetable.Events.CONTENT_URI);

			Bundle data = params[0];

			Calendar tempStart = TimetableUtils.switchTimeZone(
					(Calendar) data.getSerializable(ARG_ITEM_START),
					TimetableUtils.TIMEZONE_UTC);

			Calendar tempEnd = TimetableUtils.switchTimeZone(
					(Calendar) data.getSerializable(ARG_ITEM_END),
					TimetableUtils.TIMEZONE_UTC);

			boolean update = false;
			if (data.containsKey(ARG_ITEM_ID))
				update = true;

			ContentValues values = TimetableParser.buildContentValues(
					data.getString(ARG_ITEM_EVENT_TYPE),
					data.getString(ARG_ITEM_MODULE),
					data.getString(ARG_ITEM_LECTURER),
					data.getString(ARG_ITEM_LOCATION),
					tempStart.getTimeInMillis(), tempEnd.getTimeInMillis(),
					data.getString(ARG_ITEM_NOTES), 0, 0, null, null, null,
					true, update);

			if (update)
				return TimetableParser.updateEvent(provider, values,
						data.getLong(ARG_ITEM_ID));
			else {
				id = TimetableParser.insertEvent(provider, values);
				return id != -1;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				getApplicationContext().getContentResolver().notifyChange(
						Timetable.Events.CONTENT_URI, null);
			else
				// TODO: implement some form of error handling
				Toast.makeText(getApplicationContext(),
						getString(R.string.message_event_saved_failure),
						Toast.LENGTH_SHORT).show();
		}

	}

}
