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

package ie.clashoftheash.timetabler.ui.dialogs;

import ie.clashoftheash.timetabler.R;
import ie.clashoftheash.timetabler.provider.Timetable;
import ie.clashoftheash.timetabler.provider.TimetableParser;
import ie.clashoftheash.timetabler.ui.EventDetailActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentProviderClient;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

import com.devspark.appmsg.AppMsg;

public class DeleteEventFragment extends DialogFragment {

	/**
	 * The fragment argument representing the event ID that this fragment
	 * represents.
	 */
	private static final String ARG_EVENT_ID = "event_id";

	/**
	 * The fragment argument representing the event module that this fragment
	 * represents.
	 */
	private static final String ARG_EVENT_MODULE = "event_module";

	/**
	 * The fragment argument representing the event date that this fragment
	 * represents.
	 */
	private static final String ARG_EVENT_DATE = "event_date";

	/**
	 * The fragment argument representing the event time that this fragment
	 * represents.
	 */
	private static final String ARG_EVENT_TIME = "event_time";

	private Activity mActivity;

	/**
	 * id of event to be deleted
	 */
	private long id;

	public static DialogFragment newInstance(long id, String module,
			String time, String date) {
		DialogFragment fragment = new DeleteEventFragment();

		Bundle args = new Bundle();
		args.putLong(ARG_EVENT_ID, id);
		args.putString(ARG_EVENT_MODULE, module);
		args.putString(ARG_EVENT_DATE, date);
		args.putString(ARG_EVENT_TIME, time);

		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mActivity = getActivity();

		final Bundle args = getArguments();

		id = args.getLong(ARG_EVENT_ID);

		return new AlertDialog.Builder(mActivity)
				.setTitle(R.string.title_event_delete)
				.setMessage(
						getString(R.string.message_event_delete_single,
								args.getString(ARG_EVENT_MODULE),
								args.getString(ARG_EVENT_DATE),
								args.getString(ARG_EVENT_TIME)))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								new DeleteEvent().execute(id);
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dismiss();
							}
						}).create();
	}

	/**
	 * AsyncTask extension to perform event deletion in the background
	 */
	private class DeleteEvent extends AsyncTask<Long, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Long... params) {
			ContentProviderClient provider = mActivity.getContentResolver()
					.acquireContentProviderClient(Timetable.Events.CONTENT_URI);

			return TimetableParser.deleteEventUser(provider, id);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				if (mActivity instanceof EventDetailActivity)
					mActivity.finish();
				else {
					FragmentManager mgr = getFragmentManager();
					mgr.beginTransaction()
							.remove(mgr
									.findFragmentById(R.id.event_detail_container))
							.commit();
				}
			} else
				// TODO: implement some form of error handling
				AppMsg.makeText(mActivity,
						getString(R.string.message_event_delete_failure),
						AppMsg.STYLE_ALERT).show();
		}
	}

}