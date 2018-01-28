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

package ie.clashoftheash.timetabler.authenticator;

import ie.clashoftheash.timetabler.R;
import ie.clashoftheash.timetabler.provider.Timetable;
import ie.clashoftheash.timetabler.provider.TimetableUtils;

import java.util.Arrays;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class AccountUtils {

	// Keys for additional userdata for Account
	private static final String KEY_PROGRAMME_CODE = "programme_code";
	private static final String KEY_YEAR = "year";

	private final Context mContext;
	private final Resources res;

	public AccountUtils(Context context) {
		mContext = context;
		res = mContext.getResources();
	}

	/**
	 * Checks whether the given programme code is valid
	 * 
	 * @param programmeCode
	 *            programme code to check validity for
	 * @return true if programmeCode is valid
	 */
	public boolean verifyProgrammeCode(String programmeCode) {
		String[] validSolns = res.getStringArray(R.array.programme_codes);

		Arrays.sort(validSolns);
		return Arrays.binarySearch(validSolns, programmeCode) > 0;
	}

	/**
	 * Checks whether the given year is valid
	 * 
	 * @param year
	 *            year to check validity for
	 * @return true if year is valid
	 */
	public boolean verifyYear(String year) {
		String[] validSolns = res.getStringArray(R.array.years);

		Arrays.sort(validSolns);
		return Arrays.binarySearch(validSolns, year) > 0;
	}

	/**
	 * Creates an account
	 * 
	 * @param programmeCode
	 *            users programme code
	 * @param year
	 *            users year
	 * @return true if account created successfully
	 */
	public boolean addAccount(String programmeCode, String year) {
		Account account = new Account(res.getString(R.string.account_name),
				res.getString(R.string.account_type));
		Bundle userdata = new Bundle();
		userdata.putString(KEY_PROGRAMME_CODE, programmeCode);
		userdata.putString(KEY_YEAR, year);

		boolean created = AccountManager.get(mContext).addAccountExplicitly(
				account, null, userdata);

		if (created) {
			// set preferences
			updatePreferences(programmeCode, year);

			String authority = Timetable.AUTHORITY;

			// turn on sync
			ContentResolver.setIsSyncable(account, authority, 1);
			ContentResolver.setSyncAutomatically(account, authority, true);

			long pollFrequency = Long
					.parseLong(PreferenceManager
							.getDefaultSharedPreferences(mContext)
							.getString(
									mContext.getString(R.string.pref_key_sync_frequency),
									mContext.getString(R.string.pref_default_sync_frequency)));
			if (pollFrequency > 0)
				ContentResolver.addPeriodicSync(account, authority,
						new Bundle(), pollFrequency);
			else
				ContentResolver.removePeriodicSync(account, authority,
						new Bundle());

			ContentResolver.requestSync(account, authority, new Bundle());
		}

		return created;
	}

	/**
	 * Getter to return the users programme code
	 * 
	 * @return users programme code
	 */
	public static String getProgrammeCode(Context context) {
		return AccountManager.get(context).getUserData(getAccount(context),
				KEY_PROGRAMME_CODE);
	}

	/**
	 * Getter to return the users year
	 * 
	 * @return users year
	 */
	public static String getYear(Context context) {
		return AccountManager.get(context).getUserData(getAccount(context),
				KEY_YEAR);
	}

	/**
	 * Returns array of accounts matching account type
	 * 
	 * @return array of Accounts
	 */
	private static Account[] getAccounts(Context context) {
		return AccountManager.get(context).getAccountsByType(
				context.getString(R.string.account_type));
	}

	/**
	 * Retrieves users account
	 * 
	 * @return Account object for user account
	 */
	public static Account getAccount(Context context) {
		Account[] accounts = getAccounts(context);
		if (accounts.length < 1)
			return null;
		return accounts[0];
	}

	/**
	 * To test if an account already exists
	 * 
	 * @return true if an account exists
	 */
	public boolean accountExists() {
		return (getAccounts(mContext).length > 0);
	}

	/**
	 * Helper method to check account exists, if not a request to add one is
	 * made
	 */
	public static void checkAccountExists(Activity activity) {
		AccountUtils accUtils = new AccountUtils(activity);

		if (!accUtils.accountExists()) {
			AccountManager.get(activity).addAccount(
					activity.getString(R.string.account_type), null, null,
					null, activity, null, null);
		}
	}

	/**
	 * Updates account user data with those supplied
	 * 
	 * @param programmeCode
	 *            new programme code to add to account
	 * @param year
	 *            new year to add to account
	 */
	public void updateAccount(String programmeCode, String year) {
		// In case sync is active, cancel
		ContentResolver.cancelSync(null, Timetable.AUTHORITY);

		// Remove data in database for fresh start with new account
		TimetableUtils.deleteDatabase(mContext);

		Account account = getAccount(mContext);

		AccountManager mgr = AccountManager.get(mContext);
		mgr.setUserData(account, KEY_PROGRAMME_CODE, programmeCode);
		mgr.setUserData(account, KEY_YEAR, year);

		// Update SharedPreferences
		updatePreferences(programmeCode, year);

		// After account is updated, request sync
		ContentResolver.requestSync(account, Timetable.AUTHORITY, new Bundle());
	}

	/**
	 * Updates values for programme code and year in preferences so they are
	 * displayed properly in settings screen
	 * 
	 * @param programmeCode
	 *            New programme code
	 * @param year
	 *            New year
	 */
	private void updatePreferences(String programmeCode, String year) {
		PreferenceManager
				.getDefaultSharedPreferences(mContext)
				.edit()
				.putString(
						mContext.getString(R.string.pref_key_programme_code),
						programmeCode)
				.putString(mContext.getString(R.string.pref_key_year), year)
				.apply();
	}

}