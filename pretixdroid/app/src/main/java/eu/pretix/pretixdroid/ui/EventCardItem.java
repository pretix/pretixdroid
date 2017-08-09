package eu.pretix.pretixdroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import eu.pretix.pretixdroid.R;

/**
 * is the handler of a card that displays basic information about the event
 */
public class EventCardItem implements EventinfoListItem {

    private EventinfoActivity eventinfoActivity;
    private String eventName;
    private int totalTickets;
    private int alreadyScanned;

    EventCardItem(EventinfoActivity eventinfoActivity, JSONObject json) throws JSONException {
        this.eventinfoActivity = eventinfoActivity;
        this.setData(json);
    }

    public String getEventName() {
        return eventName;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public int getAlreadyScanned() {
        return alreadyScanned;
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
        ((TextView) view.findViewById(R.id.eventTitle)).setText(this.getEventName());
        ((TextView) view.findViewById(R.id.tickets_sold)).setText(String.valueOf(this.getTotalTickets()));
        ((TextView) view.findViewById(R.id.total_scanned)).setText(String.valueOf((this.getAlreadyScanned())));
    }

    @Override
    public void setData(JSONObject json) throws JSONException {
        eventName = json.getJSONObject("event").getString("name");
        totalTickets = json.getInt("total");
        alreadyScanned = json.getInt("checkins");
    }

}
