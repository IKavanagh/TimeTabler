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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class NetworkErrorFragment extends DialogFragment {

	/**
	 * Constants for use with fragment arguments
	 */
	private static final String ARG_MESSAGE = "message";
	private static final String ARG_POSITIVE = "positive";
	private static final String ARG_TYPE = "type";

	/**
	 * The dialog to be shown when there is no network connection available
	 */
	private static final String TYPE_NO_NETWORK = "no_network";

	/**
	 * The dialog to be shown when the internet connection redirects (most
	 * likely due to login required)
	 */
	private static final String TYPE_REDIRECT = "redirect";

	public static DialogFragment handleNoConnection() {
		Bundle args = new Bundle();
		args.putInt(ARG_MESSAGE, R.string.message_no_connection);
		args.putInt(ARG_POSITIVE, R.string.enable_wifi);
		args.putString(ARG_TYPE, TYPE_NO_NETWORK);

		DialogFragment dialog = new NetworkErrorFragment();
		dialog.setArguments(args);

		return dialog;
	}

	public static DialogFragment handleRedirect() {
		Bundle args = new Bundle();
		args.putInt(ARG_MESSAGE, R.string.message_redirect);
		args.putInt(ARG_POSITIVE, R.string.sign_in);
		args.putString(ARG_TYPE, TYPE_REDIRECT);

		DialogFragment dialog = new NetworkErrorFragment();
		dialog.setArguments(args);

		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Bundle args = getArguments();

		return new AlertDialog.Builder(getActivity())
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(args.getInt(ARG_MESSAGE))
				.setPositiveButton(args.getInt(ARG_POSITIVE),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (args.getString(ARG_TYPE).equals(
										TYPE_NO_NETWORK)) {
									Intent intent = new Intent(
											Settings.ACTION_WIRELESS_SETTINGS);
									startActivity(intent);
								} else if (args.getString(ARG_TYPE).equals(
										TYPE_REDIRECT)) {
									Intent browserIntent = new Intent(
											Intent.ACTION_VIEW,
											Uri.parse("http://www.google.ie"));
									startActivity(browserIntent);
								}
							}
						})
				.setNegativeButton(R.string.action_cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dismiss();
							}
						}).create();
	}

}