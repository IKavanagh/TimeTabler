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

package ie.clashoftheash.timetabler.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncService extends Service {

	// Used to lock SyncAdapter while creating
	private static final Object SyncAdapterLock = new Object();

	private SyncAdapter sSyncAdapter;

	@Override
	public void onCreate() {
		synchronized (SyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}

}
