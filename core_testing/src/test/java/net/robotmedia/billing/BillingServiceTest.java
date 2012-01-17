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
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BillingServiceTest extends ServiceTestCase<BillingService> {

	private final static long NONCE = 147;
	private final static String ITEM_ID = "android.test.purchased";

	public BillingServiceTest() {
		super(BillingService.class);
	}
	
	@SmallTest
	public void testStart() throws Exception {
		final Intent intent = new Intent();
		intent.setClass(getContext(), BillingService.class);
		startService(intent);
	}
	
	@SmallTest
	public void testCheckBillingSupported() throws Exception {
		BillingService.checkBillingSupported(getContext());
	}

	@SmallTest
	public void testConfirmNotifications() throws Exception {
		BillingService.confirmNotifications(getContext(), new String[]{"test"});
	}

	@SmallTest
	public void testGetPurchaseInformation() throws Exception {
		BillingService.getPurchaseInformation(getContext(), new String[]{"test"}, NONCE);
	}	

	@SmallTest
	public void testRequestPurchase() throws Exception {
		BillingService.requestPurchase(getContext(), ITEM_ID, null);
	}	

	@SmallTest
	public void testRestoreTransactions() throws Exception {
		BillingService.restoreTransactions(getContext(), NONCE);
	}	

	@SmallTest
    public void testBind() {
        final Intent intent = new Intent();
        intent.setClass(getContext(), BillingService.class);
        final IBinder service = bindService(intent); 
        assertNull(service);
    }


	@SmallTest
	public void testSynchronization() throws Exception {
		final int TEST_COUNT = 100;
		final CountDownLatch latch = new CountDownLatch(1);

		final CountDownLatch testCompletedLatch = new CountDownLatch(TEST_COUNT);

		for ( int i = 0; i < TEST_COUNT; i++ ) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						latch.await();
						BillingService.requestPurchase(getContext(), ITEM_ID, null);
					} catch (InterruptedException e) {
					} finally {
						testCompletedLatch.countDown();
					}
				}
			}).start();
		}

		latch.countDown();

		if ( !testCompletedLatch.await(3, TimeUnit.SECONDS) ) {
			Assert.fail();
		}
	}
}