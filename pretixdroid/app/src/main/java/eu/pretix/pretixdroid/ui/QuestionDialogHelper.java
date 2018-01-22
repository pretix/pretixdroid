package eu.pretix.pretixdroid.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.pretix.libpretixsync.check.TicketCheckProvider;
import eu.pretix.libpretixsync.db.Question;
import eu.pretix.pretixdroid.R;

public class QuestionDialogHelper {
    public interface RetryHandler {
        public void retry(String secret, List<TicketCheckProvider.Answer> answers);
    }

    public static void showDialog(Activity ctx, TicketCheckProvider.CheckResult res) {
        LayoutInflater inflater = ctx.getLayoutInflater();
        final Map<Question, Object> fviews = new HashMap<>();

        View view = inflater.inflate(R.layout.dialog_questions, null);
        LinearLayout llFormFields = view.findViewById(R.id.llFormFields);

        for (TicketCheckProvider.RequiredAnswer ra : res.getRequiredAnswers()) {
            TextView tv = new TextView(ctx);
            tv.setText(ra.getQuestion().getQuestion());
            llFormFields.addView(tv);

            switch (ra.getQuestion().getType()) {
                case S:
                    EditText fieldS = new EditText(ctx);
                    fieldS.setText(ra.getCurrentValue());
                    fieldS.setLines(1);
                    fviews.put(ra.getQuestion(), fieldS);
                    llFormFields.addView(fieldS);
                    break;
                case T:
                    EditText fieldT = new EditText(ctx);
                    fieldT.setText(ra.getCurrentValue());
                    fieldT.setLines(2);
                    fviews.put(ra.getQuestion(), fieldT);
                    llFormFields.addView(fieldT);
                    break;
                case N:
                    // TODO: decimal places?
                    EditText fieldN = new EditText(ctx);
                    fieldN.setText(ra.getCurrentValue());
                    fieldN.setInputType(InputType.TYPE_CLASS_NUMBER);
                    fieldN.setLines(1);
                    fviews.put(ra.getQuestion(), fieldN);
                    llFormFields.addView(fieldN);
                    break;
                case B:
                    CheckBox fieldB = new CheckBox(ctx);
                    fieldB.setText(ra.getQuestion().getQuestion());
                    fieldB.setChecked("True".equals(ra.getCurrentValue()));
                    fviews.put(ra.getQuestion(), fieldB);
                    llFormFields.addView(fieldB);
                    break;
                case F:
                    break;
                case M:
                    break;
                case C:
                    break;
                case D:
                    break;
                case H:
                    break;
                case W:
                    break;
            }
        }

        new AlertDialog.Builder(ctx)
                .setView(view)
                .setPositiveButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}
