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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import ie.clashoftheash.timetabler.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A class to handle network related operations
 * 
 * @author Ian Kavanagh
 */
public class NetworkUtils {

	private final Context mContext;

	public NetworkUtils(Context context) {
		mContext = context;
	}

	/**
	 * Checks whether there is an internet connection available
	 * 
	 * @return true if there is a connection available, otherwise false
	 */
	public boolean connectionAvailable() {
		ConnectivityManager connMgr = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

		return (activeInfo != null && activeInfo.isConnected());
	}

	/**
	 * Builds timetable URL
	 * 
	 * @param programmeCode
	 *            users programme code
	 * @param year
	 *            users year of study
	 * @param sem
	 *            semester to build URL for 1 or 2
	 * @return the built URL
	 * @throws MalformedURLException
	 */
	public URL buildTimetableUrl(String programmeCode, String year, int sem)
			throws MalformedURLException {
		String semester;

		switch (sem) {
		case 1:
			semester = "1-12";
			break;
		case 2:
			semester = "20-31";
			break;
		default:
			throw new IllegalArgumentException("There are only two semesters, 1 and 2");
		}
		return new URL(String.format(
				mContext.getString(R.string.timetable_url), programmeCode,
				year, semester));
	}

	/**
	 * Opens a Http connection with the url provided
	 * 
	 * @param url
	 *            url to open connection to
	 * @return reference to the opened http connection or null if it wasn't a
	 *         success
	 * @throws IOException
	 */
	public HttpURLConnection openConnection(URL url) throws IOException,
			IllegalArgumentException {
		if (url == null)
			throw new IllegalArgumentException("Url can't be null");

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000/* milliseconds */);
		conn.setConnectTimeout(15000/* milliseconds */);
		conn.setRequestMethod("GET");

		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
			return conn;
		throw new IOException();
	}

	/**
	 * Downloads and returns content of HttpURLConnection passed
	 * 
	 * @param conn
	 *            http connection to downloads
	 * @return string holding the data downloaded
	 * @throws IOException
	 */
	public String downloadURL(HttpURLConnection conn) throws IOException {
		InputStream stream = conn.getInputStream();

		InputStreamReader reader = new InputStreamReader(stream);
		BufferedReader bufferedReader = new BufferedReader(reader);

		String s;
		StringBuilder builder = new StringBuilder();

		while ((s = bufferedReader.readLine()) != null)
			builder.append(s);

		return builder.toString();
	}

	/**
	 * Helper method to determine if we have been redirected possibly for user
	 * to login to wifi
	 * 
	 * @param url
	 *            original url connection was set up for
	 * @param conn
	 *            connection set up for url
	 * @return true if we have been redirected
	 */
	public boolean connectionRedirected(URL url, HttpURLConnection conn) {
		return !(url.getHost().equals(conn.getURL().getHost()));
	}

	/**
	 * Determines if we can sync data based on connection available and users
	 * settings
	 * 
	 * @return true if we can sync
	 */
	public static boolean canPerformSync(Context context) {
		ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
		if (activeInfo != null && activeInfo.isConnected()) {
			if (activeInfo.getType() == ConnectivityManager.TYPE_WIFI)
				return true;
			else if (activeInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				// false = any network
				return !PreferenceManager
						.getDefaultSharedPreferences(context)
						.getBoolean(
								context.getString(R.string.pref_key_sync_wifi),
								Boolean.parseBoolean(context
										.getString(R.string.pref_default_sync_wifi)));
			}
		}
		return false;
	}

}
