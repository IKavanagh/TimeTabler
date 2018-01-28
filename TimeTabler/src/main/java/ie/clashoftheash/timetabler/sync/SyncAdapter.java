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

package ie.clashoftheash.timetabler.sync;

import ie.clashoftheash.timetabler.provider.Timetable;
import ie.clashoftheash.timetabler.provider.TimetableParser;
import ie.clashoftheash.timetabler.utils.NetworkUtils;

import java.io.IOException;
import java.net.MalformedURLException;

import org.acra.ACRA;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final String TAG = "SyncAdapter";

	private final TimetableParser parser;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);

		parser = new TimetableParser(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		if (!NetworkUtils.canPerformSync(getContext()))
			return;

		int[] changes = new int[3];

		try {
			changes = parser.beginParsing(provider);
		} catch (MalformedURLException e) {
			Log.e(TAG, "MalformedURLException", e);
			ACRA.getErrorReporter().handleSilentException(e);
			syncResult.stats.numIoExceptions++;
		} catch (IOException e) {
			Log.e(TAG, "IOException", e);
			ACRA.getErrorReporter().handleSilentException(e);
			syncResult.stats.numIoExceptions++;
		} catch (Exception e) {
			Log.e(TAG, "Exception", e);
			ACRA.getErrorReporter().handleSilentException(e);
			syncResult.stats.numParseExceptions++;
		}

		syncResult.stats.numInserts = changes[0];
		syncResult.stats.numUpdates = changes[1];
		syncResult.stats.numDeletes = changes[2];

		getContext().getContentResolver().notifyChange(
				Timetable.Events.CONTENT_URI, null);
	}

}
