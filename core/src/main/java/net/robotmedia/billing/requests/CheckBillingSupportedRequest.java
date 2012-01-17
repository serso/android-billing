package net.robotmedia.billing.requests;

import android.os.Bundle;
import net.robotmedia.billing.BillingRequestType;
import net.robotmedia.billing.BillingController;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class CheckBillingSupportedRequest extends BillingRequest {

	public CheckBillingSupportedRequest(String packageName, int startId) {
		super(packageName, startId);
	}

	@NotNull
	@Override
	public BillingRequestType getRequestType() {
		return BillingRequestType.CHECK_BILLING_SUPPORTED;
	}

	@Override
	protected void processOkResponse(Bundle response) {
		final boolean supported = this.isSuccess();
		BillingController.onBillingChecked(supported);
	}

}
