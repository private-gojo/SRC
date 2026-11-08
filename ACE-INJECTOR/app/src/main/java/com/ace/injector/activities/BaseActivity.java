package com.ace.injector.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.ace.injector.preference.SettingsPreference;
import com.ultrasshservice.config.Settings;
import android.support.v7.app.AppCompatDelegate;
import android.content.Context;
import com.ace.injector.preference.LocaleHelper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import static android.content.pm.PackageManager.GET_META_DATA;
import android.support.v7.app.NotificationCompat;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import org.json.JSONObject;
import android.content.pm.PackageInfo;
import com.ace.injector.util.Utils;
import com.ace.injector.util.AppUpdater;
import org.json.JSONException;
import com.ace.injectorpro.R;
import android.app.Notification;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.app.NotificationChannel;
import android.net.ConnectivityManager;

/**
 * Created by Pankaj on 03-11-2017.
 */
public abstract class BaseActivity extends AppCompatActivity
{
	public static int mTheme = 0;

	private ConnectivityManager connMgr;

	private NotificationManager mNotificationManager;
    
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//setTheme(ThemeUtil.getThemeId(mTheme));
		
		setModoNoturnoLocal();
		//Sulod sa onCreate MainBase.

			connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		resetTitles();
	}
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.setLocale(base));
	}
	
	public void setModoNoturnoLocal() {
		boolean is = new Settings(this)
			.getModoNoturno().equals("on");

		int night_mode = is ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
		//AppCompatDelegate.setDefaultNightMode(night_mode);
		getDelegate().setLocalNightMode(night_mode);
	}
	
	protected void resetTitles() {
		try {
			ActivityInfo info = getPackageManager().getActivityInfo(getComponentName(), GET_META_DATA);
			if (info.labelRes != 0) {
				setTitle(info.labelRes);
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void updateApp(final boolean showDialog) {
		new AppUpdater(this, new AppUpdater.OnUpdateListener() {
				@Override
				public void onUpdateListener(String result) {
					try {
						if (!result.contains("Error on getting data")) {			
							final JSONObject obj = new JSONObject(result);
							PackageInfo pinfo = Utils.getAppInfo(getApplicationContext());
							String versionName = pinfo.versionName;
							if (versionName.equals(obj.getString("versionCode"))) {
								if (showDialog){
									AlertDialog.Builder alert = new AlertDialog.Builder(BaseActivity.this);
									alert.setTitle(getString(R.string.app_name))
										.setMessage(getString(R.string.alert_app_on_latest))
										.setPositiveButton("OK", null)
										.show();
								}
							} else {
								updateNotif();
								notif1();
								notif2();
								AlertDialog.Builder alert = new AlertDialog.Builder(BaseActivity.this);
								alert.setTitle(getString(R.string.alert_update_app))
									.setMessage(obj.getString("Message"))
									.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {  
										public void onClick(DialogInterface dialog, int id) {  
											try  {
												startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(obj.getString("url"))));
											} catch (JSONException e) {

											}
										}  
									}).show();
							}
						} else if(result.contains("Error on getting data") && showDialog){
							errorUpdateDialog(result);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start(showDialog);	
    }

	private void errorUpdateDialog(String error) {
		new AlertDialog.Builder(this)
			.setTitle("Update error!!!")
			.setMessage(getString(R.string.alert_update_error) +
						"Error:" + error)
			.setPositiveButton("Ok", null)
			.create().show();
	}

	public void notif1() {
		NotificationCompat.Builder b = new NotificationCompat.Builder(this);
		b.setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
			.setSmallIcon(R.drawable.ic_launcher)
            .setTicker(getString(R.string.bugs_fixed))
            .setContentTitle(getString(R.string.alert_update_app))
            .setContentText(getString(R.string.alert_update_app_now))
            .setContentInfo(getString(R.string.app_name));


		NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(1, b.build());
	}

	private void notif2(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		r.play();
	}
	private void updateNotif(){

		NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE); 
		Notification.Builder notification = new Notification.Builder(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			notification.setChannelId(this.getPackageName() + ".icodetunnel");
			createNotification(notificationManager, this.getPackageName() + ".icodetunnel");
		}

		notification.setContentTitle("New Application Update")
			.setContentText("A new application version is detected.")
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ace_launchers))
			.setDefaults(Notification.DEFAULT_ALL)
			.setPriority(Notification.PRIORITY_HIGH)
			.setShowWhen(true)
			.setSmallIcon(R.drawable.ace_launchers);
		notificationManager.notify(4130,notification.getNotification());

	}

	private void createNotification(NotificationManager notificationManager, String id)
	{
		NotificationChannel mNotif = new NotificationChannel(id, "Developer", NotificationManager.IMPORTANCE_HIGH);
		mNotif.setShowBadge(true);
		notificationManager.createNotificationChannel(mNotif);
		// TODO: Implement this method
	}
	
}
