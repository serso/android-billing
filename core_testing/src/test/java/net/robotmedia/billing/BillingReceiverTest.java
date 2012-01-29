/*   Copyright 2011 Robot Media SL (http://www.robotmedia.net)
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package net.robotmedia.billing;

import android.content.Intent;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class BillingReceiverTest extends AndroidTestCase {

	private BillingReceiver mReceiver;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mReceiver = new BillingReceiver();
	}

	@SmallTest
	public void testNotify() throws Exception {
		final Intent intent = new Intent(BillingResponseType.IN_APP_NOTIFY.toIntentAction());
		intent.putExtra(BillingResponseType.EXTRA_NOTIFICATION_ID, "notificationId");
		mReceiver.onReceive(getContext(), intent);
	}
	
	@SmallTest
	public void testResponseCode() throws Exception {
		final Intent intent = new Intent(BillingResponseType.RESPONSE_CODE.toIntentAction());
		intent.putExtra(BillingResponseType.EXTRA_REQUEST_ID, "requestId");
		intent.putExtra(BillingResponseType.EXTRA_RESPONSE_CODE, ResponseCode.RESULT_OK.ordinal());
		mReceiver.onReceive(getContext(), intent);
	}
	
	@SmallTest
	public void testPurchaseStateChanged() throws Exception {
		final Intent intent = new Intent(BillingResponseType.PURCHASE_STATE_CHANGED.toIntentAction());
		intent.putExtra(BillingResponseType.EXTRA_INAPP_SIGNED_DATA, "");
		intent.putExtra(BillingResponseType.EXTRA_INAPP_SIGNATURE, "");
		mReceiver.onReceive(getContext(), intent);
	}

}
