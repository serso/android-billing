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

package net.robotmedia.billing.model;

import android.content.Context;
import android.database.Cursor;
import net.robotmedia.billing.model.Transaction.PurchaseState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TransactionManager {

	public synchronized static void addTransaction(@NotNull Context context, @NotNull Transaction transaction) {
		final BillingDB db = new BillingDB(context);
		try {
			db.insert(transaction);
		} finally {
			db.close();
		}
	}

	public synchronized static boolean isPurchased(@NotNull Context context, @NotNull String productId) {
		return countPurchases(context, productId) > 0;
	}

	public synchronized static int countPurchases(@NotNull Context context, @NotNull String productId) {
		return doDatabaseOperation(context, new CountPurchases(productId));
	}

	@NotNull
	public synchronized static List<Transaction> getTransactions(@NotNull Context context) {
		return doDatabaseOperation(context, new TransactionsByProductId(null));
	}

	@NotNull
	private static List<Transaction> getTransactionsFromCursor(@NotNull final Cursor cursor) {
		final List<Transaction> result = new ArrayList<Transaction>();

		while (cursor.moveToNext()) {
			result.add(BillingDB.createTransaction(cursor));
		}

		return result;
	}

	@NotNull
	public synchronized static List<Transaction> getTransactions(@NotNull Context context, @NotNull String productId) {
		return doDatabaseOperation(context, new TransactionsByProductId(productId));
	}

	private static class CountPurchases implements DatabaseOperation<Integer> {

		@NotNull
		private final String productId;

		public CountPurchases(@NotNull String productId) {
			this.productId = productId;
		}

		@NotNull
		@Override
		public Cursor createCursor(@NotNull BillingDB db) {
			return db.getTransactionsQuery(productId, PurchaseState.PURCHASED);
		}

		@NotNull
		@Override
		public Integer doOperation(@NotNull Cursor cursor) {
			return cursor.getCount();
		}
	}

	private static class TransactionsByProductId implements DatabaseOperation<List<Transaction>> {

		@Nullable
		private final String productId;

		public TransactionsByProductId(@Nullable String productId) {
			this.productId = productId;
		}

		@NotNull
		@Override
		public Cursor createCursor(@NotNull BillingDB db) {
			if (productId != null) {
				return db.getTransactionsQuery(productId);
			} else {
				return db.getAllTransactionsQuery();
			}
		}

		@NotNull
		@Override
		public List<Transaction> doOperation(@NotNull Cursor cursor) {
			return getTransactionsFromCursor(cursor);
		}
	}

	private static interface DatabaseOperation<T> {

		@NotNull
		Cursor createCursor(@NotNull BillingDB db);

		@NotNull
		T doOperation(@NotNull Cursor cursor);
	}

	@NotNull
	private static <T> T doDatabaseOperation(@NotNull Context context, @NotNull DatabaseOperation<T> operation) {
		final T result;

		BillingDB db = null;
		try {
			// open database
			db = new BillingDB(context);

			Cursor cursor = null;
			try {
				// open cursor
				cursor = operation.createCursor(db);
				// do operation
				result = operation.doOperation(cursor);
			} finally {
				// anyway if cursor was opened - close it
				if (cursor != null) {
					cursor.close();
				}
			}
		} finally {
			// anyway if database was opened - close it
			if (db != null) {
				db.close();
			}
		}

		return result;
	}

}
