package net.robotmedia.billing.requests;

import android.os.Bundle;
import net.robotmedia.billing.IBillingService;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class ConfirmNotifications extends BillingRequest {

	private String[] notifyIds;

	private static final String KEY_NOTIFY_IDS = "NOTIFY_IDS";

	public ConfirmNotifications(String packageName, int startId, String[] notifyIds) {
		super(packageName, startId);
		this.notifyIds = notifyIds;
	}

	@Override
	protected void addParams(Bundle request) {
		request.putStringArray(KEY_NOTIFY_IDS, notifyIds);
	}

	@NotNull
	@Override
	public IBillingService.Action getRequestType() {
		return IBillingService.Action.CONFIRM_NOTIFICATIONS;
	}

}
