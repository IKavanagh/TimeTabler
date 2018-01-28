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
import ie.clashoftheash.timetabler.ui.widget.AgendaAdapter;
import ie.clashoftheash.timetabler.ui.widget.SimpleSectionedListAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A list fragment representing a list of Events. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link EventDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link ItemSelectedListener}
 * interface.
 */
public class EventListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	/**
	 * Selection used to get events for Agenda
	 */
	private static final String SELECTION = "(("
			+ Timetable.Events.COLUMN_NAME_END + " BETWEEN ? AND ?) AND ("
			+ Timetable.Events.COLUMN_NAME_USER_DELETED + " = '0'))";

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * List of column names representing the data to bind to the UI
	 */
	private static final String[] from = {
			Timetable.Events.COLUMN_NAME_EVENT_TYPE,
			Timetable.Events.COLUMN_NAME_MODULE,
			Timetable.Events.COLUMN_NAME_LECTURER,
			Timetable.Events.COLUMN_NAME_LOCATION };

	/**
	 * The views that should display column in the "from" parameter for the list
	 * block
	 */
	private static final int[] toBlock = { R.id.event_type, R.id.module,
			R.id.lecturer, R.id.location };

	/**
	 * The views that should display column in the "from" parameter for the list
	 * header
	 */
	private static final int[] toHeader = { R.id.date, R.id.week, R.id.day };

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private ItemSelectedListener mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * Holds first id in cursor to set scroll position of listview when items
	 * being loaded into head of listview
	 */
	private long firstId = ListView.INVALID_ROW_ID;

	/**
	 * Holds position of first event today for easy scrolling to
	 */
	private int todaysPosition;

	/**
	 * Whether or not we should scroll to the first event after now
	 */
	private boolean mScrollToNow;

	/**
	 * Reference to the head and tail times between which events are loaded for
	 */
	private Calendar head, tail;

	/**
	 * Reference to AgendaAdapter, needed for changing cursor on load
	 */
	private AgendaAdapter mAgendaAdapter;

	/**
	 * Reference to fragments list
	 */
	private ListView lv;

	/**
	 * Reference to the ViewGroups's within the ListView's header and footer
	 */
	private ViewGroup headerView, footerView;

	/**
	 * Reference to the TextView's within the ListView's header and footer
	 */
	private TextView headerTextView, footerTextView;

	/**
	 * Number of events loaded in previous load
	 */
	private int eventCount;

	/**
	 * Flag to denote if we have events in the future
	 */
	private boolean futureEvents = true;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface ItemSelectedListener {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(long id);
	}

	/**
	 * A dummy implementation of the {@link ItemSelectedListener} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static final ItemSelectedListener sDummyCallbacks = new ItemSelectedListener() {
		@Override
		public void onItemSelected(long id) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public EventListFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof ItemSelectedListener)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (ItemSelectedListener) activity;

		activity.getContentResolver().registerContentObserver(
				Timetable.Events.CONTENT_URI, true, mObserver);

		activity.getActionBar().setTitle(R.string.agenda);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		head = Calendar.getInstance();
		head.add(Calendar.DAY_OF_YEAR, -14);

		// Set milliseconds to 0 to avoid errors with slightly different times
		head.set(Calendar.MILLISECOND, 0);

		tail = Calendar.getInstance();
		tail.add(Calendar.DAY_OF_YEAR, 14);

		// Set milliseconds to 0 to avoid errors with slightly different times
		tail.set(Calendar.MILLISECOND, 0);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup root = (ViewGroup) inflater.inflate(
				R.layout.fragment_list_with_empty_container, container, false);
		inflater.inflate(R.layout.empty_waiting_for_sync,
				(ViewGroup) root.findViewById(android.R.id.empty), true);

		lv = (ListView) root.findViewById(android.R.id.list);
		lv.setDividerHeight(0);

		// Add header and footer views to ListView
		headerView = (ViewGroup) inflater.inflate(
				R.layout.list_persistent_header_footer, lv, false);

		headerTextView = (TextView) headerView.findViewById(android.R.id.text1);
		headerTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				head.add(Calendar.DAY_OF_YEAR, -28);

				headerTextView.setVisibility(View.GONE);
				headerView.findViewById(android.R.id.progress).setVisibility(
						View.VISIBLE);

				// store first id of list to scroll to later only if events are
				// present
				if (eventCount != 0)
					firstId = mAgendaAdapter.getItemId(0);

				mScrollToNow = true;

				restartLoader();
			}
		});

		lv.addHeaderView(headerView, null, false);

		footerView = (ViewGroup) inflater.inflate(
				R.layout.list_persistent_header_footer, lv, false);

		footerTextView = (TextView) footerView.findViewById(android.R.id.text1);
		footerTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!futureEvents) // no events in future
					return;

				firstId = ListView.INVALID_ROW_ID;

				tail.add(Calendar.DAY_OF_YEAR, 28);

				footerTextView.setVisibility(View.GONE);
				footerView.findViewById(android.R.id.progress).setVisibility(
						View.VISIBLE);

				restartLoader();
			}
		});

		lv.addFooterView(footerView, null, false);

		return root;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		else
			mScrollToNow = true;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// The AgendaAdapter is wrapped in a SimpleSectionedListAdapter so that
		// we can show list headers separating out the different days
		// (Wednesday/Thursday/Friday).
		mAgendaAdapter = new AgendaAdapter(getActivity(),
				R.layout.list_item_agenda_block, from, toBlock);
		SimpleSectionedListAdapter mAdapter = new SimpleSectionedListAdapter(
				getActivity(), R.layout.list_item_agenda_header, toHeader,
				mAgendaAdapter);
		setListAdapter(mAdapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;

		getActivity().getContentResolver().unregisterContentObserver(mObserver);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(id);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_event_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_go_to_today:
			lv.setSelectionFromTop(todaysPosition, 0);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Calendar tempHead = TimetableUtils.switchTimeZone(head,
				TimetableUtils.TIMEZONE_UTC);
		Calendar tempTail = TimetableUtils.switchTimeZone(tail,
				TimetableUtils.TIMEZONE_UTC);

		String[] selectionArgs = { String.valueOf(tempHead.getTimeInMillis()),
				String.valueOf(tempTail.getTimeInMillis()) };

		return new CursorLoader(getActivity(), Timetable.Events.CONTENT_URI,
				TimetableProvider.READ_EVENT_PROJECTION, SELECTION,
				selectionArgs, Timetable.Events.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null)
			return;

		if (eventCount == 0)
			mScrollToNow = true;

		headerView.findViewById(android.R.id.progress).setVisibility(View.GONE);
		headerTextView.setVisibility(View.VISIBLE);
		headerTextView.setText(getString(R.string.agenda_header)
				+ " "
				+ DateUtils.formatDateTime(getActivity(),
						head.getTimeInMillis(), DateUtils.FORMAT_ABBREV_MONTH
								| DateUtils.FORMAT_NO_YEAR
								| DateUtils.FORMAT_SHOW_DATE));

		// TODO: have events remaining checked when footer load brings back the
		// same amount of events as last time
		if ((eventCount = cursor.getCount()) == 0) {
			if (futureEvents)
				new QueryEventsRemainingTask().execute();
			else
				lv.setVisibility(View.VISIBLE);
			return;
		}

		if (futureEvents) { // Check required in case of header load
			footerView.findViewById(android.R.id.progress).setVisibility(
					View.GONE);
			footerTextView.setVisibility(View.VISIBLE);
			footerTextView.setText(getString(R.string.agenda_footer)
					+ " "
					+ DateUtils.formatDateTime(getActivity(),
							tail.getTimeInMillis(),
							DateUtils.FORMAT_ABBREV_MONTH
									| DateUtils.FORMAT_NO_YEAR
									| DateUtils.FORMAT_SHOW_DATE));
		}

		// Build sections
		long currentTime = (Calendar.getInstance(TimetableUtils.TIMEZONE_UTC)
				.getTimeInMillis() / 1000) * 1000; // Remove millisecond error

		int firstNowPosition = ListView.INVALID_POSITION;

		List<SimpleSectionedListAdapter.Section> sections = new ArrayList<SimpleSectionedListAdapter.Section>();
		cursor.moveToFirst();

		long previousBlockStart = -1;
		long blockStart;
		while (!cursor.isAfterLast()) {
			Calendar start = Calendar.getInstance(TimetableUtils.TIMEZONE_UTC);
			start.setTimeInMillis(cursor
					.getLong(TimetableProvider.READ_EVENT_START_INDEX));

			blockStart = TimetableUtils.switchTimeZone(start,
					TimeZone.getDefault()).getTimeInMillis();

			long startMillis = start.getTimeInMillis();

			Calendar end = Calendar.getInstance(TimetableUtils.TIMEZONE_UTC);
			end.setTimeInMillis(cursor
					.getLong(TimetableProvider.READ_EVENT_END_INDEX));

			if (!TimetableUtils.isSameDay(previousBlockStart,
					start.getTimeInMillis())) {
				String day;

				if (TimetableUtils.isSameDay(currentTime, startMillis))
					day = getString(R.string.today) + ", ";
				else if (TimetableUtils.isDayBefore(currentTime, startMillis))
					day = getString(R.string.yesterday) + ", ";
				else if (TimetableUtils.isDayAfter(currentTime, startMillis))
					day = getString(R.string.tomorrow) + ", ";
				else
					day = "";

				String[] text = {
						DateUtils.formatDateTime(getActivity(), blockStart,
								DateUtils.FORMAT_SHOW_DATE
										| DateUtils.FORMAT_SHOW_YEAR),
						getString(R.string.week) + " "
								+ TimetableUtils.getWeekNumber(blockStart),
						day
								+ DateUtils.formatDateTime(getActivity(),
										blockStart,
										DateUtils.FORMAT_SHOW_WEEKDAY) };
				sections.add(new SimpleSectionedListAdapter.Section(cursor
						.getPosition(), text, startMillis < currentTime));
			}
			if (mScrollToNow && firstNowPosition == ListView.INVALID_POSITION
			// if we're currently in this block, or we're not in a block
			// and this block is in the future, then this is the scroll
			// position
					&& ((startMillis < currentTime && currentTime < end
							.getTimeInMillis()) || startMillis > currentTime)) {
				firstNowPosition = cursor.getPosition();
			}
			previousBlockStart = startMillis;
			cursor.moveToNext();
		}

		mAgendaAdapter.changeCursor(cursor);

		SimpleSectionedListAdapter.Section[] holder = new SimpleSectionedListAdapter.Section[sections
				.size()];
		((SimpleSectionedListAdapter) getListAdapter()).setSections(sections
				.toArray(holder));

		// Set scroll position
		if (firstId != ListView.INVALID_ROW_ID) {
			// loop through cursor to find position of id to scroll to
			cursor.moveToFirst();
			firstNowPosition = ListView.INVALID_POSITION;
			while (!cursor.isAfterLast()
					&& firstNowPosition == ListView.INVALID_POSITION) {
				if (firstId == cursor.getLong(0)) {
					firstNowPosition = cursor.getPosition();
				}
				cursor.moveToNext();
			}
			firstNowPosition = ((SimpleSectionedListAdapter) getListAdapter())
					.positionToSectionedPosition(firstNowPosition);
			lv.setSelectionFromTop(firstNowPosition, 0);
			firstId = ListView.INVALID_ROW_ID;
			mScrollToNow = false;
		} else if (mScrollToNow) {
			if (firstNowPosition != ListView.INVALID_POSITION) {
				firstNowPosition = ((SimpleSectionedListAdapter) getListAdapter())
						.positionToSectionedPosition(firstNowPosition);
				mScrollToNow = false;
			} else
				firstNowPosition = ((SimpleSectionedListAdapter) getListAdapter())
						.positionToSectionedPosition(cursor.getCount());

			lv.setSelectionFromTop(firstNowPosition, 0);
			todaysPosition = firstNowPosition;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched, when off allows selecting
	 * multiple events
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		lv.setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
				: ListView.CHOICE_MODE_MULTIPLE_MODAL);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			lv.setItemChecked(mActivatedPosition, false);
		} else {
			lv.setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	private final ContentObserver mObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			futureEvents = true;

			if (getActivity() == null)
				return;

			Loader<Cursor> loader = getLoaderManager().getLoader(0);
			if (loader != null)
				loader.forceLoad();
		}
	};

	/**
	 * Restarts the initial loader for this fragment
	 */
	private void restartLoader() {
		if (isAdded())
			getLoaderManager().restartLoader(0, null, this);
	}

	private class QueryEventsRemainingTask extends
			AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			// Gets start of next event converts it to a week and moves end
			// on to match 1 week ahead
			long nextEventTime = TimetableUtils.startOfNextEvent(getActivity(),
					tail.getTimeInMillis());

			if (nextEventTime == -1)
				return false;

			tail.add(
					Calendar.WEEK_OF_YEAR,
					1
							+ TimetableUtils.getWeekNumber(nextEventTime)
							- TimetableUtils.getWeekNumber(tail
									.getTimeInMillis()));

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) // events exist, attempt to load them
				restartLoader();
			else {
				getActivity().findViewById(android.R.id.empty).setVisibility(
						View.GONE); // hide empty view

				// show listview
				lv.setVisibility(View.VISIBLE);
				// change text on footer
				footerTextView.setText(R.string.empty_no_events_upcoming);
				footerView.findViewById(android.R.id.progress).setVisibility(
						View.GONE);
				futureEvents = false;
			}
		}

	}

}
