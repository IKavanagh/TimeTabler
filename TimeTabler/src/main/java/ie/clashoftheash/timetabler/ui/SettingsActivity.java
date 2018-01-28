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
import ie.clashoftheash.timetabler.provider.TimetableUtils;
import ie.clashoftheash.timetabler.ui.dialogs.RemoveAccountFragment;

import java.util.List;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener,
		RemoveAccountFragment.OnAccountRemovalListener {

	private SharedPreferences prefs;

	private static ListPreference syncFreqPreference;

	private static String prefKeyProgrammeCode, prefKeyYear, prefKeyFreq;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupActionBar();

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		prefKeyProgrammeCode = getString(R.string.pref_key_programme_code);
		prefKeyYear = getString(R.string.pref_key_year);
		prefKeyFreq = getString(R.string.pref_key_sync_frequency);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AccountUtils.checkAccountExists(this);

		prefs.registerOnSharedPreferenceChangeListener(this);

		// Check if sync setting was changed outside of app
		new VerifySyncPreference().execute(getSyncFreqPref());
	}

	@Override
	protected void onPause() {
		super.onPause();

		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			NavUtils.navigateUpTo(this, new Intent(this,
					EventListActivity.class));
			return true;
		case R.id.menu_remove_account:
			new RemoveAccountFragment().show(getFragmentManager(),
					"RemoveAccountFragment");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(prefKeyProgrammeCode) || key.equals(prefKeyYear)) {
			// Update account
			String programme = sharedPreferences.getString(
					prefKeyProgrammeCode, AccountUtils.getProgrammeCode(this));
			String year = sharedPreferences.getString(prefKeyYear,
					AccountUtils.getYear(this));

			new AccountUtils(this).updateAccount(programme, year);
		} else if (key.equals(prefKeyFreq)) {
			long pollFrequency = getSyncFreqPref();

			Account acc = AccountUtils.getAccount(this);
			String authority = Timetable.AUTHORITY;

			if (pollFrequency > 0) {
				ContentResolver.setSyncAutomatically(acc, authority, true);

				ContentResolver.addPeriodicSync(acc, authority, new Bundle(),
						pollFrequency);
			} else {
				ContentResolver.setSyncAutomatically(acc, authority, false);

				ContentResolver
						.removePeriodicSync(acc, authority, new Bundle());
			}
		}
	}

	@Override
	public void onAccountRemoval(boolean result) {
		if (result) {
			TimetableUtils.deleteDatabase(this);

			// Removing preferences for programme code and year
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.remove(getString(R.string.pref_key_programme_code))
					.remove(getString(R.string.pref_key_year)).apply();
			RemoveAccountFragment.accountRemoved(this);
		} else
			RemoveAccountFragment.accountNotRemoved(this);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	/**
	 * Helper method to return the users sync frequency preference
	 * 
	 * @return how frequently the sync should be performed, in seconds
	 */
	private long getSyncFreqPref() {
		return Long.parseLong(prefs.getString(
				getString(R.string.pref_key_sync_frequency),
				getString(R.string.pref_default_sync_frequency)));
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (isXLargeTablet(this))
			return;

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new GeneralPreferenceFragment())
				.commit();
	}

	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if the device doesn't have newer APIs like
	 * {@link PreferenceFragment}, or the device doesn't have an extra-large
	 * screen. In these cases, a single-pane "simplified" settings UI should be
	 * shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {

			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);

			} else if (preference instanceof RingtonePreference) {
				// For ringtone preferences, look up the correct display value
				// using RingtoneManager.
				if (TextUtils.isEmpty(stringValue)) {
					// Empty values correspond to 'silent' (no ringtone).
					preference.setSummary(R.string.pref_ringtone_silent);

				} else {
					Ringtone ringtone = RingtoneManager.getRingtone(
							preference.getContext(), Uri.parse(stringValue));

					if (ringtone == null) {
						// Clear the summary if there was a lookup error.
						preference.setSummary(null);
					} else {
						// Set the summary to reflect the new ringtone display
						// name.
						String name = ringtone
								.getTitle(preference.getContext());
						preference.setSummary(name);
					}
				}

			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

	/**
	 * This fragment shows all preferences. It is used when the activity is show
	 * a single-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {

		/**
		 * Mandatory empty constructor for the fragment manager to instantiate
		 * the fragment (e.g. upon screen orientation changes).
		 */
		public GeneralPreferenceFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Add 'account' preferences, and a corresponding header
			addPreferencesFromResource(R.xml.pref_account);

			// Add 'data and sync' preferences, and a corresponding header
			PreferenceCategory fakeHeader = new PreferenceCategory(
					getActivity());
			fakeHeader.setTitle(R.string.pref_header_data_sync);
			getPreferenceScreen().addPreference(fakeHeader);
			addPreferencesFromResource(R.xml.pref_data_sync);

			// Add 'about and support' preferences and a corresponding header
			fakeHeader = new PreferenceCategory(getActivity());
			fakeHeader.setTitle(R.string.pref_header_about_support);
			getPreferenceScreen().addPreference(fakeHeader);
			addPreferencesFromResource(R.xml.pref_about_support);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.

			syncFreqPreference = (ListPreference) findPreference(prefKeyFreq);

			bindPreferenceSummaryToValue(findPreference(prefKeyProgrammeCode));
			bindPreferenceSummaryToValue(findPreference(prefKeyYear));
			bindPreferenceSummaryToValue(syncFreqPreference);
		}

	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AccountPreferenceFragment extends PreferenceFragment {

		/**
		 * Mandatory empty constructor for the fragment manager to instantiate
		 * the fragment (e.g. upon screen orientation changes).
		 */
		public AccountPreferenceFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_account);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference(prefKeyProgrammeCode));
			bindPreferenceSummaryToValue(findPreference(prefKeyYear));
		}
	}

	/**
	 * This fragment shows data and sync preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DataSyncPreferenceFragment extends PreferenceFragment {

		/**
		 * Mandatory empty constructor for the fragment manager to instantiate
		 * the fragment (e.g. upon screen orientation changes).
		 */
		public DataSyncPreferenceFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_data_sync);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			syncFreqPreference = (ListPreference) findPreference(prefKeyFreq);

			bindPreferenceSummaryToValue(syncFreqPreference);
		}
	}

	/**
	 * This fragment shows about and support preferences only. It is used when
	 * the activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AboutSupportPreferenceFragment extends
			PreferenceFragment {

		/**
		 * Mandatory empty constructor for the fragment manager to instantiate
		 * the fragment (e.g. upon screen orientation changes).
		 */
		public AboutSupportPreferenceFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_about_support);
		}
	}

	/**
	 * Verifies if the current sync preference stored in the
	 * {@link android.content.SharedPreferences} is the same as that set in the
	 * systems global settings and configures the settings to be valid and match
	 * the systems settings
	 * 
	 * Takes as a parameter the current sync frequency preference
	 * 
	 * @author Ian Kavanagh
	 */
	private class VerifySyncPreference extends AsyncTask<Long, Void, String> {

		@Override
		protected String doInBackground(Long... params) {
			long pollFrequency = params[0];

			Context context = getApplicationContext();

			Account acc = AccountUtils.getAccount(context);
			String authority = Timetable.AUTHORITY;

			boolean setToSync = ContentResolver.getSyncAutomatically(acc,
					authority);

			String value = null;

			if (setToSync != (pollFrequency > 0)) {

				SharedPreferences.Editor editor = PreferenceManager
						.getDefaultSharedPreferences(context).edit();

				if (setToSync) {
					value = getString(R.string.pref_default_sync_frequency);

					if (acc != null)
						ContentResolver.addPeriodicSync(acc, authority,
								new Bundle(), Long.parseLong(value));
					editor.putString(prefKeyFreq, value);
				} else {
					value = "-1"; // Don't sync
					if (acc != null)
						ContentResolver.removePeriodicSync(acc, authority,
								new Bundle());

					editor.putString(prefKeyFreq, value);
				}
				editor.apply();
			}

			return value;
		}

		@Override
		protected void onPostExecute(String result) {
			// Null occurs if UI has not been created yet
			if (syncFreqPreference != null && result != null) {
				syncFreqPreference.setValue(result);
				bindPreferenceSummaryToValue(syncFreqPreference);
			}
		}

	}
}
