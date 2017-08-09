package eu.pretix.pretixdroid.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;

import eu.pretix.pretixdroid.R;

/**
 * is the handler of a card that displays information about each item of an event
 */
public class EventItemCardItem implements EventinfoListItem {

    private EventinfoActivity eventinfoActivity;
    private String name;
    private int checkins;
    private int total;
    private int variationCount;

    private final LinkedList<Variation> variations = new LinkedList<>();

    EventItemCardItem(EventinfoActivity eventinfoActivity, JSONObject json) throws JSONException {
        this.eventinfoActivity = eventinfoActivity;
        this.setData(json);
    }

    public String getName() {
        return name;
    }

    public int getCheckins() {
        return checkins;
    }

    public int getTotal() {
        return total;
    }

    public int getVariationCount() {
        return variationCount;
    }

    public LinkedList<Variation> getVariations() {
        return variations;
    }

    private class Variation {
        private final String name;
        private final int checkins;
        private final int total;

        public Variation(JSONObject json) throws JSONException {
            this.name = json.getString("name");
            this.checkins = json.getInt("checkins");
            this.total = json.getInt("total");
        }

        public String getName() {
            return name;
        }

        public int getCheckins() {
            return checkins;
        }

        public int getTotal() {
            return total;
        }
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
        ((TextView) view.findViewById(R.id.itemTitle)).setText(this.getName());
        ((TextView) view.findViewById(R.id.itemQuantity)).setText(String.valueOf(this.getCheckins()) + "/" + String.valueOf(this.getTotal()));

        ViewGroup variationList = ((ViewGroup) view.findViewById(R.id.variationList));
        variationList.removeAllViews();
        Iterator<Variation> iterator = this.variations.iterator();
        while (iterator.hasNext()) {
            Variation current = iterator.next();

            View variationLine = inflater.inflate(R.layout.listitem_eventitemvariation, parent, false);
            ((TextView) variationLine.findViewById(R.id.itemVariationTitle)).setText(current.getName());
            ((TextView) variationLine.findViewById(R.id.itemVariationQuantity)).setText(String.valueOf(current.getCheckins()) + "/" + String.valueOf(current.getTotal()));

            variationList.addView(variationLine);
        }
    }

    @Override
    public void setData(JSONObject json) throws JSONException {
        this.name = json.getString("name");
        this.checkins = json.getInt("checkins");
        this.total = json.getInt("total");

        JSONArray vars = json.getJSONArray("variations");
        this.variationCount = vars.length();

        this.variations.clear();
        for (int i = 0; i < this.variationCount; i++) {
            this.variations.add(new Variation(vars.getJSONObject(i)));
        }
    }
}
