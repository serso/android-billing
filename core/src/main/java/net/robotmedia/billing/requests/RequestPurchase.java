package net.robotmedia.billing.requests;

import android.app.PendingIntent;
import android.os.Bundle;
import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.IBillingService;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class RequestPurchase extends BillingRequest {

	private String itemId;
	private String developerPayload;

	private static final String KEY_ITEM_ID = "ITEM_ID";
	private static final String KEY_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";
	private static final String KEY_PURCHASE_INTENT = "PURCHASE_INTENT";

	public RequestPurchase(String packageName, int startId, String itemId, String developerPayload) {
		super(packageName, startId);
		this.itemId = itemId;
		this.developerPayload = developerPayload;
	}

	@Override
	protected void addParams(Bundle request) {
		request.putString(KEY_ITEM_ID, itemId);
		if (developerPayload != null) {
			request.putString(KEY_DEVELOPER_PAYLOAD, developerPayload);
		}
	}

	@NotNull
	@Override
	public IBillingService.Action getRequestType() {
		return IBillingService.Action.REQUEST_PURCHASE;
	}

	@Override
	public void onResponseCode(ResponseCode response) {
		super.onResponseCode(response);
		BillingController.onRequestPurchaseResponse(itemId, response);
	}

	@Override
	protected void processOkResponse(Bundle response) {
		final PendingIntent purchaseIntent = response.getParcelable(KEY_PURCHASE_INTENT);
		BillingController.onPurchaseIntent(itemId, purchaseIntent);
	}


}
