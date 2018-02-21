package eu.pretix.pretixdroid.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.pretix.libpretixsync.check.TicketCheckProvider;
import eu.pretix.libpretixsync.db.AbstractQuestion;
import eu.pretix.libpretixsync.db.Question;
import eu.pretix.libpretixsync.db.QuestionOption;
import eu.pretix.pretixdroid.R;

public class UnpaidOrderDialogHelper {
    public interface RetryHandler {
        public void retry(String secret, List<TicketCheckProvider.Answer> answers, boolean ignore_unpaid);
    }

    public static Dialog showDialog(final Activity ctx, final TicketCheckProvider.CheckResult res,
                                    final String secret, final List<TicketCheckProvider.Answer> answers,
                                    final RetryHandler retryHandler) {

        final Dialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.dialog_unpaid_title)
                .setMessage(R.string.dialog_unpaid_text)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.dialog_unpaid_retry, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        retryHandler.retry(secret, answers, true);
                    }
                }).create();
        dialog.show();
        return dialog;
    }
}
