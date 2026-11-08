package com.ace.injector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.ace.injectorpro.R;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.DialogFragment;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;
import android.view.View;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;
import com.ace.injector.util.Utils;
import android.util.Log;
import android.widget.TextView;
import android.support.v4.view.GravityCompat;
import android.widget.EditText;
import android.support.design.widget.TextInputEditText;
import com.ace.injector.DrawerLog;
import android.support.v4.widget.DrawerLayout;
import android.net.Uri;
import android.widget.Button;
import com.ace.injector.SocksHttpApp;
import android.widget.CheckBox;
import android.support.v4.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import com.ace.injector.activities.ConfigGeralActivity;
import android.view.LayoutInflater;
import android.content.pm.PackageManager;
import android.text.Html;
import android.support.v7.app.AlertDialog;
import android.content.pm.PackageInfo;
import com.ultrasshservice.logger.SkStatus;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.widget.LinearLayout;
import com.ace.injector.fragments.ProxyRemoteDialogFragment;
import android.annotation.TargetApi;
import android.os.Build;
import android.net.VpnService;
import android.content.ActivityNotFoundException;
import android.app.Activity;
import com.ultrasshservice.logger.ConnectionStatus;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import com.ace.injector.fragments.ClearConfigDialogFragment;
import com.ace.injector.activities.ConfigExportFileActivity;
import com.ace.injector.activities.ConfigImportFileActivity;
import com.ultrasshservice.config.Settings;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.PersistableBundle;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatRadioButton;
import android.widget.RadioGroup;
import com.ultrasshservice.config.ConfigParser;
import android.support.v4.app.ActivityCompat;
import android.content.DialogInterface;
import com.ultrasshservice.tunnel.TunnelManagerHelper;
import com.ultrasshservice.LaunchVpn;
import com.ace.injector.activities.AboutActivity;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import com.ace.injector.model.ViewFragment;
import android.text.InputType;
import android.widget.ImageButton;
import java.io.IOException;
import android.support.design.widget.NavigationView;
import android.util.AttributeSet;
import com.ace.injector.util.GoogleFeedbackUtils;
import com.ace.injector.activities.BaseActivity;
import com.ultrasshservice.tunnel.TunnelUtils;
import android.text.TextUtils;
import com.ace.injector.preference.LocaleHelper;
import android.support.annotation.Nullable;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.view.View.OnClickListener;
import com.ace.injector.fragments.CustomSniFragments;
import android.widget.RadioButton;
import android.text.method.LinkMovementMethod;
import com.ace.injector.adapter.LogsAdapter;
import android.support.v7.widget.LinearLayoutManager;
import com.ace.injector.view.PayloadGenerator;
import android.Manifest;
import java.io.File;
import com.ultrasshservice.util.CustomNativeLoader;
import com.ace.injector.util.KillThis;
import com.ultrasshservice.util.StreamGobbler;
import android.support.v7.app.NotificationCompat;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.app.Notification;
import android.graphics.BitmapFactory;
import android.app.NotificationChannel;
import com.ace.injector.util.AppUpdater;
import org.json.JSONObject;
import org.json.JSONException;
import android.view.MenuItem;
import android.view.Menu;
import android.util.TypedValue;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import com.ace.injector.util.MyIP_Address;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.ultrasshservice.tunnel.TunnelUtils;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.ads.reward.RewardItem;


public class SocksHttpMainActivity extends BaseActivity
	implements DrawerLayout.DrawerListener,
			View.OnClickListener, RadioGroup.OnCheckedChangeListener,
