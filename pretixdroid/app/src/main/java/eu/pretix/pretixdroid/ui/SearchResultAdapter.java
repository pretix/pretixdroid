package eu.pretix.pretixdroid.ui;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.check.TicketCheckProvider;

public class SearchResultAdapter extends ArrayAdapter<TicketCheckProvider.SearchResult> {

    private Context context;
    private int resource;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(resource, parent, false);
        } else {
            view = convertView;
        }
        TicketCheckProvider.SearchResult item = getItem(position);

        TextView tvSecret = (TextView) view.findViewById(R.id.tvSecret);
        TextView tvOrderCode = (TextView) view.findViewById(R.id.tvOrderCode);
        TextView tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        TextView tvAttendeeName = (TextView) view.findViewById(R.id.tvAttendeeName);
        TextView tvTicketName = (TextView) view.findViewById(R.id.tvTicketName);
        ImageView ivWarning = (ImageView) view.findViewById(R.id.ivWarning);
        View rlResult = view.findViewById(R.id.rlResult);

        tvSecret.setText(item.getSecret());
        tvOrderCode.setText(item.getOrderCode());
        if (item.getAttendee_name() != null && !item.getAttendee_name().equals("null")) {
            tvAttendeeName.setText(item.getAttendee_name());
        }
        tvTicketName.setText(item.getTicket() + (
                item.getVariation() != null && !item.getVariation().equals("null")
                        ? " - " + item.getVariation() : ""
        ));
        ivWarning.setVisibility(item.isRequireAttention() ? View.VISIBLE : View.GONE);
        if (item.isRedeemed()) {
            tvStatus.setText(R.string.status_redeemed);
            rlResult.setBackgroundColor(ContextCompat.getColor(context, R.color.scan_result_warn));
        } else if (!item.isPaid()) {
            tvStatus.setText(R.string.status_unpaid);
            rlResult.setBackgroundColor(ContextCompat.getColor(context, R.color.scan_result_err));
        } else {
            tvStatus.setText(R.string.status_valid);
            rlResult.setBackgroundColor(ContextCompat.getColor(context, R.color.scan_result_ok));
        }

        return view;
    }

    public SearchResultAdapter(Context context, int resource,
                               int textViewResourceId, List<TicketCheckProvider.SearchResult> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.resource = resource;
    }

}
