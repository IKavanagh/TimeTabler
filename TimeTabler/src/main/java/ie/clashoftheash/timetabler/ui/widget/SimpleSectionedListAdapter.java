/*
 * Copyright 2013 Ian Kavanagh
 * Copyright 2012 Google Inc.
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

import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SimpleSectionedListAdapter extends BaseAdapter {
	private boolean mValid = true;
	private final int mSectionResourceId;
	private final int[] mTo;
	private final LayoutInflater mLayoutInflater;
	private final ListAdapter mBaseAdapter;
	private final SparseArray<Section> mSections = new SparseArray<Section>();

	public static class Section {
		private final int firstPosition;
		private int sectionedPosition;
		private final String[] text;
		private final boolean showOverlay;

		public Section(int firstPosition, String[] text, boolean showOverlay) {
			this.firstPosition = firstPosition;
			this.text = text;
			this.showOverlay = showOverlay;
		}

	}

	public SimpleSectionedListAdapter(Context context, int sectionResourceId,
			int[] to, ListAdapter baseAdapter) {
		mLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mSectionResourceId = sectionResourceId;
		mTo = to;
		mBaseAdapter = baseAdapter;
		mBaseAdapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				mValid = !mBaseAdapter.isEmpty();
				notifyDataSetChanged();
			}

			@Override
			public void onInvalidated() {
				mValid = false;
				notifyDataSetInvalidated();
			}
		});
	}

	public void setSections(Section[] sections) {
		mSections.clear();

		Arrays.sort(sections, new Comparator<Section>() {
			@Override
			public int compare(Section o, Section o1) {
				return (o.firstPosition == o1.firstPosition) ? 0
						: ((o.firstPosition < o1.firstPosition) ? -1 : 1);
			}
		});

		int offset = 0; // offset positions for the headers we're adding
		for (Section section : sections) {
			section.sectionedPosition = section.firstPosition + offset;
			mSections.append(section.sectionedPosition, section);
			++offset;
		}

		notifyDataSetChanged();
	}

	public int positionToSectionedPosition(int position) {
		int offset = 0;
		for (int i = 0; i < mSections.size(); i++) {
			if (mSections.valueAt(i).firstPosition > position) {
				break;
			}
			++offset;
		}
		return position + offset;
	}

	public int sectionedPositionToPosition(int sectionedPosition) {
		if (isSectionHeaderPosition(sectionedPosition)) {
			return ListView.INVALID_POSITION;
		}

		int offset = 0;
		for (int i = 0; i < mSections.size(); i++) {
			if (mSections.valueAt(i).sectionedPosition > sectionedPosition) {
				break;
			}
			--offset;
		}
		return sectionedPosition + offset;
	}

	public boolean isSectionHeaderPosition(int position) {
		return mSections.get(position) != null;
	}

	@Override
	public int getCount() {
		return (mValid ? mBaseAdapter.getCount() + mSections.size() : 0);
	}

	@Override
	public Object getItem(int position) {
		return isSectionHeaderPosition(position) ? mSections.get(position)
				: mBaseAdapter.getItem(sectionedPositionToPosition(position));
	}

	@Override
	public long getItemId(int position) {
		return isSectionHeaderPosition(position) ? Integer.MAX_VALUE
				- mSections.indexOfKey(position) : mBaseAdapter
				.getItemId(sectionedPositionToPosition(position));
	}

	@Override
	public int getItemViewType(int position) {
		return isSectionHeaderPosition(position) ? getViewTypeCount() - 1
				: mBaseAdapter.getItemViewType(position);
	}

	@Override
	public boolean isEnabled(int position) {
		// noinspection SimplifiableConditionalExpression
		return isSectionHeaderPosition(position) ? false : mBaseAdapter
				.isEnabled(sectionedPositionToPosition(position));
	}

	@Override
	public int getViewTypeCount() {
		return mBaseAdapter.getViewTypeCount() + 1; // the section headings
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean hasStableIds() {
		return mBaseAdapter.hasStableIds();
	}

	@Override
	public boolean isEmpty() {
		return mBaseAdapter.isEmpty();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (isSectionHeaderPosition(position)) {
			ViewGroup view = (ViewGroup) convertView;
			if (view == null) {
				view = (ViewGroup) mLayoutInflater.inflate(mSectionResourceId,
						parent, false);
			}

			int count = mTo.length;
			for (int i = 0; i < count; i++) {
				View v = view.findViewById(mTo[i]);
				if (v != null) {
					String text = mSections.get(position).text[i];
					if (text == null)
						text = "";

					if (v instanceof TextView)
						setViewText((TextView) v, text);
					else
						throw new IllegalStateException(
								v.getClass().getName()
										+ " is not a "
										+ " view that can be bound by this SimpleSectionedListAdapter");

				}
			}

			if (mSections.get(position).showOverlay)
				view.findViewById(R.id.overlay).setVisibility(View.VISIBLE);
			else
				view.findViewById(R.id.overlay).setVisibility(View.GONE);

			return view;
		} else {
			return mBaseAdapter.getView(sectionedPositionToPosition(position),
					convertView, parent);
		}
	}

	private void setViewText(TextView v, String text) {
		v.setText(text);
	}
}
