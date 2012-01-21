package net.robotmedia.billing.requests;

import android.os.Bundle;
import net.robotmedia.billing.BillingRequestType;
import org.jetbrains.annotations.NotNull;

/**
* User: serso
* Date: 1/17/12
* Time: 12:45 PM
*/
public class ConfirmNotificationsRequest extends BillingRequest {

	@NotNull
	private final String[] notifyIds;

	private static final String KEY_NOTIFY_IDS = "NOTIFY_IDS";

	public ConfirmNotificationsRequest(@NotNull String packageName, int startId, @NotNull String[] notifyIds) {
		super(packageName, startId);
		this.notifyIds = notifyIds;
	}

	@Override
	protected void addParams(@NotNull Bundle request) {
		request.putStringArray(KEY_NOTIFY_IDS, notifyIds);
	}

	@NotNull
	@Override
	public BillingRequestType getRequestType() {
		return BillingRequestType.CONFIRM_NOTIFICATIONS;
	}

}
