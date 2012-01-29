package net.robotmedia.billing.example.auxiliary;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import net.robotmedia.billing.example.auxiliary.CatalogEntry.Managed;
import net.robotmedia.billing.model.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter used for displaying a catalog of products. If a product is
 * managed by Android Market and already purchased, then it will be
 * "grayed-out" in the list and not selectable.
 */
public class CatalogAdapter extends ArrayAdapter<String> {
	
	private CatalogEntry[] mCatalog;

	@NotNull
	private List<Transaction> mOwnedItems = new ArrayList<Transaction>();

	public CatalogAdapter(Context context, CatalogEntry[] catalog) {
		super(context, android.R.layout.simple_spinner_item);
		mCatalog = catalog;
		for (CatalogEntry element : catalog) {
			add(context.getString(element.nameId));
		}
		setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}

	@Override
	public boolean areAllItemsEnabled() {
		// Return false to have the adapter call isEnabled()
		return false;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		// If the item at the given list position is not purchasable, then
		// "gray out" the list item.
		View view = super.getDropDownView(position, convertView, parent);
		view.setEnabled(isEnabled(position));
		return view;
	}
	
	private boolean isPurchased(@NotNull String productId) {
		for (Transaction mOwnedItem : mOwnedItems) {
			if (productId.equals(mOwnedItem.productId)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		// If the item at the given list position is not purchasable,
		// then prevent the list item from being selected.
		CatalogEntry entry = mCatalog[position];
		if (entry.managed == Managed.MANAGED && isPurchased(entry.productId)) {
			return false;
		}
		return true;
	}

	public void setOwnedItems(List<Transaction> ownedItems) {
		mOwnedItems = ownedItems;
		notifyDataSetChanged();
	}

}
