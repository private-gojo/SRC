package com.ace.injector.util;

import android.content.*;
import android.widget.*;
import android.support.design.widget.*;
import android.support.v7.widget.*;
import android.support.v7.app.*;
import android.support.v7.app.AlertDialog.*;
import android.support.v7.preference.*;


public class BufferSizeDialog
{
	private AlertDialog.Builder adb;
	private SharedPreferences mPref;

	public BufferSizeDialog(Context c, final Preference p) {
		mPref = PreferenceManager.getDefaultSharedPreferences(c);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        LinearLayout ll = new LinearLayout(c);
        ll.setOrientation(1);
		ll.setPadding(40,0,40,0);
        ll.setLayoutParams(layoutParams);
		final TextInputLayout til = new TextInputLayout(c);
		final AppCompatEditText acet = new AppCompatEditText(c);
		acet.setHint("Send");
		acet.setText(mPref.getString("buffer_send", "16384"));
		til.addView(acet);
		final TextInputLayout til0 = new TextInputLayout(c);
		final AppCompatEditText acet0 = new AppCompatEditText(c);
		acet0.setHint("Receive");
		acet0.setText(mPref.getString("buffer_receive", "32768"));
		til0.addView(acet0);

		ll.addView(til);
		ll.addView(til0);
		adb = new AlertDialog.Builder(c);
		adb.setCancelable(false);
	    adb.setTitle("BufferSize");
	    adb.setMessage("Set the proxy socket buffer size\n\n[WARNING] This is for advanced user only, do not edit this if you don't know what is your doing.");
		adb.setView(ll);
		adb.setPositiveButton("SAVE", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					// TODO: Implement this method
					mPref.edit().putString("buffer_send", acet.getText().toString()).commit();
					mPref.edit().putString("buffer_receive", acet0.getText().toString()).commit();
					p.setSummary(new StringBuffer().append("Send: ").append(mPref.getString("buffer_send", "16384")).append(" | Receive: ").append(mPref.getString("buffer_receive", "32768")));
				}
			});
		adb.setNegativeButton("Cancel", null);
		adb.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					// TODO: Implement this method
					mPref.edit().putString("buffer_send", "16384").commit();
					mPref.edit().putString("buffer_receive", "32768").commit();
					p.setSummary(new StringBuffer().append("Send: ").append(mPref.getString("buffer_send", "16384")).append(" | Receive: ").append(mPref.getString("buffer_receive", "32768")));

				}
			});
	}

	public void show(){
		adb.show();
	}
}
