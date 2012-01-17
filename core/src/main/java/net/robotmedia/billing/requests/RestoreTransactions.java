package net.robotmedia.billing.requests;

import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.IBillingService;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class RestoreTransactions extends BillingRequest {

	public RestoreTransactions(String packageName, int startId) {
		super(packageName, startId);
	}

	@NotNull
	@Override
	public IBillingService.Action getRequestType() {
		return IBillingService.Action.RESTORE_TRANSACTIONS;
	}

	@Override public boolean hasNonce() { return true; }

	@Override
	public void onResponseCode(ResponseCode response) {
		super.onResponseCode(response);
		if (response == ResponseCode.RESULT_OK) {
			BillingController.onTransactionsRestored();
		}
	}

}
