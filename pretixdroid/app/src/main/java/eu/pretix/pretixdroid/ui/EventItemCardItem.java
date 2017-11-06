package eu.pretix.pretixdroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;

import eu.pretix.libpretixsync.check.TicketCheckProvider;
import eu.pretix.pretixdroid.R;

/**
 * is the handler of a card that displays information about each item of an event
 */
public class EventItemCardItem implements EventinfoListItem {

    private EventinfoActivity eventinfoActivity;
    private TicketCheckProvider.StatusResultItem resultItem;

    EventItemCardItem(EventinfoActivity eventinfoActivity, TicketCheckProvider.StatusResultItem resultItem) throws JSONException {
        this.eventinfoActivity = eventinfoActivity;
        this.resultItem = resultItem;
    }

    // --- used for the adapter --- //
    @Override
    public int getType() {
        return EventinfoActivity.EventItemAdapter.TYPE_EVENTITEMCARD;
    }

    @Override
    public View getCard(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.listitem_eventitemcard, parent, false);
        fillView(v, inflater, parent);
        v.setTag(this);
        return v;
    }

    @Override
    public void fillView(View view, LayoutInflater inflater, ViewGroup parent) {
        ((TextView) view.findViewById(R.id.itemTitle)).setText(resultItem.getName());
        ((TextView) view.findViewById(R.id.itemQuantity)).setText(String.valueOf(resultItem.getCheckins()) + "/" + String.valueOf(resultItem.getTotal()));

        ViewGroup variationList = ((ViewGroup) view.findViewById(R.id.variationList));
        variationList.removeAllViews();

        for (TicketCheckProvider.StatusResultItemVariation current : resultItem.getVariations()) {
            View variationLine = inflater.inflate(R.layout.listitem_eventitemvariation, parent, false);
            ((TextView) variationLine.findViewById(R.id.itemVariationTitle)).setText(current.getName());
            ((TextView) variationLine.findViewById(R.id.itemVariationQuantity)).setText(String.valueOf(current.getCheckins()) + "/" + String.valueOf(current.getTotal()));

            variationList.addView(variationLine);
        }
    }
}
