package net.robotmedia.billing.requests;

import android.os.Bundle;
import net.robotmedia.billing.BillingRequestType;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class GetPurchaseInformationRequest extends BillingRequest {

	private String[] notifyIds;

	private static final String KEY_NOTIFY_IDS = "NOTIFY_IDS";

	public GetPurchaseInformationRequest(String packageName, int startId, String[] notifyIds, long nonce) {
		super(packageName,startId, nonce);
		this.notifyIds = notifyIds;
	}

	@Override
	protected void addParams(@NotNull Bundle request) {
		request.putStringArray(KEY_NOTIFY_IDS, notifyIds);
	}

	@NotNull
	@Override
	public BillingRequestType getRequestType() {
		return BillingRequestType.GET_PURCHASE_INFORMATION;
	}

	@Override public boolean hasNonce() { return true; }

}
