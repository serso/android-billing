package net.robotmedia.billing.requests;

import android.os.Bundle;
import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.IBillingService;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class CheckBillingSupported extends BillingRequest {

	public CheckBillingSupported(String packageName, int startId) {
		super(packageName, startId);
	}

	@NotNull
	@Override
	public IBillingService.Action getRequestType() {
		return IBillingService.Action.CHECK_BILLING_SUPPORTED;
	}

	@Override
	protected void processOkResponse(Bundle response) {
		final boolean supported = this.isSuccess();
		BillingController.onBillingChecked(supported);
	}

}
