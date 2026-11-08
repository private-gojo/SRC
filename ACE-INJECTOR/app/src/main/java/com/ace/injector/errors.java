package com.ace.injector;

import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.text.*;
import android.widget.*;
import android.widget.LinearLayout.*;
import com.ace.injectorpro.R;

import android.support.v7.widget.Toolbar;

public class errors extends AppCompatActivity {
    TextView error;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 17;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(layoutParams);
        setContentView(linearLayout);
        toolbar = new Toolbar(this);
        setSupportActionBar(this.toolbar);
        toolbar.setTitle(Html.fromHtml("SocksHttp VPN"));
		toolbar.setBackgroundColor(this.getResources().getColor(R.color.colorPrimary));
        ScrollView sv = new ScrollView(this);
        TextView error = new TextView(this);
        sv.addView(error);
        linearLayout.addView(toolbar);
        linearLayout.addView(sv);
        error.setText(getIntent().getStringExtra("error"));
    }
}






