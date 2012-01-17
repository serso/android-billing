package net.robotmedia.billing;

import android.content.Context;
import android.content.Intent;
import net.robotmedia.billing.requests.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: serso
 * Date: 1/17/12
 * Time: 12:39 PM
 */
public interface IBillingService {

	static final String EXTRA_ITEM_ID = "ITEM_ID";
	static final String EXTRA_NONCE = "EXTRA_NONCE";
	static final String EXTRA_NOTIFY_IDS = "NOTIFY_IDS";
	static final String EXTRA_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";

	void runRequestOrQueue(@NotNull BillingRequest request);

	String getPackageName();

	public static enum Action {

		CHECK_BILLING_SUPPORTED{
			@Override
			void doAction(@NotNull IBillingService service, @NotNull Intent intent, int startId) {
				service.runRequestOrQueue(new CheckBillingSupported(service.getPackageName(), startId));
			}
		},

		CONFIRM_NOTIFICATIONS {
			@Override
			void doAction(@NotNull IBillingService service, @NotNull Intent intent, int startId) {
				final String packageName = service.getPackageName();
				final String[] notifyIds = intent.getStringArrayExtra(EXTRA_NOTIFY_IDS);

				service.runRequestOrQueue(new ConfirmNotifications(packageName, startId, notifyIds));
			}
		},

		GET_PURCHASE_INFORMATION{
			@Override
			void doAction(@NotNull IBillingService service, @NotNull Intent intent, int startId) {
				final String packageName = service.getPackageName();
				final long nonce = intent.getLongExtra(EXTRA_NONCE, 0);
				final String[] notifyIds = intent.getStringArrayExtra(EXTRA_NOTIFY_IDS);
				final GetPurchaseInformation request = new GetPurchaseInformation(packageName, startId, notifyIds);
				request.setNonce(nonce);

				service.runRequestOrQueue(request);
			}
		},

		REQUEST_PURCHASE{
			@Override
			void doAction(@NotNull IBillingService service, @NotNull Intent intent, int startId) {
				final String packageName = service.getPackageName();
				final String itemId = intent.getStringExtra(EXTRA_ITEM_ID);
				final String developerPayload = intent.getStringExtra(EXTRA_DEVELOPER_PAYLOAD);
				final RequestPurchase request = new RequestPurchase(packageName, startId, itemId, developerPayload);
				service.runRequestOrQueue(request);
			}
		},

		RESTORE_TRANSACTIONS{
			@Override
			void doAction(@NotNull IBillingService service, @NotNull Intent intent, int startId) {
				final String packageName = service.getPackageName();
				final long nonce = intent.getLongExtra(EXTRA_NONCE, 0);
				final RestoreTransactions request = new RestoreTransactions(packageName, startId);
				request.setNonce(nonce);
				service.runRequestOrQueue(request);
			}
		};

		abstract void doAction(@NotNull IBillingService service, @NotNull Intent intent, int startId);

		@NotNull
		static String toIntentAction(@NotNull Context context, @NotNull Action action) {
			return context.getPackageName() + "." + action.name();
		}

		@Nullable
		static Action fromIntentAction(@NotNull Intent intent) {
			final String actionString = intent.getAction();
			if (actionString == null) {
				return null;
			}

			final String[] split = actionString.split("\\.");
			if (split.length <= 0) {
				return null;
			}

			return Action.valueOf(split[split.length - 1]);
		}
	}


}
