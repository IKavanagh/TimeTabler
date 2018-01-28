/*
 * Copyright 2013 Ian Kavanagh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ie.clashoftheash.timetabler.ui.widget;

import ie.clashoftheash.timetabler.R;
import ie.clashoftheash.timetabler.provider.TimetableProvider;
import ie.clashoftheash.timetabler.provider.TimetableUtils;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AgendaAdapter extends SimpleCursorAdapter {

	private final Context mContext;
	private final LayoutInflater inflater;
	private final Resources res;

	public AgendaAdapter(Context context, int layout, String[] from, int[] to) {
		super(context, layout, null, from, to, 0);
		mContext = context;
		inflater = LayoutInflater.from(context);
		res = context.getResources();
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return inflater.inflate(R.layout.list_item_agenda_block, parent, false);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		super.bindView(view, context, cursor);

		String eventType = cursor
				.getString(TimetableProvider.READ_EVENT_EVENT_TYPE_INDEX);

		((TextView) view.findViewById(R.id.module)).setText(cursor
				.getString(TimetableProvider.READ_EVENT_MODULE_INDEX));

		// Gather references to needed time views
		final ViewGroup timeView = (ViewGroup) view.findViewById(R.id.time);
		final TextView startTimeView = (TextView) view
				.findViewById(R.id.start_time);
		final TextView endTimeView = (TextView) view
				.findViewById(R.id.end_time);

		// Reset text colours in case view is reused
		startTimeView.setTextColor(res
				.getColor(android.R.color.primary_text_dark));
		endTimeView.setTextColor(res
				.getColor(android.R.color.primary_text_dark));

		if (eventType.equals(mContext.getString(R.string.lecture))) {
			timeView.setBackgroundColor(res.getColor(R.color.lecture));
		} else if (eventType.equals(mContext.getString(R.string.practical))) {
			timeView.setBackgroundColor(res.getColor(R.color.practical));
		} else if (eventType.equals(mContext.getString(R.string.tutorial))) {
			timeView.setBackgroundColor(res.getColor(R.color.tutorial));
		} else if (eventType.equals(mContext.getString(R.string.seminar))) {
			timeView.setBackgroundColor(res.getColor(R.color.seminar));
		} else {
			startTimeView.setTextColor(res
					.getColor(android.R.color.primary_text_light));
			endTimeView.setTextColor(res
					.getColor(android.R.color.primary_text_light));
			timeView.setBackgroundColor(res
					.getColor(android.R.color.transparent));
		}

		java.text.DateFormat timeFormat = DateFormat.getTimeFormat(mContext);

		Calendar start = Calendar.getInstance(TimetableUtils.TIMEZONE_UTC);
		start.setTimeInMillis(cursor
				.getLong(TimetableProvider.READ_EVENT_START_INDEX));

		start = TimetableUtils.switchTimeZone(start, TimeZone.getDefault());

		Calendar end = Calendar.getInstance(TimetableUtils.TIMEZONE_UTC);
		end.setTimeInMillis(cursor
				.getLong(TimetableProvider.READ_EVENT_END_INDEX));

		end = TimetableUtils.switchTimeZone(end, TimeZone.getDefault());

		setViewText(startTimeView, timeFormat.format(start.getTime()));
		setViewText(endTimeView, timeFormat.format(end.getTime()));

		View divider = view.findViewById(R.id.divider);

		// Reset divider back to previous settings in case of reused view
		divider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
		divider.setBackgroundColor(res.getColor(android.R.color.darker_gray));

		if (end.before(Calendar.getInstance())) {
			view.findViewById(R.id.overlay).setVisibility(View.VISIBLE);
			try {
				if (getView(cursor.getPosition() + 1, null, null).findViewById(
						R.id.overlay).getVisibility() == View.GONE)
					divider.setLayoutParams(new LayoutParams(
							LayoutParams.MATCH_PARENT, 5));
				divider.setBackgroundColor(res.getColor(android.R.color.black));
			} catch (IllegalStateException e) {
				// Exception occurs if no events loaded after this event,
				// therefore this event should have the thicker divider
				divider.setLayoutParams(new LayoutParams(
						LayoutParams.MATCH_PARENT, 5));
				divider.setBackgroundColor(res.getColor(android.R.color.black));
			}
		} else
			view.findViewById(R.id.overlay).setVisibility(View.GONE);

		// if user isn't using 24 hour clock set min width for time column
		// to keep consistent size
		if (!DateFormat.is24HourFormat(mContext))
			timeView.setMinimumWidth((int) res
					.getDimension(R.dimen.list_agenda_time_width));
		else
			timeView.setMinimumWidth(0);
	}
}