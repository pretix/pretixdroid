package eu.pretix.pretixdroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * interface of all CardItems that are displayed on the eventinfo page
 */
public interface EventinfoListItem {
    /**
     * returns an integer for the adapter to distinguish between cards
     *
     * @return an integer for the adapter to distinguish between cards
     */
    int getType();

    /**
     * returns a newly inflated card with the content of this item
     *
     * @param inflater the inflater to use
     * @param parent   the parent ViewGroup
     * @return a newly inflated card with the content of this item
     */
    View getCard(LayoutInflater inflater, ViewGroup parent);

    /**
     * returns a recycled view filled with the contents of this item
     *
     * @param view a recycled view filled with the contents of this item
     */
    void fillView(View view, LayoutInflater inflater, ViewGroup parent);
}
