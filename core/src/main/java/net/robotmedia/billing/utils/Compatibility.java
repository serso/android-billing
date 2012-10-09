/*   Copyright 2011 Robot Media SL (http://www.robotmedia.net)
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package net.robotmedia.billing.utils;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Compatibility {

    @Nullable
	private static Method startIntentSender;

	public static int START_NOT_STICKY;

	@SuppressWarnings("rawtypes")
	private static final Class[] START_INTENT_SENDER_SIGNATURE = new Class[]{IntentSender.class, Intent.class, int.class, int.class, int.class};

	static {
		initCompatibility();
	}

	private static void initCompatibility() {
		try {
			final Field field = Service.class.getField("START_NOT_STICKY");
			START_NOT_STICKY = field.getInt(null);
		} catch (Exception e) {
			START_NOT_STICKY = 2;
		}

		try {
			startIntentSender = Activity.class.getMethod("startIntentSender", START_INTENT_SENDER_SIGNATURE);
		} catch (SecurityException e) {
			startIntentSender = null;
		} catch (NoSuchMethodException e) {
			startIntentSender = null;
		}
	}

	public static void startIntentSender(@NotNull Context context,
										 @NotNull IntentSender intentSender,
										 @Nullable Intent intent) {
		if (startIntentSender != null) {

			final Object[] args = new Object[5];
			args[0] = intentSender;
			args[1] = intent;
			args[2] = 0;
			args[3] = 0;
			args[4] = 0;

			try {
				startIntentSender.invoke(context, args);
			} catch (Exception e) {
				Log.e(Compatibility.class.getSimpleName(), "startIntentSender", e);
			}
		}
	}

	public static boolean isStartIntentSenderSupported() {
		return startIntentSender != null;
	}
}
