package com.ace.injector.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import com.ace.injectorpro.R;
import android.support.design.widget.TextInputLayout;
import android.support.design.widget.TextInputEditText;
public class PayloadGenerator {

    private static ArrayAdapter<String> rAdapter, iAdapter, sAdapter;
    private static CheckBox cbBack;
    private static CheckBox cbDual;
    private static CheckBox cbForward;
    private static CheckBox cbFront;
    private static CheckBox cbKeep;
    private static CheckBox cbOnline;
    private static CheckBox cbRaw;
    private static CheckBox cbReferer;
    private static CheckBox cbReverse;
    private static CheckBox cbRotate;
    private static CheckBox cbUser;
    private static TextInputEditText payload;
    private static TextInputLayout editTextHint;
    private static LayoutInflater inflater;
    private static RadioButton rNomal;
    private static RadioButton rSplit;
    private static RadioButton rDirect;
    private static Spinner injectSpin;
    private static Spinner requestSpin;
    private static Spinner splitSpin;
    private static String[] inject_items = new String[]{"Normal", "Front Inject", "Back Inject"};
    private static String[] request_items = new String[]{ "CONNECT", "GET", "POST", "PUT", "HEAD", "TRACE", "OPTIONS", "PATCH", "PROPATCH", "DELETE"};
    private static String[] split_items = new String[]{"Normal","Instant Split", "Delay Split"};
    private static Context context;
    private static View v;
    private static SharedPreferences prefs;
    private static SharedPreferences.Editor editor;
    private OnGenerateListener generateListener;
    private OnCancelListener cancelListener;
    private OnNeutralListener neutralButtonListener;
    private CharSequence generateButtonName;
    private CharSequence cancelButtonName;
    private CharSequence neutralButtonName;
    private String title;
    public PayloadGenerator(Context context) {
        this.context = context;
    }

    /** interface for positive button **/
    public interface OnGenerateListener{
        void onGenerate(String payloadGenerated);
    }

    /** interface for negative button **/
    public interface OnCancelListener {
        void onCancelListener();
    }

    /** interface for neutral button **/
    public interface OnNeutralListener {
        void onNeutralListener();
    }

    /**
     * This should not be null
     * This serves as your positive button
     **/
    public void setGenerateListener(CharSequence generateButtonName, OnGenerateListener generateListener) {
        this.generateButtonName = generateButtonName;
        this.generateListener = generateListener;
    }

    /**
     * This should not be null
     * This serves as your negative button
     **/
    public void setCancelListener(CharSequence cancelButtonName, OnCancelListener cancelListener) {
        this.cancelButtonName = cancelButtonName;
        this.cancelListener = cancelListener;
    }

    /**
     * This should not be null
     * This serves as your neutral button
     **/
    public void setNeutralButtonListener(CharSequence neutralButtonName, OnNeutralListener neutralButtonListener) {
        this.neutralButtonName = neutralButtonName;
        this.neutralButtonListener = neutralButtonListener;
    }


    public void setDialogTitle(String title) {
        this.title = title;
    }

    public void show() {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setView(generatorView());
        if (title != null) {
            adb.setTitle(title);
        }
        adb.setView(generatorView());
        if (generateListener != null) {
            adb.setPositiveButton(generateButtonName, generate);
        }
        if (cancelListener != null) {
            adb.setNegativeButton(cancelButtonName, cancel);
        }
        if (neutralButtonListener != null) {
            adb.setNeutralButton(neutralButtonName, neutral);
        }
        adb.create().show();
    }


