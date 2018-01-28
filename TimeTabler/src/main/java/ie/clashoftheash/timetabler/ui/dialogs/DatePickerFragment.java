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

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment implements
		OnDateSetListener {

	/**
	 * The callback used to indicate the user is done filling in the date.
	 */
	public interface OnDateSetListener {
		public void onDateSet(int which, int year, int month, int day);
	}

	/**
	 * Constants for use with passing data to and from activity
	 */
	public static final String WHICH = "which";
	public static final String YEAR = "year";
	public static final String MONTH = "month";
	public static final String DAY = "day";

	private Activity activity;
	private int which;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		activity = getActivity();

		Bundle args = getArguments();

		which = args.getInt(WHICH);

		// Create a new instance of DatePickerDialog and return it
		return new DatePickerDialog(activity, this, args.getInt(YEAR),
				args.getInt(MONTH), args.getInt(DAY));
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof OnDateSetListener)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		((DatePickerFragment.OnDateSetListener) activity).onDateSet(which,
				year, month, day);

	}
}