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
import ie.clashoftheash.timetabler.authenticator.AccountUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;

import com.devspark.appmsg.AppMsg;

public class AccountExistsFragment extends DialogFragment {

	public static final String ARG_PROGRAMME_CODE = "programme_code";
	public static final String ARG_YEAR = "year";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Bundle args = getArguments();

		return new AlertDialog.Builder(getActivity())
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.error_account_exists_title)
				.setMessage(R.string.error_account_exists_message)
				.setCancelable(false)
				.setPositiveButton(R.string.action_update,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final Activity activity = getActivity();

								new AccountUtils(activity).updateAccount(
										args.getString(ARG_PROGRAMME_CODE),
										args.getString(ARG_YEAR));

								AppMsg.makeText(
										activity,
										activity.getString(R.string.updated_account_success),
										AppMsg.STYLE_INFO)
										.setLayoutGravity(Gravity.BOTTOM)
										.show();
							}
						})
				.setNeutralButton(R.string.action_cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dismiss();
							}
						})
				.setNegativeButton(R.string.action_remove,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								new RemoveAccountFragment().show(
										getFragmentManager(),
										"RemoveAccountFragment");
							}
						}).create();
	}

}