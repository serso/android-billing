package net.robotmedia.billing.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.ResponseCode;
import net.robotmedia.billing.example.auxiliary.CatalogAdapter;
import net.robotmedia.billing.example.auxiliary.CatalogEntry;
import net.robotmedia.billing.helper.AbstractBillingObserver;
import net.robotmedia.billing.model.Transaction;
import net.robotmedia.billing.model.Transaction.PurchaseState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A sample application based on the original Dungeons to demonstrate how to use
 * BillingController and implement IBillingObserver.
 */
public class Dungeons extends Activity {

	private static final String TAG = "Dungeons";

	private Button mBuyButton;

	private ListView ownedItems;

	private ListView allTransactions;

	private static final int DIALOG_BILLING_NOT_SUPPORTED_ID = 2;

	private String productId;

	private CatalogAdapter mCatalogAdapter;

	private AbstractBillingObserver mBillingObserver;

	private Dialog createDialog(int titleId, int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(titleId).setIcon(android.R.drawable.stat_sys_warning).setMessage(messageId).setCancelable(
				false).setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}

	public void onBillingChecked(boolean supported) {
		if (supported) {
			restoreTransactions();
			mBuyButton.setEnabled(true);
		} else {
			showDialog(DIALOG_BILLING_NOT_SUPPORTED_ID);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBillingObserver = new AbstractBillingObserver(this) {

			@Override
			public void onCheckBillingSupportedResponse(boolean supported) {
				Dungeons.this.onBillingChecked(supported);
			}

			@Override
			public void onPurchaseIntentFailure(@NotNull String productId, @NotNull ResponseCode responseCode) {
				// do nothing
			}

			@Override
			public void onPurchaseStateChanged(@NotNull String productId, @NotNull PurchaseState state) {
				Dungeons.this.onPurchaseStateChanged(productId, state);
			}

			@Override
			public void onRequestPurchaseResponse(@NotNull String productId, @NotNull ResponseCode response) {
				Dungeons.this.onRequestPurchaseResponse(productId, response);
			}
		};
		
		setContentView(R.layout.main);

		setupWidgets();
		BillingController.registerObserver(mBillingObserver);
		BillingController.checkBillingSupported(this);
		updateTransactions();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_BILLING_NOT_SUPPORTED_ID:
			return createDialog(R.string.billing_not_supported_title, R.string.billing_not_supported_message);
		default:
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		BillingController.unregisterObserver(mBillingObserver);
		super.onDestroy();
	}

	public void onPurchaseStateChanged(String productId, PurchaseState state) {
		Log.i(TAG, "onPurchaseStateChanged() productId: " + productId + ", state: " + state);
		Toast.makeText(this, "onPurchaseStateChanged() productId: " + productId + ", state: " + state, Toast.LENGTH_SHORT).show();
		updateTransactions();
	}

	public void onRequestPurchaseResponse(String productId, ResponseCode response) {
		Log.i(TAG, "onRequestPurchaseResponse() productId: " + productId + ", response: " + response);
		Toast.makeText(this, "onRequestPurchaseResponse() productId: " + productId + ", response: " + response, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Restores previous transactions, if any. This happens if the application
	 * has just been installed or the user wiped data. We do not want to do this
	 * on every startup, rather, we want to do only when the database needs to
	 * be initialized.
	 */
	private void restoreTransactions() {
		if (!mBillingObserver.isTransactionsRestored()) {
			BillingController.restoreTransactions(this);
			Toast.makeText(this, R.string.restoring_transactions, Toast.LENGTH_LONG).show();
		}
	}

	private void setupWidgets() {
		mBuyButton = (Button) findViewById(R.id.buy_button);
		mBuyButton.setEnabled(false);
		mBuyButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(Dungeons.this, "Purchasing " + productId, Toast.LENGTH_SHORT).show();
				BillingController.requestPurchase(Dungeons.this, productId, true /* confirm */);
			}
		});

		final Spinner mSelectItemSpinner = (Spinner) findViewById(R.id.item_choices);
		mCatalogAdapter = new CatalogAdapter(this, CatalogEntry.CATALOG);
		mSelectItemSpinner.setAdapter(mCatalogAdapter);
		mSelectItemSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				productId = CatalogEntry.CATALOG[position].productId;
			}

			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});

		ownedItems = (ListView) findViewById(R.id.owned_items);
		allTransactions = (ListView) findViewById(R.id.all_transactions);
	}

	private void updateTransactions() {
		final List<Transaction> items = BillingController.getTransactions(this);

		Toast.makeText(this, items.size() + " items found!", Toast.LENGTH_SHORT).show();

		final List<String> ownedItems = new ArrayList<String>();
		for (Transaction t : items) {
			if (t.purchaseState == PurchaseState.PURCHASED) {
				ownedItems.add(t.productId);
			}
		}

		Toast.makeText(this, ownedItems.size() + " purchased items found!", Toast.LENGTH_SHORT).show();

		mCatalogAdapter.setOwnedItems(ownedItems);

		this.ownedItems.setAdapter(new ArrayAdapter<String>(this, R.layout.item_row, R.id.item_name, ownedItems));
		this.allTransactions.setAdapter(new ArrayAdapter<Transaction>(this, R.layout.item_row, R.id.item_name, items));
	}
}