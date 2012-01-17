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

package net.robotmedia.billing;

import java.util.Collection;
import java.util.LinkedList;

import net.robotmedia.billing.requests.*;
import net.robotmedia.billing.utils.Compatibility;

import com.android.vending.billing.IMarketBillingService;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BillingService extends Service implements ServiceConnection, IBillingService {

	private static final String ACTION_MARKET_BILLING_SERVICE = "com.android.vending.billing.MarketBillingService.BIND";

	private static final LinkedList<BillingRequest> mPendingRequests = new LinkedList<BillingRequest>();

	@Nullable
	private static IMarketBillingService mService;

	public static void checkBillingSupported(@NotNull Context context) {
		context.startService(createIntent(context, Action.CHECK_BILLING_SUPPORTED));
	}

	public static void confirmNotifications(@NotNull Context context, @NotNull String[] notifyIds) {
		final Intent intent = createIntent(context, Action.CONFIRM_NOTIFICATIONS);
		intent.putExtra(EXTRA_NOTIFY_IDS, notifyIds);
		context.startService(intent);
	}

	public static void confirmNotifications(@NotNull Context context, @NotNull Collection<String> notifyIds) {
		confirmNotifications(context, notifyIds.toArray(new String[notifyIds.size()]));
	}

	public static void getPurchaseInformation(@NotNull Context context, @NotNull Collection<String> notifyIds, long nonce) {
		getPurchaseInformation(context, notifyIds.toArray(new String[notifyIds.size()]), nonce);
	}

	public static void getPurchaseInformation(@NotNull Context context, @NotNull String[] notifyIds, long nonce) {
		final Intent intent = createIntent(context, Action.GET_PURCHASE_INFORMATION);
		intent.putExtra(EXTRA_NOTIFY_IDS, notifyIds);
		intent.putExtra(EXTRA_NONCE, nonce);
		context.startService(intent);
	}

	public static void requestPurchase(@NotNull Context context, String itemId, String developerPayload) {
		final Intent intent = createIntent(context, Action.REQUEST_PURCHASE);
		intent.putExtra(EXTRA_ITEM_ID, itemId);
		intent.putExtra(EXTRA_DEVELOPER_PAYLOAD, developerPayload);
		context.startService(intent);
	}

	public static void restoreTransactions(@NotNull Context context, long nonce) {
		final Intent intent = createIntent(context, Action.RESTORE_TRANSACTIONS);
		intent.setClass(context, BillingService.class);
		intent.putExtra(EXTRA_NONCE, nonce);
		context.startService(intent);
	}

	@NotNull
	private static Intent createIntent(@NotNull Context context, @NotNull Action action) {
		final Intent result = new Intent(Action.toIntentAction(context, action));

		result.setClass(context, BillingService.class);

		return result;
	}

	private void bindMarketBillingService() {
		try {
			final boolean bindResult = bindService(new Intent(ACTION_MARKET_BILLING_SERVICE), this, Context.BIND_AUTO_CREATE);
			if (!bindResult) {
				Log.e(this.getClass().getSimpleName(), "Could not bind to MarketBillingService");
			}
		} catch (SecurityException e) {
			Log.e(this.getClass().getSimpleName(), "Could not bind to MarketBillingService", e);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMarketBillingService.Stub.asInterface(service);
		runPendingRequests();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent, startId);
	}

	// @Override // Avoid compile errors on pre-2.0
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent, startId);
		return Compatibility.START_NOT_STICKY;
	}

	private void handleCommand(@NotNull Intent intent, int startId) {
		final Action action = Action.fromIntentAction(intent);
		if (action != null) {
			action.doAction(this, intent, startId);
		}
	}

	private void runPendingRequests() {
		BillingRequest request;
		int maxStartId = -1;
		while ((request = mPendingRequests.peek()) != null) {
			if (mService != null) {
				runRequest(request);
				mPendingRequests.remove();
				if (maxStartId < request.getStartId()) {
					maxStartId = request.getStartId();
				}
			} else {
				bindMarketBillingService();
				return;
			}
		}
		if (maxStartId >= 0) {
			stopSelf(maxStartId);
		}
	}

	private void runRequest(BillingRequest request) {
		try {
			final long requestId = request.run(mService);
			BillingController.onRequestSent(requestId, request);
		} catch (RemoteException e) {
			Log.w(this.getClass().getSimpleName(), "Remote billing service crashed");
			// TODO: Retry?
		}
	}

	@Override
	public void runRequestOrQueue(@NotNull BillingRequest request) {
		mPendingRequests.add(request);
		if (mService == null) {
			bindMarketBillingService();
		} else {
			runPendingRequests();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Ensure we're not leaking Android Market billing service
		if (mService != null) {
			try {
				unbindService(this);
			} catch (IllegalArgumentException e) {
				// This might happen if the service was disconnected
			}
		}
	}

}