CompoundButton.OnCheckedChangeListener, SkStatus.StateListener, RewardedVideoAdListener
{

	@Override
	public void onRewardedVideoAdLoaded() {
		if (rewardedAd.isLoaded()){
			rewardedAd.show();
		}
	}

	@Override
	public void onRewardedVideoAdOpened() {
	}

	@Override
	public void onRewardedVideoStarted() {
	}

	@Override
	public void onRewardedVideoAdClosed() {
		Toast.makeText(SocksHttpMainActivity.this, "Thank you for supporting!! 💙", Toast.LENGTH_SHORT)
			.show();
	}

	@Override
	public void onRewarded(RewardItem p1) {
	}

	@Override
	public void onRewardedVideoAdLeftApplication() {
	}

	@Override
	public void onRewardedVideoAdFailedToLoad(int p1) {
	}
	
	private InterstitialAd success;
	private static final String TAG = SocksHttpMainActivity.class.getSimpleName();
	private static final String UPDATE_VIEWS = "MainUpdate";
	public static final String OPEN_LOGS = "com.slipkprojects.sockshttp:openLogs";
	private DrawerLog mDrawer;
	private LinearLayout view_all_layout;
	private PayloadGenerator paygen;
	private Settings mConfig;
	private Toolbar toolbar_main;
	private Handler mHandler;
	private LinearLayout cmodsniLayout;
	private LinearLayout ViewGone;
	private TextView cmodsniText;
	private LinearLayout mainLayout;
	private LinearLayout proxyInputLayout;
	private TextView proxyText;
	private RadioGroup metodoConexaoRadio;
	private android.support.v7.widget.CardView payloadLayout;
	private TextInputEditText payloadEdit;
	private CheckBox customPayloadSwitch;
	private Button starterButton;
	private android.support.v7.widget.CardView configMsgLayout;
	private TextView configMsgText;
	private LinearLayout payload_menu;
	private TextView activitymainTextView1;
	private LogsAdapter mLogAdapter;
	

		
	private CheckBox startSSHCheckbox;
	private CheckBox dnsCheckBox;
	private AdView adsBannerView;
	private RewardedVideoAd rewardedAd;
	private InterstitialAd mInterstitialAd;
	
	
	@Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

		mHandler = new Handler();
		mConfig = new Settings(this);
		mDrawer = new DrawerLog(this);
		paygen = new PayloadGenerator(this);
		SharedPreferences prefs = getSharedPreferences(SocksHttpApp.PREFS_GERAL, Context.MODE_PRIVATE);
		Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
		boolean showFirstTime = prefs.getBoolean("connect_first_time", true);
		int lastVersion = prefs.getInt("last_version", 0);
		// se primeira vez
		if (showFirstTime)
        {
            SharedPreferences.Editor pEdit = prefs.edit();
            pEdit.putBoolean("connect_first_time", false);
            pEdit.apply();

			Settings.setDefaultConfig(this);

			showBoasVindas();
        }
		setupInterstitial();
		loadInterstitialAds();
		try {
			int idAtual = ConfigParser.getBuildId(this);

			if (lastVersion < idAtual) {
				SharedPreferences.Editor pEdit = prefs.edit();
				pEdit.putInt("last_version", idAtual);
				pEdit.apply();

				// se estiver atualizando
				if (!showFirstTime) {
					if (lastVersion <= 12) {
						Settings.setDefaultConfig(this);
						Settings.clearSettings(this);

						Toast.makeText(this, "Settings have been cleaned to avoid bugs",
							Toast.LENGTH_LONG).show();
					}
				}

			}
		} catch(IOException e) {}
		
		
		// set layout
		doLayout();

		// verifica se existe algum problema


		// recebe local dados
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_VIEWS);
		filter.addAction(OPEN_LOGS);
		updatez(false);
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(mActivityReceiver, filter);
			
		doUpdateLayout();
		MobileAds.initialize(this, "ca-app-pub-4327217352829821~9017714353");
	}

	public void tunnelz(View v)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		boolean isRunning = SkStatus.isTunnelActive();
		if (!isRunning) {
			if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
				startActivity(new Intent(this, TunnelActivity.class));
			}
		}
	}
	
	public void hype(View v)
	{
		AlertDialog.Builder builder1 = new AlertDialog.Builder(SocksHttpMainActivity.this);

		builder1.setTitle(Html.fromHtml("<b>Join Facebook Group<b>"));
		builder1.setMessage(Html.fromHtml("Facebook Group: <br><a href=\"https://www.facebook.com/groups/125851496114300/?ref=share\">https://www.facebook.com/groups/125851496114300/?ref=share</a><br><br><br>Facebook Group 2: <br><a href=\"https://www.facebook.com/groups/234423324271554/?ref=share\">https://www.facebook.com/groups/234423324271554/?ref=share</a>"));

		builder1.setCancelable(false);
		builder1.setPositiveButton("ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
            });
		AlertDialog Alert1 = builder1.create();
		Alert1 .show();
		((TextView)Alert1.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

	}
	
	
	
	public void updatez(final boolean showDialog) {
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
									AlertDialog.Builder alert = new AlertDialog.Builder(SocksHttpMainActivity.this);
									alert.setTitle(getString(R.string.app_name))
										.setMessage(getString(R.string.alert_app_on_latest))
										.setPositiveButton("OK", null)
										.show();
								}
							} else {
								updateNotif();
								notif1();
								notif2();
								AlertDialog.Builder alert = new AlertDialog.Builder(SocksHttpMainActivity.this);
								alert.setTitle(getString(R.string.alert_update_app))
									.setMessage(obj.getString("Message"))
									.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {  
										public void onClick(DialogInterface dialog, int id) {  
											dialog.dismiss();
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
	
	/**
	 * Layout
	 */
	 
	private void doLayout() {
		setContentView(R.layout.activity_main_drawer);
		final SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(this);
		toolbar_main = (Toolbar) findViewById(R.id.toolbar_main);
		setSupportActionBar(toolbar_main);

		mDrawer.setDrawer(this);
		// set ADS
		adsBannerView = (AdView) findViewById(R.id.adBannerMainView);
		rewardedAd = MobileAds.getRewardedVideoAdInstance(this);
		rewardedAd.setRewardedVideoAdListener(this);
		if (TunnelUtils.isNetworkOnline(this)) {

			adsBannerView.setAdListener(new AdListener() {
					@Override
					public void onAdLoaded() {
						if (adsBannerView != null) {
							adsBannerView.setVisibility(View.VISIBLE);
						}
					}
				});

			adsBannerView.loadAd(new AdRequest.Builder()
								 .build());
		}
		
		int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (permissionCheck != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
		}
		startSSHCheckbox = (CheckBox) findViewById(R.id.startSSHCheckbox);
		startSSHCheckbox.setText("Start SSH");
		startSSHCheckbox.setTextSize(TypedValue.COMPLEX_UNIT_DIP,12);
		startSSHCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mPref.edit().putBoolean("startSSHCheck",isChecked).apply();
				}
			});
		dnsCheckBox=(CheckBox) findViewById(R.id.useDns);
		dnsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mPref.edit().putBoolean("startDns",isChecked).apply();
				}
			});
		dnsCheckBox.setTextSize(TypedValue.COMPLEX_UNIT_DIP,12);
		
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		mLogAdapter = new LogsAdapter(layoutManager,this);
		mLogAdapter.scrollToLastPosition();
		
		mainLayout = (LinearLayout) findViewById(R.id.activity_mainLinearLayout);
		
		starterButton = (Button) findViewById(R.id.activity_starterButtonMain);
		view_all_layout = (LinearLayout)findViewById(R.id.view_all_layout);
		payload_menu = (LinearLayout)findViewById(R.id.payload_menu);
		proxyInputLayout = (LinearLayout) findViewById(R.id.activity_mainInputProxyLayout);
		proxyText = (TextView) findViewById(R.id.activity_mainProxyText);
		activitymainTextView1 = (TextView)findViewById(R.id.activitymainTextView1);
		ViewGone = (LinearLayout) findViewById(R.id.ViewGone);
		cmodsniLayout = (LinearLayout) findViewById(R.id.cmods_snilayout);
		cmodsniText = (TextView) findViewById(R.id.cmods_snitext);
		cmodsniText.setText(mConfig.getPrivString(Settings.CUSTOM_SNI));
		cmodsniLayout.setOnClickListener(new OnClickListener(){
				SharedPreferences prefs = mConfig.getPrefsPrivate();
				@Override
				public void onClick(View p1) {
					if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
						doSaveData();

						DialogFragment fragSni = new CustomSniFragments();
						fragSni.show(getSupportFragmentManager(), "customSni");
					}
				}
			});
		
		metodoConexaoRadio = (RadioGroup) findViewById(R.id.activity_mainMetodoConexaoRadio);
		customPayloadSwitch = (CheckBox) findViewById(R.id.activity_mainCustomPayloadSwitch);

		starterButton.setOnClickListener(this);
		proxyInputLayout.setOnClickListener(this);
		dnsCheckBox.setOnClickListener(this);
		
		payloadLayout = (android.support.v7.widget.CardView) findViewById(R.id.activity_mainInputPayloadLinearLayout);
		payloadEdit = (TextInputEditText) findViewById(R.id.activity_mainInputPayloadEditText);

		configMsgLayout = (android.support.v7.widget.CardView) findViewById(R.id.activity_mainMensagemConfigLinearLayout);
		configMsgText = (TextView) findViewById(R.id.activity_mainMensagemConfigTextView);

		// fix bugs
		if (mConfig.getPrefsPrivate().getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			
		}
		else {
			payloadEdit.setText(mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY));
		}

		metodoConexaoRadio.setOnCheckedChangeListener(this);
		customPayloadSwitch.setOnCheckedChangeListener(this);
		
	}
	private void generator(){

		paygen.setDialogTitle(getString(R.string.pay_gen));
		paygen.setCancelListener(getString(R.string.cancel), new PayloadGenerator.OnCancelListener(){

                @Override
                public void onCancelListener() {
                }
            });
        paygen.setGenerateListener(getString(R.string.gen), new PayloadGenerator.OnGenerateListener(){

                @Override
                public void onGenerate(String payloadGenerated) {              
					SharedPreferences prefs = mConfig.getPrefsPrivate();
					if (!prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)){
						payloadEdit.setText(payloadGenerated);     
					} else {
						Toast.makeText(SocksHttpMainActivity.this, R.string.custom_payload_msg, Toast.LENGTH_SHORT).show();
					}                                              
				}
            });
        paygen.show();
	}
	private void adsPopUp() {
		if (this.success.isLoaded()) {
            success.show();
		} else {
			// Log.d("TAG", "The interstitial wasn't loaded yet.");
        }
	}
	
	private void setupInterstitial(){

        mInterstitialAd = new InterstitialAd(this);

        mInterstitialAd.setAdUnitId("ca-app-pub-4327217352829821/8634570970"); // inter ads

		mInterstitialAd.setAdListener(new AdListener() {

				@Override
				public void onAdClosed() {
					// Code to be executed when the interstitial ad is closed.
					Toast.makeText(SocksHttpMainActivity.this, "Thank you for supporting!! 💙", Toast.LENGTH_SHORT)
						.show();
					adsPopUp();
					loadInterstitial();

				}
			});

        loadInterstitial();
	}

    private void loadInterstitial(){

        mInterstitialAd.loadAd(new AdRequest.Builder().build());

    }

    private void showInterstitial(){
        if (mInterstitialAd.isLoaded()){
            mInterstitialAd.show(); 
		} else {
            loadInterstitial();    
        }                                                     
    }
	
	private void loadRewardedVideoAd() {
        rewardedAd.loadAd("ca-app-pub-4327217352829821/2371387288",
                          new AdRequest.Builder().build());
    }
	
	
	private void doUpdateLayout() {
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		final SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean isRunning = SkStatus.isTunnelActive();
		int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_SSLTUNNEL);
		
		setStarterButton(starterButton, this);
		setPayloadSwitch(tunnelType, !prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true));
		
		startSSHCheckbox.setEnabled(!isRunning);
		startSSHCheckbox.setChecked(mPref.getBoolean("startSSHCheck",true));
		dnsCheckBox.setChecked(mPref.getBoolean("startDns",true));
		dnsCheckBox.setEnabled(!isRunning);

		String proxyStr = getText(R.string.def_value).toString();

		if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			proxyStr = "*******";
			proxyInputLayout.setEnabled(false);
			customPayloadSwitch.setEnabled(false);
			payloadLayout.setVisibility(View.VISIBLE);
			payloadLayout.setEnabled(false);
			payloadEdit.setText("Locked");
		}
		else {
			String proxy = mConfig.getPrivString(Settings.PROXY_IP_KEY);

			if (proxy != null && !proxy.isEmpty())
				proxyStr = String.format("%s:%s", proxy, mConfig.getPrivString(Settings.PROXY_PORTA_KEY));
			proxyInputLayout.setEnabled(!isRunning);
		} 

		proxyText.setText(proxyStr);
		String sniStr = getText(R.string.ssl_value).toString();


		if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			sniStr = "*******";
			cmodsniLayout.setEnabled(false);
			payloadLayout.setVisibility(View.VISIBLE);
			payloadLayout.setEnabled(false);
			customPayloadSwitch.setEnabled(false);
			payloadEdit.setText("Locked");
		}
		else {
			String sni = mConfig.getPrivString(Settings.CUSTOM_SNI);

			if (sni != null && !sni.isEmpty())
				sniStr = String.format(mConfig.getPrivString(Settings.CUSTOM_SNI));
			cmodsniLayout.setEnabled(!isRunning);
		} 

		cmodsniText.setText(sniStr);
		
		SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();

		switch (tunnelType) {
			case Settings.bTUNNEL_TYPE_SSH_DIRECT:
				((AppCompatRadioButton) findViewById(R.id.activity_mainSSHDirectRadioButton))
					.setChecked(true);
				activitymainTextView1.setText(getString(R.string.direct));
				//edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.GONE);
				customPayloadSwitch.setClickable(true);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				break;

			case Settings.bTUNNEL_TYPE_SSH_PROXY:
				((AppCompatRadioButton) findViewById(R.id.activity_mainSSHProxyRadioButton))
					.setChecked(true);
				activitymainTextView1.setText(getString(R.string.http));
				proxyInputLayout.setVisibility(View.VISIBLE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.GONE);
				customPayloadSwitch.setClickable(true);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				break;
			case Settings.bTUNNEL_TYPE_SSH_SSLTUNNEL:
				((AppCompatRadioButton) findViewById(R.id.activity_mainSSLProxyRadioButton))
					.setChecked(true);
				activitymainTextView1.setText(getString(R.string.ssl));
				customPayloadSwitch.setClickable(false);
				customPayloadSwitch.setChecked(false);
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.VISIBLE);
				payload_menu.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.VISIBLE);
				customPayloadSwitch.setVisibility(View.GONE);
				break;
			case Settings.bTUNNEL_TYPE_PAY_SSL:
				((AppCompatRadioButton) findViewById(R.id.activity_mainSSLPayloadRadioButton))
					.setChecked(true);
				activitymainTextView1.setText(getString(R.string.sslpayload));
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.VISIBLE);
				cmodsniLayout.setVisibility(View.VISIBLE);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setClickable(true);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				break;
			
			case Settings.bTUNNEL_TYPE_SLOWDNS:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SLOWDNS).apply();
				final SharedPreferences slowprefs = mConfig.getPrefsPrivate();
				final String chave = slowprefs.getString(Settings.CHAVE_KEY, "chaveKey");
				final String nameserver = slowprefs.getString(Settings.NAMESERVER_KEY, "serverNameKey");
				final String dns = slowprefs.getString(Settings.DNS_KEY, "dnsKey");
				slowprefs.getString(Settings.CHAVE_KEY, chave);
				slowprefs.getString(Settings.NAMESERVER_KEY, nameserver);
				slowprefs.getString(Settings.DNS_KEY, dns);
				activitymainTextView1.setText(getString(R.string.slowdns));
				view_all_layout.setEnabled(false);
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.GONE);
				ViewGone.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.GONE);
				payload_menu.setVisibility(View.GONE);
				customPayloadSwitch.setClickable(false);
				customPayloadSwitch.setVisibility(View.GONE);
				break;
				
			case Settings.bTUNNEL_TYPE_SSL_RP:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSL_RP);
				activitymainTextView1.setText(getString(R.string.sslrp));
				proxyInputLayout.setVisibility(View.VISIBLE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.VISIBLE);
				cmodsniLayout.setVisibility(View.VISIBLE);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setClickable(true);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				break;
		}

		int msgVisibility = View.GONE;
		int loginVisibility = View.GONE;
		String msgText = "";
		boolean enabled_radio = !isRunning;

		if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			
			
			String msg = mConfig.getPrivString(Settings.CONFIG_MENSAGEM_KEY);
			if (!msg.isEmpty()) {
				msgText = msg.replace("\n", "<br/>");
				msgVisibility = View.VISIBLE;
			}
			
			if (mConfig.getPrivString(Settings.PROXY_IP_KEY).isEmpty() ||
					mConfig.getPrivString(Settings.PROXY_PORTA_KEY).isEmpty()) {
				enabled_radio = false;
			}
		}

		
		configMsgText.setText(msgText.isEmpty() ? "" : Html.fromHtml(msgText));
		configMsgLayout.setVisibility(msgVisibility);
		
		// desativa/ativa radio group
		for (int i = 0; i < metodoConexaoRadio.getChildCount(); i++) {
			metodoConexaoRadio.getChildAt(i).setEnabled(enabled_radio);
		}
	}
	
	
	private synchronized void doSaveData() {
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		if (mainLayout != null && !isFinishing())
			mainLayout.requestFocus();
		
		if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			if (payloadEdit != null && !prefs.getBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true)) {
				edit.putString(Settings.CUSTOM_PAYLOAD_KEY, payloadEdit.getText().toString());
			}
			
		}
		int tunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
		if (tunnelType == Settings.bTUNNEL_TYPE_SLOWDNS) {
			edit.putString(Settings.SERVIDOR_KEY, "127.0.0.1");
			edit.putString(Settings.SERVIDOR_PORTA_KEY, "2222");

		}

		edit.apply();
	}


	/**
	 * Tunnel SSH
	 */

	public void startOrStopTunnel(Activity activity) {
		if (SkStatus.isTunnelActive()) {
			TunnelManagerHelper.stopSocksHttp(activity);
			loadRewardedVideoAd();
		}
		else {
			// oculta teclado se vísivel, tá com bug, tela verde
			//Utils.hideKeyboard(activity);
			if(startSSHCheckbox.isChecked()){
				this.startService(new Intent(this, InjectorService.class).setAction("START"));
				Toast.makeText(SocksHttpMainActivity.this, "Start SSH is running",
							   Toast.LENGTH_SHORT).show();
			}else{
				this.startService(new Intent(this, InjectorService.class).setAction("STOP"));
				Toast.makeText(SocksHttpMainActivity.this, "Start SSH is off",
							   Toast.LENGTH_SHORT).show();
			}
			loadRewardedVideoAd();
			Settings config = new Settings(activity);
			
			Intent intent = new Intent(activity, LaunchVpn.class);
			intent.setAction(Intent.ACTION_MAIN);
			
			if (config.getHideLog()) {
				intent.putExtra(LaunchVpn.EXTRA_HIDELOG, true);
			}
			
			activity.startActivity(intent);
		}
	}
	
	
	

	private void setPayloadSwitch(int tunnelType, boolean isCustomPayload) {
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		boolean isRunning = SkStatus.isTunnelActive();

		customPayloadSwitch.setChecked(isCustomPayload);

		if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
			payloadEdit.setEnabled(false);
			
			if (mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY).isEmpty()) {
				customPayloadSwitch.setEnabled(false);
			}
			else {
				customPayloadSwitch.setEnabled(!isRunning);
			}
			
			if (!isCustomPayload && tunnelType == Settings.bTUNNEL_TYPE_SSH_PROXY)
				payloadEdit.setText(Settings.PAYLOAD_DEFAULT);
			else
				payloadEdit.setText("*******");
		}
		else {
			customPayloadSwitch.setEnabled(!isRunning);

			if (isCustomPayload) {
				payloadEdit.setText(mConfig.getPrivString(Settings.CUSTOM_PAYLOAD_KEY));
				payloadEdit.setEnabled(!isRunning);
			}
			else if (tunnelType == Settings.bTUNNEL_TYPE_SSH_PROXY) {
				payloadEdit.setText(Settings.PAYLOAD_DEFAULT);
				payloadEdit.setEnabled(false);
			} else if (tunnelType == Settings.bTUNNEL_TYPE_SSH_DIRECT) {
				payloadEdit.setText(Settings.PAYLOAD_DEFAULT);
				payloadEdit.setEnabled(false);
			} else if (tunnelType == Settings.bTUNNEL_TYPE_PAY_SSL) {
				payloadEdit.setText(Settings.PAYLOAD_DEFAULT);
				payloadEdit.setEnabled(false);
			}else if (tunnelType == Settings.bTUNNEL_TYPE_SSL_RP) {
				payloadEdit.setText(Settings.PAYLOAD_DEFAULT);
				payloadEdit.setEnabled(false);
			} 
		}

		if (isCustomPayload || tunnelType == Settings.bTUNNEL_TYPE_SSH_PROXY) {
			payloadLayout.setVisibility(View.VISIBLE);
		} else if (isCustomPayload || tunnelType == Settings.bTUNNEL_TYPE_SSH_DIRECT) {
			payloadLayout.setVisibility(View.VISIBLE);
		}else if (isCustomPayload || tunnelType == Settings.bTUNNEL_TYPE_PAY_SSL) {
			payloadLayout.setVisibility(View.VISIBLE);
		}else if (isCustomPayload || tunnelType == Settings.bTUNNEL_TYPE_SSL_RP) {
			payloadLayout.setVisibility(View.VISIBLE);
		} else {
			payloadLayout.setVisibility(View.GONE);
		}
	}

	public void setStarterButton(Button starterButton, Activity activity) {
		String state = SkStatus.getLastState();
		boolean isRunning = SkStatus.isTunnelActive();

		if (starterButton != null) {
			int resId;
			
			SharedPreferences prefsPrivate = new Settings(activity).getPrefsPrivate();

			if (ConfigParser.isValidadeExpirou(prefsPrivate
					.getLong(Settings.CONFIG_VALIDADE_KEY, 0))) {
				resId = R.string.expired;
				starterButton.setEnabled(false);

				if (isRunning) {
					startOrStopTunnel(activity);
				}
			}
			else if (prefsPrivate.getBoolean(Settings.BLOQUEAR_ROOT_KEY, false) &&
					ConfigParser.isDeviceRooted(activity)) {
			   resId = R.string.blocked;
			   starterButton.setEnabled(false);
			   
			   Toast.makeText(activity, R.string.error_root_detected, Toast.LENGTH_SHORT)
					.show();

			   if (isRunning) {
				   startOrStopTunnel(activity);
			   }
			}
			else if (SkStatus.SSH_INICIANDO.equals(state)) {
				resId = R.string.stop;
				starterButton.setEnabled(false);
			}
			else if (SkStatus.SSH_PARANDO.equals(state)) {
				resId = R.string.state_stopping;
				starterButton.setEnabled(false);
		
				//stopdns();
				
			}
			else {
				resId = isRunning ? R.string.stop : R.string.start;
				starterButton.setEnabled(true);
			}

			starterButton.setText(resId);
		}
	}
	

	
	@Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
    }
	
	private boolean isMostrarSenha = false;
	
	@Override
	public void onClick(View p1)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		switch (p1.getId()) {
			case R.id.activity_starterButtonMain:
				doSaveData();
				startOrStopTunnel(this);
				break;

			case R.id.activity_mainInputProxyLayout:
				if (!prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
					doSaveData();

					DialogFragment fragProxy = new ProxyRemoteDialogFragment();
					fragProxy.show(getSupportFragmentManager(), "proxyDialog");
				}
				break;
			case R.id.useDns:
				if(dnsCheckBox.isChecked()){
					Snackbar.make(p1, Html.fromHtml("<font color='#0A87FB'>Improved privacy and bypass Internet Censorship</font>"), Snackbar.LENGTH_LONG).setAction("SET DNS", new OnClickListener()
						{

							@Override
							public void onClick(View p1)
							{
								SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
								if(dnsCheckBox.getText().toString().equals("DNS (Default DNS)")){
									mPref.edit().putBoolean("Default_dns",true).apply();
									mPref.edit().putBoolean("Google_dns",false).apply();
									mPref.edit().putBoolean("Primary_dns",false).apply();
								}else if(dnsCheckBox.getText().toString().equals("DNS (Google DNS)")){
									mPref.edit().putBoolean("Default_dns",false).apply();
									mPref.edit().putBoolean("Google_dns",true).apply();
									mPref.edit().putBoolean("Primary_dns",false).apply();
									mPref.edit().putString(Settings.DNSRESOLVER_KEY1,"8.8.8.8").apply();
									mPref.edit().putString(Settings.DNSRESOLVER_KEY2,"8.8.4.4").apply();
								}else if(dnsCheckBox.getText().toString().equals("DNS (Primary DNS)")){
									mPref.edit().putBoolean("Default_dns",false).apply();
									mPref.edit().putBoolean("Google_dns",false).apply();
									mPref.edit().putBoolean("Primary_dns",true).apply();

								}else{
									mPref.edit().putBoolean(Settings.DNSFORWARD_KEY,false).apply();
									mPref.edit().putString(Settings.DNSTYPE_KEY, Settings.DNS_DEFAULT_KEY).apply();
									//custom.setText(mPref.getString(Settings.DNSTYPE_KEY,Settings.DNS_DEFAULT_KEY));
								}
								Intent TunnDNS = new Intent(SocksHttpMainActivity.this, CustomDNS.class);
								startActivity(TunnDNS);
							}


						}).show();
				} else {
					Toast.makeText(SocksHttpMainActivity.this, "DNS (Google DNS) is off",
								   Toast.LENGTH_SHORT).show();
				}
				break;
			
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup p1, int p2)
	{
		SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();

		switch (p1.getCheckedRadioButtonId()) {
			case R.id.activity_mainSSHDirectRadioButton:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.GONE);
				customPayloadSwitch.setClickable(true);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				//activitymainTextView1.setText("SSH DIRECT");
				break;

			case R.id.activity_mainSSHProxyRadioButton:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_PROXY);
				proxyInputLayout.setVisibility(View.VISIBLE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.GONE);
				customPayloadSwitch.setClickable(true);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				//activitymainTextView1.setText("SSH + HTTP PROXY");
				break;
			case R.id.activity_mainSSLProxyRadioButton:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_SSLTUNNEL);
				edit.putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true).apply();
				customPayloadSwitch.setClickable(false);
				customPayloadSwitch.setChecked(false);
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.VISIBLE);
				payload_menu.setVisibility(View.GONE);
				cmodsniLayout.setVisibility(View.VISIBLE);
				customPayloadSwitch.setVisibility(View.GONE);
				//activitymainTextView1.setText("SSL/TLS");
				break;
			case R.id.activity_mainSSLPayloadRadioButton:
				edit.putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_PAY_SSL);
				proxyInputLayout.setVisibility(View.GONE);
				payloadLayout.setVisibility(View.VISIBLE);
				ViewGone.setVisibility(View.VISIBLE);
				cmodsniLayout.setVisibility(View.VISIBLE);
				payload_menu.setVisibility(View.VISIBLE);
				customPayloadSwitch.setClickable(true);
				customPayloadSwitch.setVisibility(View.VISIBLE);
				//activitymainTextView1.setText("SSL + HTTP PAYLOAD");
				break;
		}

		edit.apply();

		doSaveData();
		doUpdateLayout();
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		switch (p1.getId()) {
			case R.id.activity_mainCustomPayloadSwitch:
				edit.putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, !p2);
				setPayloadSwitch(prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT), p2);
				break;
		}

		edit.apply();

		doSaveData();
	}
	
	protected void showBoasVindas() {
		new AlertDialog.Builder(this)
            . setTitle(R.string.attention)
            . setMessage(R.string.first_start_msg)
			. setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface di, int p) {
					// ok
				}
			})
			. setCancelable(false)
            . show();
	}
	
	@Override
	public void updateState(final String state, String msg, int localizedResId, final ConnectionStatus level, Intent intent)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				doUpdateLayout();
				
			}
		});
		
		switch (state) {
			case SkStatus.SSH_CONECTADO:
				// carrega ads banner
				//showInterstitial();
				updatez(false);
				
				updateApp(false);
				if (adsBannerView != null && TunnelUtils.isNetworkOnline(SocksHttpMainActivity.this)) {
					adsBannerView.setAdListener(new AdListener() {
							@Override
							public void onAdLoaded() {
								if (adsBannerView != null && !isFinishing()) {
									adsBannerView.setVisibility(View.VISIBLE);
								}
							}
						});
					adsBannerView.postDelayed(new Runnable() {
							@Override
							public void run() {
								loadInterstitialAds();
								//showInterstitial();
								loadRewardedVideoAd();
								if (adsBannerView != null && !isFinishing()) {
									adsBannerView.loadAd(new AdRequest.Builder()
														 .build());
								}
							}
						}, 5000);
				}
			break;
		}
	}
	private void loadInterstitialAds(){
		success = new InterstitialAd(this);
		success.setAdUnitId("ca-app-pub-4327217352829821/2371387288");
		success.loadAd(new AdRequest.Builder().build());
		success.setAdListener(new AdListener() {

				private Context applicationContext;

				@Override
				public void onAdLoaded() {
					// Code to be executed when an ad finishes loading.
					loadInterstitial();
				}

				@Override
				public void onAdFailedToLoad(int errorCode) {
					// Code to be executed when an ad request fails.
				}

				@Override
				public void onAdOpened() {
					// Code to be executed when the ad is displayed.
				}

				@Override
				public void onAdLeftApplication() {
					// Code to be executed when the user has left the app.
				}

				@Override
				public void onAdClosed() {
					loadInterstitialAds();
					//Toast.makeText(getApplicationContext(),"Thanks for Supporting ❤️",Toast.LENGTH_SHORT).show();
					// Code to be executed when the interstitial ad is closed.
				}
			});
	}

	/**
	 * Recebe locais Broadcast
	 */

	private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

            if (action.equals(UPDATE_VIEWS) && !isFinishing()) {
				doUpdateLayout();
			}
			else if (action.equals(OPEN_LOGS)) {
				if (mDrawer != null && !isFinishing()) {
					DrawerLayout drawerLayout = mDrawer.getDrawerLayout();
					
					if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
						drawerLayout.openDrawer(GravityCompat.START);
					}
				}
			}
        }
    };


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Menu Itens
		switch (item.getItemId()) {
			case R.id.miPaygen:
				SharedPreferences jprefs = mConfig.getPrefsPrivate();
                if (SkStatus.isTunnelActive()) {
                    Toast.makeText(this, R.string.error_tunnel_service_execution,
                                   Toast.LENGTH_SHORT).show();
                } else if (jprefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
                    Toast.makeText(this, R.string.payload_locked_msg,
                                   Toast.LENGTH_SHORT).show();
                } else {
					generator();
                }          
				break;
			case R.id.miLimparConfig:
				if (!SkStatus.isTunnelActive()) {
					DialogFragment dialog = new ClearConfigDialogFragment();
					dialog.show(getSupportFragmentManager(), "alertClearConf");
				} else {
					Toast.makeText(this, R.string.error_tunnel_service_execution, Toast.LENGTH_SHORT)
						.show();
				}
				break;

			case R.id.miSettings:
				Intent intent = new Intent(SocksHttpMainActivity.this, ConfigGeralActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				SocksHttpMainActivity.this.startActivity(intent);
				break;

			case R.id.miSettingImportar:
				if (SkStatus.isTunnelActive()) {
					Toast.makeText(this, R.string.error_tunnel_service_execution,
						Toast.LENGTH_SHORT).show();
				}
				else {
					Intent intentImport = new Intent(this, ConfigImportFileActivity.class);
					startActivity(intentImport);
				}
				break;
				
			case R.id.miCheckIP:
				Intent intent5 = new Intent(this, MyIP_Address.class);
				startActivity(intent5);
				break;
				
			case R.id.miHostChecker:
				Intent intent6 = new Intent(this, HostChecker.class);
				startActivity(intent6);
				break;

			case R.id.miSettingExportar:
				SharedPreferences prefs = mConfig.getPrefsPrivate();
				
				if (SkStatus.isTunnelActive()) {
					Toast.makeText(this, R.string.error_tunnel_service_execution,
						Toast.LENGTH_SHORT).show();
				}
				else if (prefs.getBoolean(Settings.CONFIG_PROTEGER_KEY, false)) {
					Toast.makeText(this, R.string.error_settings_blocked,
						Toast.LENGTH_SHORT).show();
				}
				else {
					Intent intentExport = new Intent(this, ConfigExportFileActivity.class);
					startActivity(intentExport);
				}
				break;

				// logs opções
			case R.id.miLimparLogs:
				mDrawer.clearLogs();
			break;
			
			case R.id.Share_Logs:
				Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT,SkStatus.CopyLogs());
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, SkStatus.CopyLogs());
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
				break;
			
			case R.id.miCopyLogs:
				int sdk = android.os.Build.VERSION.SDK_INT;
				if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
					android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					clipboard.setText(getLog());
				} else {
					android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
					android.content.ClipData clip = android.content.ClipData.newPlainText("text label",getLog());
					clipboard.setPrimaryClip(clip);
				}
				Toast.makeText(SocksHttpMainActivity.this, "log copied", Toast.LENGTH_SHORT).show();
				
				break;
			
			case R.id.miExit:
				if (Build.VERSION.SDK_INT >= 16) {
					finishAffinity();
				}
				
				System.exit(0);
			break;
			
			case R.id.miUpdateApp:
				updateApp(false);
				
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	private String getLog()
    {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < mLogAdapter.getItemCount(); i++) {
			str.append(mLogAdapter.getItem(i) +"\n");
		}
		return str.toString();
	}
	
	@Override
	public void onBackPressed() {
		DrawerLayout layout = mDrawer.getDrawerLayout();

		
			showExitDialog();

	}

	@Override
    public void onResume() {
        super.onResume();

		mDrawer.onResume();
		
		//doSaveData();
		//doUpdateLayout();
		if (adsBannerView != null) {
			adsBannerView.resume();
		}
		SkStatus.addStateListener(this);
		
		
    }

	@Override
	protected void onPause()
	{
		super.onPause();
		
		doSaveData();
		
		SkStatus.removeStateListener(this);
		
		if (adsBannerView != null) {
			adsBannerView.resume();
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		mDrawer.onDestroy();

		LocalBroadcastManager.getInstance(this)
			.unregisterReceiver(mActivityReceiver);
			
		if (adsBannerView != null) {
			adsBannerView.resume();
		}
	}


	/**
	 * DrawerLayout Listener
	 */

	@Override
	public void onDrawerOpened(View view) {
		if (view.getId() == R.id.activity_mainLogsDrawerLinear) {
			toolbar_main.getMenu().clear();
			getMenuInflater().inflate(R.menu.logs_menu, toolbar_main.getMenu());
		}
	}

	@Override
	public void onDrawerClosed(View view) {
		if (view.getId() == R.id.activity_mainLogsDrawerLinear) {
			toolbar_main.getMenu().clear();
			getMenuInflater().inflate(R.menu.main_menu, toolbar_main.getMenu());
		}
	}

	@Override
	public void onDrawerStateChanged(int stateId) {}
	@Override
	public void onDrawerSlide(View view, float p2) {}

	
	/**
	 * Utils
	 */

	public static void updateMainViews(Context context) {
		Intent updateView = new Intent(UPDATE_VIEWS);
		LocalBroadcastManager.getInstance(context)
			.sendBroadcast(updateView);
	}
	
	public void showExitDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).
			create();
		dialog.setTitle(getString(R.string.attention));
		dialog.setMessage(getString(R.string.alert_exit));

		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.
				string.exit),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					Utils.exitAll(SocksHttpMainActivity.this);
				}
			}
		);

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.
				string.minimize),
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// minimiza app
					Intent startMain = new Intent(Intent.ACTION_MAIN);
					startMain.addCategory(Intent.CATEGORY_HOME);
					startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(startMain);
				}
			}
		);

		dialog.show();
	}
}