    private DialogInterface.OnClickListener generate = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int in) {
            switch (in) {
                case BUTTON_POSITIVE:
                    String sPayload = payload.getText().toString();
                    StringBuilder sb = new StringBuilder();
                    String crlf = "[crlf]";
                    String space = " ";
                    String connect = "CONNECT ";
                    String host = "Host: ";
                    String host_port = "[host_port]";
                    String protocol = " [protocol]";
                    String outro = crlf + crlf;
                    String onePone = "HTTP/1.1";
                    String http = "http://";
                    String raw = "[raw]";
                    int r = requestSpin.getSelectedItemPosition();
                    int i = injectSpin.getSelectedItemPosition();
                    if(!rDirect.isChecked()){
                        switch(i){
                            case 0:
                                sb.append(connect);
                                if(cbFront.isChecked()){
                                    sb.append(sPayload).append("@");
                                    if(r == 0){
                                        sb.append(host_port);
                                        sb.append(protocol).append(crlf);
                                    }else{
                                        sb.append(host_port).append(space);
                                        sb.append(onePone);
                                        sb.append(outro);
                                        sb.append((String) requestSpin.getSelectedItem()).append(space);
                                        sb.append(http.concat(sPayload).concat("/"));
                                        sb.append(protocol).append(crlf);
                                    }
                                }else if(cbBack.isChecked()){
                                    sb.append(host_port).append("@").append(sPayload);
                                    if(r == 0){
                                        sb.append(protocol).append(crlf);
                                    }else{
                                        sb.append(space);
                                        sb.append(onePone);
                                        sb.append(outro);
                                        sb.append((String) requestSpin.getSelectedItem()).append(space);
                                        sb.append(http.concat(sPayload).concat("/"));
                                        sb.append(protocol).append(crlf);
                                    }
                                }else{
                                    if(r == 0){
                                        sb.append(host_port);
                                        sb.append(protocol).append(crlf);
                                        if(rSplit.isChecked()){
                                            int s = splitSpin.getSelectedItemPosition();
                                            switch(s){
                                                case 0:
                                                    sb.append("[split]");
                                                    sb.append(connect).append(http).append(sPayload).append("/").append(space).append(onePone).append(crlf);
                                                    break;
                                                case 1:
                                                    sb.append("[instant_split]");
                                                    sb.append(connect).append(http).append(sPayload).append("/").append(space).append(onePone).append(crlf);
                                                    break;
                                                case 2:
                                                    sb.append("[delay_split]");
                                                    sb.append(connect).append(http).append(sPayload).append("/").append(space).append(onePone).append(crlf);
                                                    break;
                                                default: break;
                                            }
                                        }
                                    }
                                }
                                break;
                            case 1:
                                sb.append((String) requestSpin.getSelectedItem()).append(space);
                                sb.append(http.concat(sPayload).concat("/")).append(space);
                                sb.append(onePone).append(crlf);
                                break;
                            case 2:
                                sb.append(connect);
                                if(cbFront.isChecked()){
                                    sb.append(sPayload).append("@");
                                    sb.append(host_port);
                                }else if(cbBack.isChecked()){
                                    sb.append(host_port);
                                    sb.append("@").append(sPayload);
                                }else{
                                    sb.append(host_port);
                                }
                                if(rSplit.isChecked()){
                                    sb.append(protocol);
                                    sb.append(crlf);
                                }else{
                                    sb.append(space);
                                    sb.append(onePone);
                                    sb.append(crlf);
                                }
                                if(rSplit.isChecked()){
                                    int s = splitSpin.getSelectedItemPosition();
                                    switch(s){
                                        case 0:
                                            sb.append("[split]");
                                            break;
                                        case 1:
                                            sb.append("[instant_split]");
                                            break;
                                        case 2:
                                            sb.append("[delay_split]");
                                            break;
                                        default: break;
                                    }
                                }else{
                                    sb.append(crlf);
                                }
                                sb.append((String) requestSpin.getSelectedItem()).append(space);
                                sb.append(http.concat(sPayload).concat("/"));
                                if(rSplit.isChecked()){
                                    sb.append(space).append(onePone).append(crlf);
                                }else{
                                    sb.append(protocol).append(crlf);
                                }
                                break;
                        }
                    }else{
                        String d = (String) requestSpin.getSelectedItem();
                        sb.append(d);
                        if(cbFront.isChecked())
                            sb.append(space).append(sPayload).append("@").append(host_port).append(protocol).append(crlf);
                        else if(cbBack.isChecked())
                            sb.append(" [host_port]").append("@").append(sPayload).append(protocol).append(crlf);
                        else
                            sb.append(space).append(host_port).append(protocol).append(crlf);
                    }
                    if(sPayload.isEmpty() || sPayload.equals("")){

                    }else{
                        sb.append(host).append(sPayload);

                        if(cbOnline.isChecked()){
                            sb.append(crlf).append("X-Online-Host: ").append(sPayload);
                        }
                        if(cbForward.isChecked()){
                            sb.append(crlf).append("X-Forward-Host: ").append(sPayload);
                        }
                        if(cbReverse.isChecked()){
                            sb.append(crlf).append("X-Forwarded-For: ").append(sPayload);
                        }
                    }
                    if(cbKeep.isChecked()){
                        sb.append(crlf).append("Connection: Keep-Alive");
                    }
                    if(cbUser.isChecked()){
                        sb.append(crlf).append("User-Agent: [ua]");
                    }
                    if(cbReferer.isChecked()){
                        sb.append(crlf).append("Referer: ").append(sPayload);
                    }
                    if(cbDual.isChecked()){
                        sb.append(crlf).append(connect).append(host_port).append(protocol);
                    }
                    if(i ==1){
                        sb.append(outro);
                        if(rSplit.isChecked()){
                            int s = splitSpin.getSelectedItemPosition();
                            switch(s){
                                case 0:
                                    sb.append("[split]");
                                    break;
                                case 1:
                                    sb.append("[instant_split]");
                                    break;
                                case 2:
                                    sb.append("[delay_split]");
                                    break;
                                default: break;
                            }
                        }
                        sb.append(connect);
                        if(cbFront.isChecked()){
                            sb.append(sPayload).append("@").append(host_port).append(protocol).append(outro);
                        }else if(cbBack.isChecked()){
                            sb.append(host_port).append("@").append(sPayload).append(protocol).append(outro);
                        }else{
                            sb.append(host_port).append(protocol).append(outro);
                        }
                    }else{
                        sb.append(outro);
                    }
                    String f = sb.toString();
                    if(cbRaw.isChecked()){
                        if(f.contains("CONNECT [host_port] [protocol]")){
                            String rw = f.replace("CONNECT [host_port] [protocol]",raw);
                            if(cbRotate.isChecked()){
                                if(!rw.contains(";")){
                                    Toast.makeText(context, "Invalid URL/Host",Toast.LENGTH_SHORT).show();
                                }else{
                                    generateListener.onGenerate(rw);
                                }
                            }else{
                                generateListener.onGenerate(rw);
                            }
                        }
                    }else{
                        if(cbRotate.isChecked()){
                            if(!sb.toString().contains(";")){
                                Toast.makeText(context, "Invalid URL/Host", Toast.LENGTH_SHORT).show();
                            }else{
                                generateListener.onGenerate(sb.toString());
                            }
                        }else{
                            generateListener.onGenerate(sb.toString());
                        }

                    }
                    save();
            }
        }
    };

    private DialogInterface.OnClickListener cancel = new DialogInterface.OnClickListener(){

        @Override
        public void onClick(DialogInterface p1, int p2)
        {
            // TODO: Implement this method
            if(cancelListener != null){
                cancelListener.onCancelListener();
            }

        }
    };

    private DialogInterface.OnClickListener neutral = new DialogInterface.OnClickListener(){

        @Override
        public void onClick(DialogInterface p1, int p2)
        {
            // TODO: Implement this method
            if(neutralButtonListener != null){
                neutralButtonListener.onNeutralListener();
            }

        }
    };

    private static View generatorView(){
        inflater = LayoutInflater.from(context);
        v = inflater.inflate(R.layout.payload_generator_dialog, null);
        payload = v.findViewById(R.id.host);
        editTextHint = v.findViewById(R.id.editTextHint);
        rNomal = v.findViewById(R.id.rNormal);
        rSplit = v.findViewById(R.id.rSplit);
        rDirect = v.findViewById(R.id.rDirect);
        cbRotate = v.findViewById(R.id.cbRotate);
        cbOnline = v.findViewById(R.id.cbOnline);
        cbForward =  v.findViewById(R.id.cbForward);
        cbReverse =  v.findViewById(R.id.cbReverse);
        cbKeep =  v.findViewById(R.id.cbKeep);
        cbUser =  v.findViewById(R.id.cbUser);
        cbReferer =  v.findViewById(R.id.cbReferer);
        cbFront =  v.findViewById(R.id.cbFront);
        cbBack =  v.findViewById(R.id.cbBack);
        cbRaw =  v.findViewById(R.id.cbRaw);
        cbDual =  v.findViewById(R.id.cbDual);
        requestSpin = v.findViewById(R.id.request_spin);
        injectSpin = v.findViewById(R.id.inject_spin);
        splitSpin = v.findViewById(R.id.split_spin);
        rNomal.setOnCheckedChangeListener(cb);
        rSplit.setOnCheckedChangeListener(cb);
        rDirect.setOnCheckedChangeListener(cb);
        cbFront.setOnCheckedChangeListener(cb);
        cbBack.setOnCheckedChangeListener(cb);
        cbRaw.setOnCheckedChangeListener(cb);
        cbDual.setOnCheckedChangeListener(cb);
        cbRotate.setOnCheckedChangeListener(cb);
        rAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, request_items);
        iAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, inject_items);
        sAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, split_items);
        requestSpin.setAdapter(rAdapter);
        injectSpin.setAdapter(iAdapter);
        splitSpin.setAdapter(sAdapter);
        rAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        iAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        requestSpin.setPrompt("Request Method");
        injectSpin.setPrompt("Injection Method");
        splitSpin.setPrompt("Split Method");
        requestSpin.setOnItemSelectedListener(isl);
        injectSpin.setOnItemSelectedListener(isl);
        load();
        splitSpin.setSelection(0);
        requestSpin.setSelection(0);
        injectSpin.setSelection(0);
        rNomal.setChecked(true);
        v.findViewById(R.id.payload_generator_dialogLinearLayout).requestFocus();
        return v;
    }



    private static CompoundButton.OnCheckedChangeListener cb = new CompoundButton.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton p1, boolean p2)
        {
			switch(p1.getId()){                 
				case R.id.rNormal:           
					if(p2){
						splitSpin.setEnabled(false);
						injectSpin.setEnabled(true);
						requestSpin.setSelection(0);
						injectSpin.setSelection(0);
					}
					break;
				case R.id.rSplit:
					if(p2){
						splitSpin.setEnabled(true);
						requestSpin.setSelection(1);
						injectSpin.setSelection(2);
						splitSpin.setSelection(2);
					}
					break;   
				case R.id.rDirect:
					if(p2){
						splitSpin.setEnabled(false);
						injectSpin.setEnabled(false);
					}
					break;

				case R.id.cbRaw:
					if(p2){
						if(cbDual.isChecked()){
							cbDual.setChecked(false);
						}
					}
					break;
				case R.id.cbDual:
					if(p2){
						if(cbRaw.isChecked()){
							cbRaw.setChecked(false);
						}
					}
					break;

                case R.id.cbFront:
					if(p2){
						if(cbBack.isChecked()){
							cbBack.setChecked(false);
						}
					}
					break;
				case R.id.cbBack:
                    if(p2){
						if(cbFront.isChecked()){
							cbFront.setChecked(false);
						}
					}
					break;
				case R.id.cbRotate:
					if(p2){
						editTextHint.setHint("URL/Host (eg: 1.com;2.com;3.com)");                  
					}else{
						editTextHint.setHint("URL/Host");
					}
					break;
			}  
        }

    };

    private static AdapterView.OnItemSelectedListener isl = new AdapterView.OnItemSelectedListener(){
        @Override
        public void onItemSelected(AdapterView<?> p1, View p2, int p3, long p4)
        {
            int id = p1.getId();
            if(id == R.id.request_spin){
                if(p3!=0){
                    if(injectSpin.getSelectedItemPosition() == 1){
                        return;
                    }else{
                        injectSpin.setSelection(2);
                    }
                }
                editor.putInt("reqSpin", p3).commit();
            }else if(id == R.id.inject_spin){
                if(p3 !=0 ){
                    requestSpin.setSelection(1);
                }else if(p3 == 0){
                    if(rSplit.isChecked()){
                        rNomal.setChecked(true);
                    }
                    requestSpin.setSelection(0);
                }
                editor.putInt("injSpin", p3).commit();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> p1)
        {
        }
    };

    private static void load(){
        if (prefs == null && editor == null) {
            prefs = context.getSharedPreferences("status404error_prefs", context.MODE_PRIVATE);
            editor = prefs.edit();
        }
        payload.setText(prefs.getString("my_inputted_payload",""));
        rNomal.setChecked(prefs.getBoolean("rNormal", false));
        rSplit.setChecked(prefs.getBoolean("rSplit", false));
        cbRotate.setChecked(prefs.getBoolean("cbRotate", false));     
        rDirect.setChecked(prefs.getBoolean("rDirect", false));
        cbBack.setChecked(prefs.getBoolean("cbBack", false));
        cbFront.setChecked(prefs.getBoolean("cbFront",false));
        cbOnline.setChecked(prefs.getBoolean("cbOnline", false));
        cbForward.setChecked(prefs.getBoolean("cbForward", false));
        cbReverse.setChecked(prefs.getBoolean("cbReverse", false));
        cbKeep.setChecked(prefs.getBoolean("cbKeep", false));
        cbUser.setChecked(prefs.getBoolean("cbUser", false));
        cbReferer.setChecked(prefs.getBoolean("cbReferer", false));
        cbRaw.setChecked(prefs.getBoolean("cbRaw", false));
        cbDual.setChecked(prefs.getBoolean("cbDual", false));
        requestSpin.setSelection(prefs.getInt("reqSpin",0));
        injectSpin.setSelection(prefs.getInt("injSpin",0));
    }
    private static void save(){
        if (prefs == null && editor == null) {
            prefs = context.getSharedPreferences("status404error_prefs", context.MODE_PRIVATE);
            editor = prefs.edit();
        }
        editor.putString("my_inputted_payload",payload.getText().toString()).commit();
        editor.putBoolean("rNormal", rNomal.isChecked()).commit();
        editor.putBoolean("rSplit", rSplit.isChecked()).commit();
        editor.putBoolean("rDirect", rDirect.isChecked()).commit();
        editor.putBoolean("cbFront", cbFront.isChecked()).commit();
        editor.putBoolean("cbBack", cbBack.isChecked()).commit();
        editor.putBoolean("cbOnline", cbOnline.isChecked()).commit();
        editor.putBoolean("cbForward", cbForward.isChecked()).commit();
        editor.putBoolean("cbReverse", cbReverse.isChecked()).commit();
        editor.putBoolean("cbKeep", cbKeep.isChecked()).commit();
        editor.putBoolean("cbUser", cbUser.isChecked()).commit();
        editor.putBoolean("cbReferer", cbReferer.isChecked()).commit();
        editor.putBoolean("cbRaw", cbRaw.isChecked()).commit();
        editor.putBoolean("cbDual", cbDual.isChecked()).commit();
        editor.putBoolean("cbRotate", cbRotate.isChecked()).commit();
    }

}
