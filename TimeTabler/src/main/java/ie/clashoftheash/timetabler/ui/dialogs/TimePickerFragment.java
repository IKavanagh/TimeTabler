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
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.widget.TimePicker;

public class TimePickerFragment extends DialogFragment implements
		OnTimeSetListener {

	/**
	 * The callback interface used to indicate the user is done filling in the
	 * time (they clicked on the 'Set' button).
	 */
	public interface OnTimeSetListener {
		public void onTimeSet(int which, int hour, int minute);
	}

	/**
	 * Constants for use with passing data to and from activity
	 */
	public static final String WHICH = "which";
	public static final String HOUR_OF_DAY = "hour";
	public static final String MINUTE = "minute";

	private Activity activity;
	private int which;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		activity = getActivity();

		Bundle args = getArguments();

		which = args.getInt(WHICH);

		// Create a new instance of DatePickerDialog and return it
		return new TimePickerDialog(activity, this, args.getInt(HOUR_OF_DAY),
				args.getInt(MINUTE),
				android.text.format.DateFormat.is24HourFormat(activity));
	}

	public void onTimeSet(TimePicker view, int hour, int minute) {
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof OnTimeSetListener)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		((TimePickerFragment.OnTimeSetListener) activity).onTimeSet(which,
				hour, minute);
	}

}