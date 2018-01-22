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

public class QuestionDialogHelper {
    public interface RetryHandler {
        public void retry(String secret, List<TicketCheckProvider.Answer> answers);
    }

    static class OptionAdapter extends ArrayAdapter<QuestionOption> {
        public OptionAdapter(Context context, List<QuestionOption> objects) {
            super(context, R.layout.spinneritem_simple, objects);
        }
    }

    public static void addError(Context ctx, Object f, TextView label, int strid) {
        if (f instanceof EditText) {
            ((EditText) f).setError(strid == 0 ? null : ctx.getString(strid));
        } else if (f instanceof List && ((List) f).get(0) instanceof EditText) {
            ((List<EditText>) f).get(1).setError(strid == 0 ? null : ctx.getString(strid));
        } else if (label != null) {
            label.setError(strid == 0 ? null : ctx.getString(strid));
        }

    }

    public static Dialog showDialog(final Activity ctx, final TicketCheckProvider.CheckResult res,
                                    final String secret, final RetryHandler retryHandler) {
        LayoutInflater inflater = ctx.getLayoutInflater();
        final Map<Question, Object> fviews = new HashMap<>();
        final Map<Question, TextView> labels = new HashMap<>();
        final SimpleDateFormat hf = new SimpleDateFormat("HH:mm", Locale.US);
        final SimpleDateFormat wf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        View view = inflater.inflate(R.layout.dialog_questions, null);
        final LinearLayout llFormFields = view.findViewById(R.id.llFormFields);

        for (TicketCheckProvider.RequiredAnswer ra : res.getRequiredAnswers()) {
            TextView tv = new TextView(ctx);
            tv.setText(ra.getQuestion().getQuestion());
            llFormFields.addView(tv);
            labels.put(ra.getQuestion(), tv);

            switch (ra.getQuestion().getType()) {
                case S:
                    EditText fieldS = new EditText(ctx);
                    fieldS.setText(ra.getCurrentValue());
                    fieldS.setLines(1);
                    fieldS.setSingleLine(true);
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
                    EditText fieldN = new EditText(ctx);
                    fieldN.setText(ra.getCurrentValue());
                    fieldN.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    fieldN.setSingleLine(true);
                    fieldN.setLines(1);
                    fviews.put(ra.getQuestion(), fieldN);
                    llFormFields.addView(fieldN);
                    break;
                case B:
                    CheckBox fieldB = new CheckBox(ctx);
                    fieldB.setText(R.string.yes);
                    fieldB.setChecked("True".equals(ra.getCurrentValue()));
                    fviews.put(ra.getQuestion(), fieldB);
                    llFormFields.addView(fieldB);
                    break;
                case F:
                    break;
                case M:
                    List<CheckBox> fields = new ArrayList<>();
                    List<String> selected = Arrays.asList(ra.getCurrentValue().split(","));
                    for (QuestionOption opt : ra.getQuestion().getOptions()) {
                        CheckBox field = new CheckBox(ctx);
                        field.setText(opt.getValue());
                        field.setTag(opt.getServer_id());
                        if (selected.contains(opt.getServer_id().toString())) {
                            field.setChecked(true);
                        }
                        fields.add(field);
                        llFormFields.addView(field);
                    }
                    fviews.put(ra.getQuestion(), fields);
                    break;
                case C:
                    Spinner fieldC = new Spinner(ctx);
                    List<QuestionOption> opts = ra.getQuestion().getOptions();
                    QuestionOption emptyOpt = new QuestionOption();
                    emptyOpt.setServer_id(0L);
                    emptyOpt.setValue("");
                    opts.add(0, emptyOpt);
                    fieldC.setAdapter(new OptionAdapter(ctx, opts));
                    int i = 0;
                    for (QuestionOption opt : ra.getQuestion().getOptions()) {
                        if (opt.getServer_id().toString().equals(ra.getCurrentValue())) {
                            fieldC.setSelection(i);
                            break;
                        }
                        i++;
                    }
                    fviews.put(ra.getQuestion(), fieldC);
                    llFormFields.addView(fieldC);
                    break;
                case D:
                    DatePickerField fieldD = new DatePickerField(ctx);
                    if (ra.getCurrentValue().length() > 0) {
                        try {
                            fieldD.setValue(df.parse(ra.getCurrentValue()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    fviews.put(ra.getQuestion(), fieldD);
                    llFormFields.addView(fieldD);
                    break;
                case H:
                    TimePickerField fieldH = new TimePickerField(ctx);
                    fviews.put(ra.getQuestion(), fieldH);
                    try {
                        fieldH.setValue(LocalTime.fromDateFields(hf.parse(ra.getCurrentValue())));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    llFormFields.addView(fieldH);
                    break;
                case W:
                    List<EditText> fieldsW = new ArrayList<>();
                    LinearLayout llInner = new LinearLayout(ctx);
                    llInner.setOrientation(LinearLayout.HORIZONTAL);

                    DatePickerField fieldWD = new DatePickerField(ctx);
                    fieldWD.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                    fieldsW.add(fieldWD);
                    llInner.addView(fieldWD);

                    TimePickerField fieldWH = new TimePickerField(ctx);
                    fieldWH.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                    fieldsW.add(fieldWH);
                    llInner.addView(fieldWH);

                    if (ra.getCurrentValue().length() > 0) {
                        try {
                            fieldWD.setValue(wf.parse(ra.getCurrentValue()));
                            fieldWH.setValue(LocalTime.fromDateFields(wf.parse(ra.getCurrentValue())));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    fviews.put(ra.getQuestion(), fieldsW);
                    llFormFields.addView(llInner);
                    break;
            }
        }

        final Dialog dialog = new AlertDialog.Builder(ctx)
                .setView(view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Dummy. We set this below since we don't want to auto-close tis
                    }
                }).create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        final List<TicketCheckProvider.Answer> answers = new ArrayList<>();
                        boolean has_errors = false;

                        for (TicketCheckProvider.RequiredAnswer ra : res.getRequiredAnswers()) {
                            String answer = "";
                            boolean empty = false;
                            Object field = fviews.get(ra.getQuestion());

                            switch (ra.getQuestion().getType()) {
                                case S:
                                case T:
                                    answer = ((EditText) field).getText().toString();
                                    empty = answer.trim().equals("");
                                    break;
                                case N:
                                    answer = ((EditText) field).getText().toString();
                                    empty = answer.trim().equals("");
                                    break;
                                case B:
                                    answer = ((CheckBox) field).isChecked() ? "True" : "False";
                                    empty = answer.equals("False");
                                    break;
                                case F:
                                    empty = true;
                                    break;
                                case M:
                                    empty = true;
                                    StringBuilder aw = new StringBuilder();
                                    for (CheckBox f : (List<CheckBox>) field) {
                                        if (f.isChecked()) {
                                            if (!empty) {
                                                aw.append(",");
                                            }
                                            aw.append(f.getTag());
                                            empty = false;
                                        }
                                    }
                                    answer = aw.toString();
                                    break;
                                case C:
                                    QuestionOption opt = ((QuestionOption) ((Spinner) field).getSelectedItem());
                                    if (opt.getServer_id() == 0) {
                                        empty = true;
                                    } else {
                                        answer = opt.getServer_id().toString();
                                    }
                                    break;
                                case D:
                                    empty = (((DatePickerField) field).getValue() == null);
                                    if (!empty) {
                                        answer = df.format(((DatePickerField) field).getValue().getTime());
                                    }
                                    break;
                                case H:
                                    empty = (((TimePickerField) field).getValue() == null);
                                    if (!empty) {
                                        answer = hf.format(((TimePickerField) field).getValue().toDateTimeToday().toDate());
                                    }
                                    break;
                                case W:
                                    List<EditText> fieldset = (List<EditText>) field;
                                    empty = (
                                            ((DatePickerField) fieldset.get(0)).getValue() == null
                                                    || ((TimePickerField) fieldset.get(1)).getValue() == null
                                    );
                                    if (!empty) {
                                        answer = wf.format(
                                                LocalDate.fromCalendarFields(((DatePickerField) fieldset.get(0)).getValue()).toDateTime(
                                                        ((TimePickerField) fieldset.get(1)).getValue()
                                                ).toDate()
                                        );
                                    }
                                    break;
                            }

                            if (empty && ra.getQuestion().isRequired()) {
                                has_errors = true;
                                addError(ctx, field, labels.get(ra.getQuestion()), R.string.question_input_required);
                            } else if (empty) {
                                answers.add(new TicketCheckProvider.Answer(ra.getQuestion(), ""));
                                addError(ctx, field, labels.get(ra.getQuestion()), 0);
                            } else {
                                try {
                                    ra.getQuestion().clean_answer(answer, ra.getQuestion().getOptions());
                                    addError(ctx, field, labels.get(ra.getQuestion()), 0);
                                } catch (AbstractQuestion.ValidationException e) {
                                    has_errors = true;
                                    addError(ctx, field, labels.get(ra.getQuestion()), R.string.question_input_invalid);
                                }
                                answers.add(new TicketCheckProvider.Answer(ra.getQuestion(), answer));
                            }
                        }
                        if (!has_errors) {
                            dialog.dismiss();
                            retryHandler.retry(secret, answers);
                        } else {
                            Toast.makeText(ctx, R.string.question_validation_error, Toast.LENGTH_SHORT);
                        }
                    }
                });
            }
        });
        dialog.show();
        return dialog;
    }
}
