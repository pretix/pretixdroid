package eu.pretix.pretixdroid.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.DatePicker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class DatePickerField extends android.support.v7.widget.AppCompatEditText {
    Calendar cal = Calendar.getInstance();
    DateFormat dateFormat;
    boolean set = false;

    DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, monthOfYear);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            setText(dateFormat.format(cal.getTime()));
            set = true;
        }
    };


    public DatePickerField(final Context context) {
        super(context);
        dateFormat = android.text.format.DateFormat.getDateFormat(context);

        setFocusable(false);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        context,
                        dateChangeListener,
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                ).show();
            }
        });
    }

    public Calendar getValue() {
        if (!set) {
            return null;
        }
        return (Calendar) cal.clone();
    }

    public void setValue(Calendar cal) {
        this.cal = (Calendar) cal.clone();
        set = true;
        setText(dateFormat.format(cal.getTime()));
    }

    public void setValue(Date date) {
        cal.setTime(date);
        set = true;
        setText(dateFormat.format(cal.getTime()));
    }
}
