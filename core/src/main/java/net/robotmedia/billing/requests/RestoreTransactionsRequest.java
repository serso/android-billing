package net.robotmedia.billing.requests;

import net.robotmedia.billing.BillingRequestType;
import net.robotmedia.billing.BillingController;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class RestoreTransactionsRequest extends BillingRequest {

	public RestoreTransactionsRequest(String packageName, int startId, long nonce) {
		super(packageName, startId, nonce);
	}

	@NotNull
	@Override
	public BillingRequestType getRequestType() {
		return BillingRequestType.RESTORE_TRANSACTIONS;
	}

	@Override public boolean hasNonce() { return true; }

	@Override
	public void onResponseCode(@NotNull ResponseCode response) {
		super.onResponseCode(response);
		if (response == ResponseCode.RESULT_OK) {
			BillingController.onTransactionsRestored();
		}
	}

}
