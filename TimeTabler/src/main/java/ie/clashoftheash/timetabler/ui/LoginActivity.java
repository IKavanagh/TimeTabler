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
import ie.clashoftheash.timetabler.ui.dialogs.AccountExistsFragment;
import ie.clashoftheash.timetabler.ui.dialogs.NetworkErrorFragment;
import ie.clashoftheash.timetabler.utils.NetworkUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.acra.ACRA;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;

/**
 * Activity which displays a login screen to the user
 */
public class LoginActivity extends AccountAuthenticatorActivity {

	private AccountUtils mAccounts;
	private NetworkUtils network;

	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private AuthenticationTask mAuthTask = null;

	/**
	 * Keep track of progress dialog so we can cancel it
	 */
	private ProgressDialog mProgressDialog = null;

	// Values for programme code and year at the time of the authenticator
	// attempt.
	private String programmeCode;
	private String year;

	// UI references.
	private AutoCompleteTextView viewProgrammeCode;
	private AutoCompleteTextView viewYear;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAccounts = new AccountUtils(this);
		network = new NetworkUtils(this);

		setContentView(R.layout.activity_login);

		findViewById(R.id.create_account).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						beginAuthenticaton();
					}
				});

		// Set data and sync preference settings first time app is opened
		PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);

		// Set up AutoCompleteTextView
		viewProgrammeCode = (AutoCompleteTextView) findViewById(R.id.programme_code);
		viewYear = (AutoCompleteTextView) findViewById(R.id.year);

		Resources res = getResources();
		String[] programmeValues = res.getStringArray(R.array.programme_codes);
		String[] yearValues = res.getStringArray(R.array.years);

		// Set programme codes
		ArrayAdapter<String> programmeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, programmeValues);
		viewProgrammeCode.setAdapter(programmeAdapter);

		// Set year
		ArrayAdapter<String> yearAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, yearValues);
		viewYear.setAdapter(yearAdapter);

		// If done is clicked performClick on Create account button
		viewYear.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					findViewById(R.id.create_account).performClick();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();

		// If account exists, alert user
		if (mAccounts.accountExists())
			AppMsg.makeText(this, getString(R.string.account_already_exists),
					AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();
	}

	@Override
	public void onBackPressed() {
		// If user does not set up account, application is hidden and user is
		// sent back to where they entered the app
		if (!mAccounts.accountExists())
			moveTaskToBack(true);
		else
			super.onBackPressed();
	}

	/**
	 * Begins authentication by verifying data entered, if data entered is wrong
	 * errors are presented to the user and no authentication attempt is made
	 */
	void beginAuthenticaton() {
		if (mAuthTask != null)
			return; // Already attempting authentication

		// Reset errors
		viewProgrammeCode.setError(null);
		viewYear.setError(null);

		// Store values at the time of the login attempt
		programmeCode = viewProgrammeCode.getText().toString();
		year = viewYear.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid year
		if (TextUtils.isEmpty(year)) {
			viewYear.setError(getString(R.string.error_field_required));
			focusView = viewYear;
			cancel = true;
		} else if (!mAccounts.verifyYear(year)) {
			viewYear.setError(getString(R.string.error_invalid_year));
			focusView = viewYear;
			cancel = true;
		}

		// Check for a valid programme code
		if (TextUtils.isEmpty(programmeCode)) {
			viewProgrammeCode
					.setError(getString(R.string.error_field_required));
			focusView = viewProgrammeCode;
			cancel = true;
		} else if (!mAccounts.verifyProgrammeCode(programmeCode)) {
			viewProgrammeCode
					.setError(getString(R.string.error_invalid_programme_code));
			focusView = viewProgrammeCode;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first form
			// field with an error
			focusView.requestFocus();
			return;
		}

		if (mAccounts.accountExists()) {
			AccountExistsFragment fragment = new AccountExistsFragment();

			Bundle args = new Bundle();
			args.putString(AccountExistsFragment.ARG_PROGRAMME_CODE, programmeCode);
			args.putString(AccountExistsFragment.ARG_YEAR, year);

			fragment.setArguments(args);
			fragment.show(getFragmentManager(), "AccountExistsFragment");
			return;
		}

		if (!network.connectionAvailable()) {
			NetworkErrorFragment.handleNoConnection().show(
					getFragmentManager(), "NetworkErrorFragment");
			return;
		}

		startAuthentication();
	}

	/**
	 * Shows a progress dialog to the user and starts the authentication task
	 */
	private void startAuthentication() {
		// Show a progress dialog
		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog
					.setMessage(getString(R.string.progress_authenticating));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressNumberFormat(null);
			mProgressDialog.setProgressPercentFormat(null);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface dialog) {
					if (mAuthTask != null) { // Cancel authentication task
						mAuthTask.cancel(true);
						mAuthTask = null;
					}
				}

			});
		}
		mProgressDialog.show();

		mAuthTask = new AuthenticationTask();
		mAuthTask.execute(programmeCode, year);
	}

	private void onAuthenticationComplete(boolean result) {
		if (mAuthTask != null) {
			mAuthTask.cancel(true);
			mAuthTask = null;
		}

		mProgressDialog.dismiss();

		if (result) {
			if (mAccounts.addAccount(programmeCode, year)) {

				final Intent intent = new Intent();
				intent.putExtra(AccountManager.KEY_ACCOUNT_NAME,
						getString(R.string.account_name));
				intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,
						getString(R.string.account_type));
				setAccountAuthenticatorResult(intent.getExtras());
				setResult(RESULT_OK, intent);

				finish();
				return;
			}
		}

		AppMsg.makeText(this, getString(R.string.error_authenticating),
				AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();
	}

	/**
	 * Used to verify the combination of year and programme code entered by the
	 * user return a valid timetable
	 * 
	 * Takes the programme code and year entered by the user as arguments
	 * 
	 * @author Ian Kavanagh
	 */
	private class AuthenticationTask extends AsyncTask<String, Void, Boolean> {
		private static final String TAG = "AuthenticationTask";

		@Override
		protected Boolean doInBackground(String... params) {

			HttpURLConnection conn = null;

			try {
				URL url = network.buildTimetableUrl(params[0], params[1], 1);
				conn = network.openConnection(url);

				if (network.connectionRedirected(url, conn)) {
					NetworkErrorFragment.handleRedirect().show(
							getFragmentManager(), "");

					return false;
				}

				return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Invalid URL passed to openConnection", e);
				ACRA.getErrorReporter().handleSilentException(e);
				return false;
			} catch (IOException e) {
				Log.e(TAG,
						"IOException occured building url or opening connection",
						e);
				ACRA.getErrorReporter().handleSilentException(e);
				return false;
			} finally {
				if (conn != null)
					conn.disconnect();
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			onAuthenticationComplete(success);
		}

		@Override
		protected void onCancelled() {
			mProgressDialog.dismiss();
			if (mAuthTask != null) {
				mAuthTask.cancel(true);
				mAuthTask = null;
			}
		}

	}

}
