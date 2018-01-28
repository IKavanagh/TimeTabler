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

package ie.clashoftheash.timetabler.utils;

import ie.clashoftheash.timetabler.R;
import ie.clashoftheash.timetabler.provider.Timetable;
import ie.clashoftheash.timetabler.provider.TimetableProvider;
import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;

import com.devspark.appmsg.AppMsg;

public class LoadEvent extends AsyncTask<Long, Void, Cursor> {

	/**
	 * The callback used to indicate the event has been loaded from the
	 * database.
	 */
	public interface EventLoadListener {
		public void OnEventLoadFinished(Cursor c);
	}

	private final Activity mActivity;

	public LoadEvent(Activity activity) {
		mActivity = activity;
	}

	@Override
	protected Cursor doInBackground(Long... params) {
		// Activities calling this AsyncTask extension must implement its
		// callbacks
		if (!(mActivity instanceof EventLoadListener)) {
			throw new IllegalStateException(
					"Activity must implement classes callbacks.");
		}

		Uri uri = ContentUris.withAppendedId(Timetable.Events.CONTENT_URI,
				params[0]);

		return mActivity.getContentResolver().query(uri,
				TimetableProvider.READ_EVENT_PROJECTION, null, null, null);
	}

	@Override
	protected void onPostExecute(Cursor result) {
		if (result == null || result.getCount() != 1 || !result.moveToFirst()) {
			// Display message to user informing them no event could be found
			AppMsg.makeText(
					mActivity,
					mActivity.getString(R.string.message_error_cant_find_event),
					AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();
			return;
		}

		((EventLoadListener) mActivity).OnEventLoadFinished(result);
	}
}