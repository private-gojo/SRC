package com.ace.injector.fragments;


import android.support.v4.app.DialogFragment;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import com.ace.injectorpro.*;
import android.view.View;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import com.ace.injector.SocksHttpMainActivity;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.ace.injectorpro.R;
import android.view.ViewGroup;
import android.widget.Button;
import com.ultrasshservice.config.Settings;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatCheckBox;
import android.content.SharedPreferences;
import android.widget.TextView;

public class CustomSniFragments extends DialogFragment
implements View.OnClickListener {

    private Settings mConfig;

    private boolean usarProxyAutenticacao;
    private boolean usarPayloadPadrao;

    private TextInputEditText customSNI;
	private TextView custom;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);

        mConfig = new Settings(getContext());
        SharedPreferences prefs = mConfig.getPrefsPrivate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        getDialog().setCanceledOnTouchOutside(false);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        LayoutInflater li = LayoutInflater.from(getContext());
        View view = li.inflate(R.layout.custom_sni, null); 

        customSNI = view.findViewById(R.id.fragment_custom_sni);

        Button cancelButton = view.findViewById(R.id.fragment_sni_remoteCancelButton);
        Button saveButton = view.findViewById(R.id.fragment_sni_remoteSaveButton);

        cancelButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        customSNI.setText(mConfig.getPrivString(Settings.CUSTOM_SNI));

        //setCancelable(false);

        return new AlertDialog.Builder(getActivity())
            . setTitle("Custom SNI")
            .setView(view)

            . show();
    }

    /**
     * onClick implementação
     */

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
            case R.id.fragment_sni_remoteSaveButton:
                String mCustomSNI = customSNI.getEditableText().toString();

                SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();

                edit.putString(Settings.CUSTOM_SNI, mCustomSNI);

                edit.apply();


                SocksHttpMainActivity.updateMainViews(getContext());

                dismiss();

                break;

            case R.id.fragment_sni_remoteCancelButton:
                dismiss();
                break;
        }
    }

}
