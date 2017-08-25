package eu.pretix.pretixdroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

/**
 * is the handler of a card that displays basic information about the event
 */
public class EventCardItem implements EventinfoListItem {

    private EventinfoActivity eventinfoActivity;
    private TicketCheckProvider.StatusResult statusResult;

    EventCardItem(EventinfoActivity eventinfoActivity, TicketCheckProvider.StatusResult statusResult) throws JSONException {
        this.eventinfoActivity = eventinfoActivity;
        this.statusResult = statusResult;
    }

    // --- used for the adapter --- //
    @Override
    public int getType() {
        return EventinfoActivity.EventItemAdapter.TYPE_EVENTCARD;
    }

    @Override
    public View getCard(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.listitem_eventcard, parent, false);
        fillView(v, inflater, parent);
        v.setTag(this);
        return v;
    }

    @Override
    public void fillView(View view, LayoutInflater inflater, ViewGroup parent) {
        ((TextView) view.findViewById(R.id.eventTitle)).setText(statusResult.getEventName());
        ((TextView) view.findViewById(R.id.tickets_sold)).setText(String.valueOf(statusResult.getTotalTickets()));
        ((TextView) view.findViewById(R.id.total_scanned)).setText(String.valueOf((statusResult.getAlreadyScanned())));
    }

}
