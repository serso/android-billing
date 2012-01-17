package com.android.vending.billing;

import android.os.Bundle;
import android.os.IBinder;
import org.jetbrains.annotations.NotNull;

/**
 * User: serso
 * Date: 1/17/12
 * Time: 12:07 PM
 */
// to keep IDEA happy
public interface IMarketBillingService {

	public static interface stub {
		IMarketBillingService asInterface(IBinder service);
	}

	public static stub Stub = null;

	@NotNull
    Bundle sendBillingRequest(@NotNull Bundle bundle);
}
