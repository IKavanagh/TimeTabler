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

import java.io.IOException;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

import com.devspark.appmsg.AppMsg;

public class RemoveAccountFragment extends DialogFragment {

	/**
	 * Interface used to alert the user to the result of an attempt to remove an
	 * account
	 */
	public interface OnAccountRemovalListener {
		/**
		 * This method will be invoked after an attempt to remove an account
		 * 
		 * @param result
		 *            whether the removal operation was a success or failure
		 */
		public void onAccountRemoval(boolean result);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Activities containing this fragment must implement its callbacks.
		if (!(getActivity() instanceof OnAccountRemovalListener)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.title_account_remove)
				.setMessage(R.string.message_account_remove)
				.setPositiveButton(R.string.action_remove,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								new RemoveAccountTask().execute();
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

	private class RemoveAccountTask extends AsyncTask<Void, Void, Boolean> {
		private static final String TAG = "RemoveAccountTask";

		private Activity activity;

		@Override
		protected Boolean doInBackground(Void... params) {
			activity = getActivity();

			try {
				return AccountManager
						.get(activity)
						.removeAccount(AccountUtils.getAccount(activity), null,
								null).getResult();
			} catch (AuthenticatorException e) {
				return handleException(activity, e);
			} catch (OperationCanceledException e) {
				return handleException(activity, e);
			} catch (IOException e) {
				return handleException(activity, e);
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			((OnAccountRemovalListener) activity).onAccountRemoval(result);
		}

		private boolean handleException(Activity activity, Exception e) {
			Log.e(TAG, "Error whilst removing account", e);

			if (AccountUtils.getAccount(activity) != null) {
				Log.w(TAG, "Account still removed successfully");
				return true;
			}
			return false;
		}
	}

	public static void accountRemoved(Activity activity) {
		AppMsg.makeText(activity,
				activity.getString(R.string.remove_account_success),
				AppMsg.STYLE_CONFIRM).setLayoutGravity(Gravity.BOTTOM).show();

		// Account needs to be created to use app, request system to add one
		AccountManager.get(activity).addAccount(
				activity.getString(R.string.account_type), null, null, null,
				activity, null, null);
	}

	public static void accountNotRemoved(Activity activity) {
		AppMsg.makeText(activity,
				activity.getString(R.string.remove_account_failure),
				AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();
	}

}