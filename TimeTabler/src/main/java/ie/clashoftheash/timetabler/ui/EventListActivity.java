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
import ie.clashoftheash.timetabler.utils.LoadEvent;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * An activity representing a list of Events. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link EventDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link EventListFragment} and the item details (if present) is a
 * {@link EventDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link EventListFragment.ItemSelectedListener} interface to listen for item
 * selections.
 */
public class EventListActivity extends Activity implements
		EventListFragment.ItemSelectedListener, LoadEvent.EventLoadListener {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_event_list);

		if (findViewById(R.id.event_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.

			((EventListFragment) getFragmentManager().findFragmentById(
					R.id.event_list)).setActivateOnItemClick(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		AccountUtils.checkAccountExists(this);
	}

	/**
	 * Callback method from {@link EventListFragment.ItemSelectedListener}
	 * indicating that the item with the given ID was selected.
	 */
	@Override
	public void onItemSelected(long id) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putLong(EventDetailFragment.ARG_EVENT_ID, id);
			Fragment fragment = new EventDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
					.replace(R.id.event_detail_container, fragment).commit();
		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, EventDetailActivity.class);
			detailIntent.putExtra(EventDetailFragment.ARG_EVENT_ID, id);
			startActivity(detailIntent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_event, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_create_event:
			startActivity(new Intent(this, CreateEventActivity.class));
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void OnEventLoadFinished(Cursor c) {
		Fragment fragment = getFragmentManager().findFragmentById(
				R.id.event_detail_container);
		if (fragment.isAdded())
			((EventDetailFragment) fragment).eventLoaded(c);
		else if (c != null)
			c.close();
	}
}
