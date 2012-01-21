package net.robotmedia.billing.requests;

import android.app.PendingIntent;
import android.os.Bundle;
import net.robotmedia.billing.BillingRequestType;
import net.robotmedia.billing.BillingController;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class RequestPurchase extends BillingRequest {

	private String productId;
	private String developerPayload;

	private static final String KEY_ITEM_ID = "ITEM_ID";
	private static final String KEY_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";
	private static final String KEY_PURCHASE_INTENT = "PURCHASE_INTENT";

	public RequestPurchase(String packageName, int startId, String productId, String developerPayload) {
		super(packageName, startId);
		this.productId = productId;
		this.developerPayload = developerPayload;
	}

	@Override
	protected void addParams(@NotNull Bundle request) {
		request.putString(KEY_ITEM_ID, productId);
		if (developerPayload != null) {
			request.putString(KEY_DEVELOPER_PAYLOAD, developerPayload);
		}
	}

	@NotNull
	@Override
	public BillingRequestType getRequestType() {
		return BillingRequestType.REQUEST_PURCHASE;
	}

	@Override
	public void onResponseCode(@NotNull ResponseCode response) {
		super.onResponseCode(response);
		BillingController.onRequestPurchaseResponse(productId, response);
	}

	@Override
	protected void processOkResponse(@NotNull Bundle response) {
		final PendingIntent purchaseIntent = response.getParcelable(KEY_PURCHASE_INTENT);
		BillingController.onPurchaseIntent(productId, purchaseIntent);
	}

	@Override
	protected void processNotOkResponse(@NotNull Bundle response, @NotNull ResponseCode responseCode) {
		BillingController.onPurchaseIntentFailure(productId, responseCode);
	}
}
