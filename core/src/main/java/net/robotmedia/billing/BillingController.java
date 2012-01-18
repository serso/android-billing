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

import java.util.*;

import net.robotmedia.billing.helper.AbstractBillingObserver;
import net.robotmedia.billing.requests.IBillingRequest;
import net.robotmedia.billing.requests.ResponseCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.robotmedia.billing.model.Transaction;
import net.robotmedia.billing.model.TransactionManager;
import net.robotmedia.billing.security.DefaultSignatureValidator;
import net.robotmedia.billing.security.ISignatureValidator;
import net.robotmedia.billing.utils.Compatibility;
import net.robotmedia.billing.utils.Security;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class BillingController {

	public static enum BillingStatus {
		UNKNOWN,
		SUPPORTED,
		UNSUPPORTED
	}

	/**
	 * Used to provide on-demand values to the billing controller.
	 */
	public interface IConfiguration {

		/**
		 * Returns a salt for the obfuscation of purchases in local memory.
		 *
		 * @return array of 20 random bytes.
		 */
		public byte[] getObfuscationSalt();

		/**
		 * Returns the public key used to verify the signature of responses of
		 * the Market Billing service.
		 *
		 * @return Base64 encoded public key.
		 */
		public String getPublicKey();
	}

	@NotNull
	private static BillingStatus status = BillingStatus.UNKNOWN;

	private static IConfiguration configuration = null;
	private static boolean debug = false;

	@Nullable
	private static ISignatureValidator validator = null;

	private static final String JSON_NONCE = "nonce";
	private static final String JSON_ORDERS = "orders";


	// synchronized field
	@NotNull
	private static final Set<String> automaticConfirmations = new HashSet<String>();

	// synchronized field
	@NotNull
	private static final Map<String, Set<String>> manualConfirmations = new HashMap<String, Set<String>>();

	// synchronized field
	@NotNull
	private static final Map<Long, IBillingRequest> pendingRequests = new HashMap<Long, IBillingRequest>();

	public static final String LOG_TAG = "Billing";


	/**
	 * Adds the specified notification to the set of manual confirmations of the
	 * specified item.
	 *
	 * @param itemId		 id of the item.
	 * @param notificationId id of the notification.
	 */
	private static void addManualConfirmation(@NotNull String itemId, @NotNull String notificationId) {
		synchronized (manualConfirmations) {
			Set<String> notifications = manualConfirmations.get(itemId);
			if (notifications == null) {
				notifications = new HashSet<String>();
				manualConfirmations.put(itemId, notifications);
			}
			notifications.add(notificationId);
		}
	}

	/**
	 * Returns the billing status. If it is currently unknown, checks the billing
	 * status asynchronously, in which case observers will eventually receive
	 * a {@link IBillingObserver#onBillingChecked(boolean)} notification.
	 *
	 * @param context context
	 * @return the current billing status (unknown, supported or unsupported).
	 * @see IBillingObserver#onBillingChecked(boolean)
	 */
	public static BillingStatus checkBillingSupported(@NotNull Context context) {
		if (status == BillingStatus.UNKNOWN) {
			BillingService.checkBillingSupported(context);
		}
		return status;
	}

	/**
	 * Requests to confirm all pending notifications for the specified item.
	 *
	 * @param context context
	 * @param itemId  id of the item whose purchase must be confirmed.
	 * @return true if pending notifications for this item were found, false
	 *         otherwise.
	 */
	public static boolean confirmNotifications(@NotNull Context context, @NotNull String itemId) {
		synchronized (manualConfirmations) {
			final Set<String> notifications = manualConfirmations.get(itemId);
			if (notifications != null) {
				confirmNotifications(context, notifications);
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Requests to confirm all specified notifications.
	 *
	 * @param context   context
	 * @param notifyIds array with the ids of all the notifications to confirm.
	 */
	private static void confirmNotifications(Context context, String[] notifyIds) {
		BillingService.confirmNotifications(context, notifyIds);
	}

	private static void confirmNotifications(@NotNull Context context, @NotNull Collection<String> notifyIds) {
		BillingService.confirmNotifications(context, notifyIds);
	}

	/**
	 * Returns the number of purchases for the specified item. Refunded and
	 * cancelled purchases are not subtracted. See
	 *
	 * @param context context
	 * @param itemId  id of the item whose purchases will be counted.
	 * @return number of purchases for the specified item.
	 */
	public static int countPurchases(Context context, String itemId) {
		final byte[] salt = getSalt();
		itemId = salt != null ? Security.obfuscate(context, salt, itemId) : itemId;
		return TransactionManager.countPurchases(context, itemId);
	}

	protected static void debug(String message) {
		if (debug) {
			Log.d(LOG_TAG, message);
		}
	}

	/**
	 * Requests purchase information for the specified notification. Immediately
	 * followed by a call to
	 * {@link #onPurchaseStateChanged(Context, String, String)}, if the request
	 * is successful.
	 *
	 * @param context  context
	 * @param notifyId id of the notification whose purchase information is
	 *                 requested.
	 */
	private static void getPurchaseInformation(Context context, String notifyId) {
		final long nonce = Security.generateNonce();
		BillingService.getPurchaseInformation(context, new String[]{notifyId}, nonce);
	}

	/**
	 * Gets the salt from the configuration and logs a warning if it's null.
	 *
	 * @return salt.
	 */
	@Nullable
	private static byte[] getSalt() {
		byte[] salt = null;
		if (configuration == null || ((salt = configuration.getObfuscationSalt()) == null)) {
			Log.w(LOG_TAG, "Can't (un)obfuscate purchases without salt");
		}
		return salt;
	}

	/**
	 * Lists all transactions stored locally, including cancellations and
	 * refunds.
	 *
	 * @param context context
	 * @return list of transactions.
	 */
	public static List<Transaction> getTransactions(Context context) {
		List<Transaction> transactions = TransactionManager.getTransactions(context);
		unobfuscate(context, transactions);
		return transactions;
	}

	/**
	 * Lists all transactions of the specified item, stored locally.
	 *
	 * @param context context
	 * @param itemId  id of the item whose transactions will be returned.
	 * @return list of transactions.
	 */
	public static List<Transaction> getTransactions(Context context, String itemId) {
		final byte[] salt = getSalt();
		itemId = salt != null ? Security.obfuscate(context, salt, itemId) : itemId;
		List<Transaction> transactions = TransactionManager.getTransactions(context, itemId);
		unobfuscate(context, transactions);
		return transactions;
	}

	/**
	 * Returns true if the specified item has been registered as purchased in
	 * local memory. Note that if the item was later canceled or refunded this
	 * will still return true. Also note that the item might have been purchased
	 * in another installation, but not yet registered in this one.
	 *
	 * @param context context
	 * @param itemId  item id.
	 * @return true if the specified item is purchased, false otherwise.
	 */
	public static boolean isPurchased(Context context, String itemId) {
		final byte[] salt = getSalt();
		itemId = salt != null ? Security.obfuscate(context, salt, itemId) : itemId;
		return TransactionManager.isPurchased(context, itemId);
	}

	/**
	 * Obfuscates the specified purchase. Only the order id, product id and
	 * developer payload are obfuscated.
	 *
	 * @param context  context
	 * @param purchase purchase to be obfuscated.
	 * @see #unobfuscate(Context, Transaction)
	 */
	static void obfuscate(Context context, Transaction purchase) {
		final byte[] salt = getSalt();
		if (salt == null) {
			return;
		}
		purchase.orderId = Security.obfuscate(context, salt, purchase.orderId);
		purchase.productId = Security.obfuscate(context, salt, purchase.productId);
		purchase.developerPayload = Security.obfuscate(context, salt, purchase.developerPayload);
	}

	/**
	 * Called after the response to a
	 * {@link net.robotmedia.billing.requests.CheckBillingSupportedRequest} request is
	 * received.
	 *
	 * @param supported billing supported
	 */
	public static void onBillingChecked(boolean supported) {
		status = supported ? BillingStatus.SUPPORTED : BillingStatus.UNSUPPORTED;
		BillingObserverRegistry.onBillingCheckedObservers(supported);
	}

	/**
	 * Called when an IN_APP_NOTIFY message is received.
	 *
	 * @param context  context
	 * @param notifyId notification id.
	 */
	protected static void onNotify(Context context, String notifyId) {
		debug("Notification " + notifyId + " available");

		getPurchaseInformation(context, notifyId);
	}

	/**
	 * Called after the response to a
	 * {@link net.robotmedia.billing.requests.GetPurchaseInformationRequest} request is
	 * received. Registers all transactions in local memory and confirms those
	 * who can be confirmed automatically.
	 *
	 * @param context	context
	 * @param signedData signed JSON data received from the Market Billing service.
	 * @param signature  data signature.
	 */
	protected static void onPurchaseStateChanged(@NotNull Context context, @Nullable String signedData, @Nullable String signature) {
		debug("Purchase state changed");

		if (TextUtils.isEmpty(signedData)) {
			Log.w(LOG_TAG, "Signed data is empty");
			return;
		}

		if (!debug) {
			if (TextUtils.isEmpty(signature)) {
				Log.w(LOG_TAG, "Empty signature requires debug mode");
				return;
			}

			final ISignatureValidator validator = getSignatureValidator();
			if (!validator.validate(signedData, signature)) {
				Log.w(LOG_TAG, "Signature does not match data.");
				return;
			}
		}

		List<Transaction> transactions;
		try {
			final JSONObject jObject = new JSONObject(signedData);
			if (!verifyNonce(jObject)) {
				Log.w(LOG_TAG, "Invalid nonce");
				return;
			}
			transactions = parseTransactions(jObject);
		} catch (JSONException e) {
			Log.e(LOG_TAG, "JSON exception: ", e);
			return;
		}

		final List<String> confirmations = new ArrayList<String>();
		for (Transaction transaction : transactions) {

			if (transaction.notificationId != null) {
				synchronized (automaticConfirmations) {
					if (automaticConfirmations.contains(transaction.productId)) {
						confirmations.add(transaction.notificationId);
					} else {
						// TODO: Discriminate between purchases, cancellations and
						// refunds.
						addManualConfirmation(transaction.productId, transaction.notificationId);
					}
				}
			} else {
				// TODO: Discriminate between purchases, cancellations and
				// refunds.
				addManualConfirmation(transaction.productId, transaction.notificationId);
			}
			storeTransaction(context, transaction);
			BillingObserverRegistry.notifyPurchaseStateChange(transaction.productId, transaction.purchaseState);
		}

		if (!confirmations.isEmpty()) {
			final String[] notifyIds = confirmations.toArray(new String[confirmations.size()]);
			confirmNotifications(context, notifyIds);
		}
	}

	/**
	 * Called after a {@link net.robotmedia.billing.requests.BillingRequest} is
	 * sent.
	 *
	 * @param requestId the id the request.
	 * @param request   the billing request.
	 */
	protected static void onRequestSent(long requestId, IBillingRequest request) {
		debug("Request " + requestId + " of type " + request.getRequestType() + " sent");

		if (request.isSuccess()) {
			synchronized (pendingRequests) {
				pendingRequests.put(requestId, request);
			}
		} else if (request.hasNonce()) {
			// in case of unsuccessful request with nonce we shall unregister nonce
			Security.removeNonce(request.getNonce());
		}
	}

	/**
	 * Called after a {@link net.robotmedia.billing.requests.BillingRequest} is
	 * sent.
	 *
	 * @param requestId	the id of the request.
	 * @param responseCode the response code.
	 * @see net.robotmedia.billing.requests.ResponseCode
	 */
	protected static void onResponseCode(long requestId, int responseCode) {
		final ResponseCode response = ResponseCode.valueOf(responseCode);
		debug("Request " + requestId + " received response " + response);

		synchronized (pendingRequests) {
			final IBillingRequest request = pendingRequests.get(requestId);
			if (request != null) {
				pendingRequests.remove(requestId);
				request.onResponseCode(response);
			}
		}
	}

	/**
	 * Parse all purchases from the JSON data received from the Market Billing
	 * service.
	 *
	 * @param data JSON data received from the Market Billing service.
	 * @return list of purchases.
	 * @throws JSONException if the data couldn't be properly parsed.
	 */
	private static List<Transaction> parseTransactions(@NotNull JSONObject data) throws JSONException {
		final List<Transaction> result = new ArrayList<Transaction>();

		final JSONArray orders = data.optJSONArray(JSON_ORDERS);
		if (orders != null) {
			for (int i = 0; i < orders.length(); i++) {
				final JSONObject jElement = orders.getJSONObject(i);
				result.add(Transaction.parse(jElement));
			}
		}

		return result;
	}

	/**
	 * Requests the purchase of the specified item. The transaction will not be
	 * confirmed automatically.
	 *
	 * @param context context
	 * @param itemId  id of the item to be purchased.
	 * @see #requestPurchase(Context, String, boolean)
	 */
	public static void requestPurchase(@NotNull Context context, @NotNull String itemId) {
		requestPurchase(context, itemId, false);
	}

	/**
	 * Requests the purchase of the specified item with optional automatic
	 * confirmation.
	 *
	 * @param context		  context
	 * @param itemId		   id of the item to be purchased.
	 * @param autoConfirmation if true, the transaction will be confirmed automatically. If
	 *                         false, the transaction will have to be confirmed with a call
	 *                         to {@link #confirmNotifications(Context, String)}.
	 * @see IBillingObserver#onPurchaseIntent(String, PendingIntent)
	 */
	public static void requestPurchase(@NotNull Context context, @NotNull String itemId, boolean autoConfirmation) {
		if (autoConfirmation) {
			synchronized (automaticConfirmations) {
				automaticConfirmations.add(itemId);
			}
		}
		BillingService.requestPurchase(context, itemId, null);
	}

	/**
	 * Requests to restore all transactions.
	 *
	 * @param context context
	 */
	public static void restoreTransactions(Context context) {
		final long nonce = Security.generateNonce();
		BillingService.restoreTransactions(context, nonce);
	}

	/**
	 * Sets the configuration instance of the controller.
	 *
	 * @param config configuration instance.
	 */
	public static void setConfiguration(IConfiguration config) {
		configuration = config;
	}

	/**
	 * Sets debug mode.
	 *
	 * @param debug debug
	 */
	public static void setDebug(boolean debug) {
		BillingController.debug = debug;
	}

	/**
	 * Sets a custom signature validator. If no custom signature validator is
	 * provided,
	 * {@link net.robotmedia.billing.security.DefaultSignatureValidator} will
	 * be used.
	 *
	 * @param validator signature validator instance.
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public static void setSignatureValidator(ISignatureValidator validator) {
		BillingController.validator = validator;
	}

	@NotNull
	public static ISignatureValidator getSignatureValidator() {
		return BillingController.validator != null ? BillingController.validator : new DefaultSignatureValidator(BillingController.configuration);
	}

	/**
	 * Starts the specified purchase intent with the specified activity.
	 *
	 * @param activity	   activity
	 * @param purchaseIntent purchase intent.
	 * @param intent		 intent
	 */
	public static void startPurchaseIntent(Activity activity, PendingIntent purchaseIntent, Intent intent) {
		if (Compatibility.isStartIntentSenderSupported()) {
			// This is on Android 2.0 and beyond. The in-app buy page activity
			// must be on the activity stack of the application.
			Compatibility.startIntentSender(activity, purchaseIntent.getIntentSender(), intent);
		} else {
			// This is on Android version 1.6. The in-app buy page activity must
			// be on its own separate activity stack instead of on the activity
			// stack of the application.
			try {
				purchaseIntent.send(activity, 0 /* code */, intent);
			} catch (CanceledException e) {
				Log.e(LOG_TAG, "Error starting purchase intent", e);
			}
		}
	}

	static void storeTransaction(Context context, Transaction t) {
		final Transaction t2 = t.clone();
		obfuscate(context, t2);
		TransactionManager.addTransaction(context, t2);
	}

	static void unobfuscate(Context context, List<Transaction> transactions) {
		for (Transaction p : transactions) {
			unobfuscate(context, p);
		}
	}

	/**
	 * Unobfuscate the specified purchase.
	 *
	 * @param context  context
	 * @param purchase purchase to unobfuscate.
	 * @see #obfuscate(Context, Transaction)
	 */
	static void unobfuscate(@NotNull Context context, @NotNull Transaction purchase) {
		final byte[] salt = getSalt();
		if (salt == null) {
			return;
		}
		purchase.orderId = Security.unobfuscate(context, salt, purchase.orderId);
		purchase.productId = Security.unobfuscate(context, salt, purchase.productId);
		purchase.developerPayload = Security.unobfuscate(context, salt, purchase.developerPayload);
	}

	private static boolean verifyNonce(@NotNull JSONObject data) {
		long nonce = data.optLong(JSON_NONCE);
		if (Security.isNonceKnown(nonce)) {
			Security.removeNonce(nonce);
			return true;
		} else {
			return false;
		}
	}

	public static void onRequestPurchaseResponse(@NotNull String itemId, @NotNull ResponseCode response) {
		BillingObserverRegistry.onRequestPurchaseResponse(itemId, response);
	}

	public static void onPurchaseIntent(@NotNull String itemId, @NotNull PendingIntent purchaseIntent) {
		BillingObserverRegistry.onPurchaseIntent(itemId, purchaseIntent);
	}

	public static void onTransactionsRestored() {
		BillingObserverRegistry.onTransactionsRestored();
	}

	public static void registerObserver(@NotNull IBillingObserver billingObserver) {
		BillingObserverRegistry.registerObserver(billingObserver);
	}

	public static void unregisterObserver(@NotNull IBillingObserver billingObserver) {
		BillingObserverRegistry.unregisterObserver(billingObserver);
	}
}
