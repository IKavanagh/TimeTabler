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
import ie.clashoftheash.timetabler.provider.Timetable;
import ie.clashoftheash.timetabler.provider.TimetableProvider;
import ie.clashoftheash.timetabler.provider.TimetableUtils;
import ie.clashoftheash.timetabler.ui.dialogs.DeleteEventFragment;
import ie.clashoftheash.timetabler.utils.LoadEvent;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;

/**
 * A fragment representing a single Event detail screen. This fragment is either
 * contained in a {@link EventListActivity} in two-pane mode (on tablets) or a
 * {@link EventDetailActivity} on handsets.
 */
public class EventDetailFragment extends Fragment {

	/**
	 * The fragment argument representing the event ID that this fragment
	 * represents.
	 */
	public static final String ARG_EVENT_ID = "event_id";

	private Cursor cursor;

	// Set to the events id after UI populated
	private long id = -1;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EventDetailFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		activity.getContentResolver().registerContentObserver(
				Timetable.Events.CONTENT_URI, true, mObserver);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		if (getArguments().containsKey(ARG_EVENT_ID))
			// Load the event content specified by the fragment arguments
			new LoadEvent(getActivity()).execute(getArguments().getLong(
					ARG_EVENT_ID));
		else
			displayEventErrorMessage();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_event_detail,
				container, false);

		if (cursor != null)
			populateView();

		return rootView;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		getActivity().getContentResolver().unregisterContentObserver(mObserver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_event_detail, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_edit_event:
			Intent intent = new Intent(getActivity(), CreateEventActivity.class);
			intent.putExtra(CreateEventActivity.ARG_ITEM_ID, id);
			startActivity(intent);
			return true;
		case R.id.menu_delete_event:
			if (id != -1) {
				View rootView = getView();

				String time = ((TextView) rootView.findViewById(R.id.time))
						.getText().toString();

				DeleteEventFragment.newInstance(
						id,
						((TextView) rootView.findViewById(R.id.module))
								.getText().toString(),
						time.substring(0, time.indexOf(" ")),
						((TextView) rootView.findViewById(R.id.date)).getText()
								.toString()).show(getFragmentManager(), null);
			} else
				// TODO: prevent this from occurring however unlikely
				displayEventErrorMessage();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private final ContentObserver mObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			if (getActivity() == null)
				return;

			new LoadEvent(getActivity()).execute(id);
		}
	};

	/**
	 * Called from parent activity when event has been loaded to populate view
	 * 
	 * @param c
	 *            Cursor holding data to be used for populating view
	 */
	void eventLoaded(Cursor c) {
		cursor = c;

		// Check if UI has been inflated before attempting to populate
		try {
			if (getView().findViewById(R.id.event_detail) != null)
				populateView();
		} catch (NullPointerException e) {
			// onCreateView not called yet, UI will be populated in its call,
			// can ignore exception
		}
	}

	private void populateView() {
		// Make sure cursor is on first element
		try {
			if (cursor.moveToFirst()) {
				// Everything ok add data to UI
				View rootView = getView();

				String eventType = cursor
						.getString(TimetableProvider.READ_EVENT_EVENT_TYPE_INDEX);

				View headerView = rootView.findViewById(R.id.event_header);

				TextView eventTypeView = (TextView) rootView
						.findViewById(R.id.event_type);
				TextView weekView = (TextView) rootView.findViewById(R.id.week);
				TextView dateView = (TextView) rootView.findViewById(R.id.date);

				Resources res = getResources();
				// Colour item depending on event type
				if (eventType.equals(getString(R.string.lecture))) {
					headerView
							.setBackgroundColor(res.getColor(R.color.lecture));
				} else if (eventType.equals(getString(R.string.practical))) {
					headerView.setBackgroundColor(res
							.getColor(R.color.practical));
				} else if (eventType.equals(getString(R.string.tutorial))) {
					headerView.setBackgroundColor(res
							.getColor(R.color.tutorial));
				} else if (eventType.equals(getString(R.string.seminar))) {
					headerView
							.setBackgroundColor(res.getColor(R.color.seminar));
				} else {
					eventTypeView.setTextColor(res
							.getColor(android.R.color.primary_text_light));
					weekView.setTextColor(res
							.getColor(android.R.color.primary_text_light));
					dateView.setTextColor(res
							.getColor(android.R.color.primary_text_light));
				}

				eventTypeView.setText(eventType);

				((TextView) rootView.findViewById(R.id.module)).setText(cursor
						.getString(TimetableProvider.READ_EVENT_MODULE_INDEX));
				((TextView) rootView.findViewById(R.id.location))
						.setText(cursor
								.getString(TimetableProvider.READ_EVENT_LOCATION_INDEX));
				((TextView) rootView.findViewById(R.id.lecturer))
						.setText(cursor
								.getString(TimetableProvider.READ_EVENT_LECTURER_INDEX));
				((TextView) rootView.findViewById(R.id.notes)).setText(cursor
						.getString(TimetableProvider.READ_EVENT_NOTES_INDEX));

				Calendar start = Calendar
						.getInstance(TimetableUtils.TIMEZONE_UTC);
				start.setTimeInMillis(cursor
						.getLong(TimetableProvider.READ_EVENT_START_INDEX));

				start = TimetableUtils.switchTimeZone(start,
						TimeZone.getDefault());

				Calendar end = Calendar
						.getInstance(TimetableUtils.TIMEZONE_UTC);
				end.setTimeInMillis(cursor
						.getLong(TimetableProvider.READ_EVENT_END_INDEX));

				end = TimetableUtils.switchTimeZone(end, TimeZone.getDefault());

				DateFormat timeFormat = android.text.format.DateFormat
						.getTimeFormat(getActivity());

				DateFormat longDateFormat = android.text.format.DateFormat
						.getLongDateFormat(getActivity());

				dateView.setText(longDateFormat.format(start.getTime()));
				weekView.setText(getString(R.string.week) + " "
						+ TimetableUtils.getWeekNumber(start.getTimeInMillis()));

				((TextView) rootView.findViewById(R.id.day)).setText(DateUtils
						.formatDateTime(getActivity(), start.getTimeInMillis(),
								DateUtils.FORMAT_SHOW_WEEKDAY));

				((TextView) rootView.findViewById(R.id.time))
						.setText(timeFormat.format(start.getTime()) + " - "
								+ timeFormat.format(end.getTime()));

				// Setting id after UI filled
				id = cursor
						.getLong(cursor.getColumnIndex(Timetable.Events._ID));

				cursor.close();
			} else
				displayEventErrorMessage();
			// TODO: provide recovery option
		} catch (NullPointerException e) {
			displayEventErrorMessage();
			// TODO: provide recovery option
		}
	}

	/**
	 * Helper method to display an error message if we couldn't find the event
	 */
	private void displayEventErrorMessage() {
		AppMsg.makeText(getActivity(), R.string.message_no_event_found,
				AppMsg.STYLE_ALERT).show();
	}
}
