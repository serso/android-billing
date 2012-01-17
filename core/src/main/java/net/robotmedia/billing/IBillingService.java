package net.robotmedia.billing;

import android.content.Context;
import android.content.Intent;
import net.robotmedia.billing.requests.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: serso
 * Date: 1/17/12
 * Time: 12:39 PM
 */
public interface IBillingService {

	void runRequestOrQueue(@NotNull IBillingRequest request);

	String getPackageName();

}
