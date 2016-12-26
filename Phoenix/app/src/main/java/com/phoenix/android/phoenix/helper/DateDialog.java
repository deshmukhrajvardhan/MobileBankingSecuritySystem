package com.phoenix.android.phoenix.helper;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import com.phoenix.android.phoenix.R;

import java.util.Calendar;

/**
 * Created by Akash on 27-11-2016.
 */
public class DateDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener{

    EditText card_expiration_date;
    public DateDialog(View view){
        card_expiration_date = (EditText)view;
    }

    public Dialog onCreateDialog(Bundle savedInstanceDate){
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int date  = c.get(Calendar.DATE);
        return new DatePickerDialog(getActivity(), this,year,month,date);
    }

    public void onDateSet(DatePicker view, int year, int month, int day){
        String date = day + "-" + (month+1) + "-" + year;
        card_expiration_date.setText(date);
    }

}
