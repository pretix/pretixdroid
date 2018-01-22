package eu.pretix.pretixdroid.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.TimePicker;

import org.joda.time.LocalTime;


public class TimePickerField extends android.support.v7.widget.AppCompatEditText {
    LocalTime localTime = new LocalTime();
    java.text.DateFormat dateFormat;
    boolean set = false;

    TimePickerDialog.OnTimeSetListener timeChangeListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker timePicker, int i, int i1) {
            localTime = localTime.withHourOfDay(i).withMinuteOfHour(i1);
            setText(dateFormat.format(localTime.toDateTimeToday().toDate()));
            set = true;
        }
    };

    public TimePickerField(final Context context) {
        super(context);
        dateFormat = android.text.format.DateFormat.getTimeFormat(context);

        setFocusable(false);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(
                        context,
                        timeChangeListener,
                        localTime.getHourOfDay(),
                        localTime.getMinuteOfHour(),
                        android.text.format.DateFormat.is24HourFormat(context)
                ).show();
            }
        });
    }

    public LocalTime getValue() {
        if (!set) {
            return null;
        }
        return localTime;
    }

    public void setValue(LocalTime t) {
        localTime = t;
        set = true;
        setText(dateFormat.format(t.toDateTimeToday().toDate()));
    }
}
