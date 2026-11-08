package com.ace.injector.preference;

import android.support.v7.preference.PreferenceFragmentCompat;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.support.v7.preference.Preference;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.EditTextPreference;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.CheckBoxPreference;
import android.content.Intent;
import com.ace.injector.SocksHttpApp;
import com.ace.injectorpro.R;
import android.support.v7.preference.ListPreference;
import android.content.Context;
import android.support.v7.app.AppCompatDelegate;
import com.ultrasshservice.logger.SkStatus;
import com.ultrasshservice.config.SettingsConstants;
import com.ultrasshservice.config.Settings;
import android.support.v7.preference.PreferenceScreen;
import com.ultrasshservice.logger.ConnectionStatus;
import android.os.Handler;
import android.app.Activity;
import com.ace.injector.LauncherActivity;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import com.ace.injector.SocksHttpMainActivity;
import android.support.v4.content.LocalBroadcastManager;
import com.ace.injector.CustomDNS;
import com.ace.injector.util.BufferSizeDialog;

public class SettingsPreference extends PreferenceFragmentCompat
	implements Preference.OnPreferenceChangeListener, SettingsConstants,
		SkStatus.StateListener
{
	private Handler mHandler;
	private SharedPreferences mPref;
	
	public static final String
		SSHSERVER_PREFERENCE_KEY = "screenSSHSettings",
	SLOWDNS_PREFERENCE_KEY = "screenSlowSettings";
		
	private String[] settings_disabled_keys = {
		AUTO_PINGER,
		SSH_COMPRESSION,
		DNSFORWARD_KEY,
		UDPFORWARD_KEY,
		UDPRESOLVER_KEY,
		PINGER_KEY,
		AUTO_CLEAR_LOGS_KEY,
		HIDE_LOG_KEY,
		MODO_NOTURNO_KEY
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mHandler = new Handler();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		SkStatus.addStateListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		
		SkStatus.removeStateListener(this);
	}
	
	
	@Override
    public void onCreatePreferences(Bundle bundle, String root_key)
	{
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.app_preferences, root_key);
		
		mPref = getPreferenceManager().getDefaultSharedPreferences(getContext());
		
		final Preference buffer = findPreference("buffer_size");
		buffer.setSummary(new StringBuffer().append("Send: ").append(mPref.getString("buffer_send", "16384")).append(" | Receive: ").append(mPref.getString("buffer_receive", "32768")));
		buffer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					new BufferSizeDialog(getActivity(), buffer).show();
					return true;
				}
			});
		
		Preference udpForwardPreference = (CheckBoxPreference)
			findPreference(UDPFORWARD_KEY);
		udpForwardPreference.setOnPreferenceChangeListener(this);
		
		Preference dnsForwardPreference = (CheckBoxPreference)
			findPreference(DNSFORWARD_KEY);
		dnsForwardPreference.setOnPreferenceChangeListener(this);
		
		ListPreference modoNoturno = (ListPreference)
			findPreference(MODO_NOTURNO_KEY);
		modoNoturno.setOnPreferenceChangeListener(this);
		SettingsAdvancedPreference.setListPreferenceSummary(modoNoturno, modoNoturno.getValue());
		
		
		setRunningTunnel(SkStatus.isTunnelActive());
	}
	
	private void onChangeUseVpn(boolean use_vpn){
		Preference udpResolverPreference = (EditTextPreference)
			findPreference(UDPRESOLVER_KEY);
		
		
		for (String key : settings_disabled_keys){
			getPreferenceManager().findPreference(key)
				.setEnabled(use_vpn);
		}

		use_vpn = true;
		if (use_vpn) {
			boolean isUdpForward = mPref.getBoolean(UDPFORWARD_KEY, false);
			boolean isDnsForward = mPref.getBoolean(DNSFORWARD_KEY, false);
			
			udpResolverPreference.setEnabled(isUdpForward);
			
		}
		else {
			String[] list = {
				UDPFORWARD_KEY,
				UDPRESOLVER_KEY,
				DNSFORWARD_KEY
			};
			for (String key : list) {
				getPreferenceManager().findPreference(key)
					.setEnabled(false);
			}
		}
	}
	
	private void setRunningTunnel(boolean isRunning) {
		if (isRunning) {
			for (String key : settings_disabled_keys){
				getPreferenceManager().findPreference(key)
					.setEnabled(false);
			}
		}
		else {
			onChangeUseVpn(true);
		}
	}

	
	/**
	* Preference.OnPreferenceChangeListener
	* Implementação
	*/
	
	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue)
	{
		switch (pref.getKey()) {
			case UDPFORWARD_KEY:
				boolean isUdpForward = (boolean) newValue;

				Preference udpResolverPreference = (EditTextPreference)
					findPreference(UDPRESOLVER_KEY);
				udpResolverPreference.setEnabled(isUdpForward);
			break;
			
			case DNSFORWARD_KEY:
				boolean isDnsForward = (boolean) newValue;

				
				if(isDnsForward==true){
					mPref.edit().putBoolean("Google_dns",true).apply();
					Intent TunnDNS = new Intent(getActivity(), CustomDNS.class);
					getActivity().startActivity(TunnDNS);
				}
			break;
			
			case MODO_NOTURNO_KEY:
				final String enableModoNoturno = (String)newValue;
				
				if (enableModoNoturno.equals(mPref.getString(MODO_NOTURNO_KEY, "off"))) {
					return false;
				}

				/*SettingsAdvancedPreference.setListPreferenceSummary(pref_list, (String) newValue);
				
				new AlertDialog.Builder(getContext())
					. setTitle(R.string.attention)
					. setMessage(R.string.restarting_app_theme)
					. setCancelable(false)
					. show();*/
				
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						Context context = SocksHttpApp.getApp();
						
						new Settings(context)
							.setModoNoturno(enableModoNoturno);
						
						// reinicia app
						Intent startActivity = new Intent(context, LauncherActivity.class);
						int pendingIntentId = 123456;
						PendingIntent pendingIntent = PendingIntent.getActivity(context, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT);

						AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
						mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);

						// encerra tudo
						/*if (Build.VERSION.SDK_INT >= 16) {
							Activity act = getActivity();
							if (act != null)
								act.finishAffinity();
						}*/
						
						System.exit(0);
					}
				}, 300);
				
				getActivity().finish();
			return false;
		
		}
		return true;
	}

	@Override
	public void updateState(String state, String logMessage, int localizedResId, ConnectionStatus level, Intent intent)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				setRunningTunnel(SkStatus.isTunnelActive());
			}
		});
	}

	
}
