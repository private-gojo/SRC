package app.one.secondvpnlite;


import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import app.one.secondvpnlite.configs.ExportConfig;
import app.one.secondvpnlite.configs.ImportConfig;
import app.one.secondvpnlite.drawer.DrawerLog;
import app.one.secondvpnlite.logs.AppLogManager;
import app.one.secondvpnlite.service.TunnelManager;
import app.one.secondvpnlite.settings.AppSettings;
import app.one.secondvpnlite.tunnel.TunnelUtils;

public class MainActivity extends AppCompatActivity implements DrawerLayout.DrawerListener,View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    public static SharedPreferences app_prefs;
    public static SharedPreferences start_msg;
    public static final int START_VPN_PROFILE = 70;
    private final static String TAG = "MainActivity";

    private Thread mTunnelThread;
    private TunnelManager mTunnelManager;


    public static Handler UIHandler;

    static
    {
        UIHandler = new Handler(Looper.getMainLooper());
    }



    public static void runOnUI(Runnable runnable) {
        RelayMainUi.runOnUI(runnable);
    }

    public static void setButtonStatus(String status) {
        RelayMainUi.setButtonStatus(status);
    }

    public static void updateUIAfterImport() {
        RelayMainUi.updateUIAfterImport();
    }

    @SuppressLint("StaticFieldLeak")
    public static Button start_button ;
    @SuppressLint("StaticFieldLeak")
    public static TextView servidor_ssh;
    @SuppressLint("StaticFieldLeak")
    public static TextView servidor_proxy;
    @SuppressLint("StaticFieldLeak")
    public static TextView custom_sni;
    @SuppressLint("StaticFieldLeak")
    public static TextView custom_payload;
    @SuppressLint("StaticFieldLeak")
    public static TextView user_and_pass;
    public static AppCompatRadioButton modo_http;
    public static AppCompatRadioButton modo_https;
    public static SwitchCompat payload_after_tls;
    public static SwitchCompat direct_mode;
    @SuppressLint("StaticFieldLeak")
    public static TextView config_msg;
    @SuppressLint("StaticFieldLeak")
    static LinearLayout servidor_ssh_layout;
    @SuppressLint("StaticFieldLeak")
    static LinearLayout servidor_proxy_layout;
    @SuppressLint("StaticFieldLeak")
    static LinearLayout custom_sni_layout;
    @SuppressLint("StaticFieldLeak")
    static LinearLayout payload_layout;
    @SuppressLint("StaticFieldLeak")
    static LinearLayout user_and_pass_layout;

    private boolean configIsOk;
    private boolean exportWithoutLogin = false;
    private boolean intentToImport = false;

    DrawerLog mDrawer;
    Toolbar app_toolbar;

    static AdView mAdView;
    static InterstitialAd mInterstitialAd;
    public static Context sContext;
    public static Activity sActivity;
    public static PowerManager.WakeLock wakeLock;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startApp();


    }

    private void startApp() {
        RelayMainStartup.start(this);
    }

    /*private void ccb() {
        if (SecondVPN.getIsCustomFileIsLocked() && !servidor_ssh.getText().toString().contains(getAst())){
             f(c());
        }
    }*/

    @Override
    public void onBackPressed()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

        super.onBackPressed();
    }

    private boolean duringisLoad = false;
    private static void setup_ad() {
        RelayMainAds.setup(sContext, mAdView);
    }

    static int delay = 4000;
    static boolean isShowing = false;

    public static void ladi() {
        RelayMainAds.loadInterstitialIfNeeded(sActivity, sContext, app_prefs, mAdView);
    }

    private void clear_textviews(){
        try{
                servidor_ssh.setText("");
                custom_payload.setText("");
                user_and_pass.setText("");
                servidor_proxy.setText("");
                direct_mode.setText("");
                custom_sni.setText("");
                user_and_pass.setText("");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateLayoutWithConfig1() {
        RelayMainUi.updateLayoutWithConfig1();
    }

    public static void updateLayoutWithConfig2() {
        RelayMainUi.updateLayoutWithConfig2();
    }

    private static void updateLayoutWithConfig3() {
        RelayMainUi.updateLayoutWithConfig3();
    }

    void showStartMsg() {
        RelayMainStartup.showStartMessage(this);
    }

    public void getPermissionAndroid13() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void stopVPNService(){
        try{
            TunnelManager.stopForwarderSocks();
            TunnelManager.isToStopService = true;
            mTunnelManager = new TunnelManager();
            mTunnelThread = new Thread(mTunnelManager);
            mTunnelThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVPNService() {
        RelayMainVpnFlow.requestPermission(
                this,
                () -> onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null)
        );
    }



    private boolean checkConfig(boolean istoSave) {
        RelayMainValidation.ValidationResult result =
                RelayMainValidation.validate(this, istoSave);
        configIsOk = result.getConfigIsOk();
        exportWithoutLogin = result.getExportWithoutLogin();
        return configIsOk;
    }

    public static boolean checkValidade(long validadeDateMillis) {
        return RelayMainValidation.isExpired(validadeDateMillis);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        RelayMainVpnFlow.handlePermissionResult(
                this,
                requestCode,
                resultCode,
                () -> checkConfig(false),
                () -> showLog(),
                () -> startTunnelManagerFromFlow()
        );
    }

    void startTunnelManagerFromFlow() {
        mTunnelManager = new TunnelManager();
        mTunnelThread = new Thread(mTunnelManager);
        mTunnelThread.start();
    }


    private void showLog() {
        if (mDrawer != null && !isFinishing()) {
            DrawerLayout drawerLayout = mDrawer.getDrawerLayout();

            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        }
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        RelayMainEvents.onCheckedChanged(this, intentToImport, this::savePreferences);
    }

    @Override
    protected void onPause()
    {
        savePreferences();
        //Appodeal.destroy(Appodeal.BANNER_VIEW | Appodeal.INTERSTITIAL);
        super.onPause();
    }


    @Override
    protected void onStop() {
        if (!duringisLoad){

        }
        super.onStop();
    }


    @Override
    protected void onResume()
    {

        //ccb();
        if (!intentToImport){
            savePreferences();
           // updateUIAfterImport();

        }else{
            intentToImport = false;
        }

        //if (SecondVPN.getCurrentVpnStatus().equals("CONECTADO")){
        if (TunnelManager.isServiceRunning){
            /*if (mInterstitialAd == null){
                setup_ad();
            }*/

            if (mInterstitialAd == null){
                setup_ad();
            }

        }

        super.onResume();
    }



    @Override
    protected void onDestroy()
    {
        savePreferences();
        mAdView.destroy();
        super.onDestroy();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Menu Itens
        switch (item.getItemId()) {
            case R.id.import_file:
                intentToImport = true;
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else{
                    Intent import_file = new Intent(this, ImportConfig.class);
                    this.startActivity(import_file);
                }

            break;

            case R.id.clearlogs:
                AppLogManager.clearLog();
                //AppLogManager.addLog(getString(R.string.cleared_logs));
                break;
            case R.id.clearconfig:
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else{
                   // AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
                    builder.setTitle(getString(R.string.reset_app_config_title));
                    builder.setCancelable(true)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SecondVPN.setDefaultPrefs();
                                    clear_textviews();
                                    updateUIAfterImport();
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();

                }
                break;
            case R.id.forcestop:
                android.os.Process.killProcess(android.os.Process.myPid());

                break;
            case R.id.cleardata:

                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else{
                   // AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.AlertDialogTheme);
                    builder.setTitle(getString(R.string.reset_app_title));
                    builder.setMessage(getString(R.string.reset_app_txt))
                            .setCancelable(true)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    try{
                                        String packageName = getApplicationContext().getPackageName();
                                        Runtime runtime = Runtime.getRuntime();
                                        runtime.exec("pm clear "+packageName);
                                    } catch (Exception e) {
                                        Toast.makeText(MainActivity.this, getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }



                break;

            case R.id.export_file:
                savePreferences();
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else if (!checkConfig(true)){
                    if(exportWithoutLogin){
                        if (!SecondVPN.getIsCustomFileIsLocked()){
                            //ATIVIDADE EXPORTAR
                            Intent export_file = new Intent(this, ExportConfig.class);
                            this.startActivity(export_file);
                        }else{
                            Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(this,this.getString(R.string.empty_configs_export),Toast.LENGTH_LONG).show();
                    }
                }else if (SecondVPN.getIsCustomFileLockSettings()){
                    Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();
                }else{
                    if (!SecondVPN.getIsCustomFileIsLocked()){
                        //ATIVIDADE EXPORTAR
                        Intent export_file = new Intent(this, ExportConfig.class);
                        this.startActivity(export_file);
                    }else{
                        Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();
                    }
                }
            break;

            case R.id.settings:
                if (start_button.getText().toString().equals(this.getString(R.string.stop))) {
                    Toast.makeText(this,this.getString(R.string.only_disconnected),Toast.LENGTH_SHORT).show();
                }else if (SecondVPN.getIsCustomFileLockSettings()){
                    Toast.makeText(this,this.getString(R.string.block_by_config_author),Toast.LENGTH_LONG).show();

                }else{
                    Intent settings = new Intent(this, AppSettings.class);
                    this.startActivity(settings);
                }
            break;

            case R.id.minimize:
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
             break;

            case R.id.exit_app:
                finishAffinity();
            break;

        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        {
            switch (v.getId()) {
                case R.id.activity_StartConnection:
                    setStartOrStopVPN(this);
                    break;

                case R.id.logs:
                    showLog();
                    break;

            }

        }
    }

    private void setStartOrStopVPN(MainActivity mainActivity) {
        if (!start_button.getText().toString().equals(this.getString(R.string.stop))) {
            //SET PREFERENCES
            savePreferences();
            startVPNService();
            Log.d("MainActivity","Start VPN Tunnel");
        }else{
            stopVPNService();
            Log.d("MainActivity","Stop VPN Tunnel");
        }
    }

    private void savePreferences(){
        if (SecondVPN.getIsCustomFileIsLocked()){
            if (!SecondVPN.isLockLoginEdit()){
                app_prefs.edit().putString("SSH_AUTH_DATA",user_and_pass.getText().toString()).apply();
            }
        }
        if (!SecondVPN.getIsCustomFileIsLocked()){
            //SALVA USUARIO E SENHA
            app_prefs.edit().putString("SSH_AUTH_DATA",user_and_pass.getText().toString()).apply();
            //SALVA POR MODO DE CONEXAO
            if (modo_http.isChecked()){
                if (direct_mode.isChecked()){
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("PAYLOAD_KEY",custom_payload.getText().toString()).apply();

                }else{
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("PAYLOAD_KEY",custom_payload.getText().toString()).apply();
                    app_prefs.edit().putString("PROXY_IP_DOMAIN",servidor_proxy.getText().toString()).apply();
                }

                //}else if (SecondVPN.getConnectionMode().equals("MODO_HTTPS")){
            }
            if (modo_https.isChecked()){
                if (payload_after_tls.isChecked()){
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("PAYLOAD_KEY",custom_payload.getText().toString()).apply();
                    app_prefs.edit().putString("CUSTOM_SNI",custom_sni.getText().toString()).apply();

                }else{
                    app_prefs.edit().putString("SSH_SERVER_DOMAIN",servidor_ssh.getText().toString()).apply();
                    app_prefs.edit().putString("CUSTOM_SNI",custom_sni.getText().toString()).apply();

                }
            }
        }
    }

    public static void disableInterfaceOnConnect() {
        RelayMainUi.disableInterfaceOnConnect();
    }

    public static void enableInterfaceOnConnect() {
        RelayMainUi.enableInterfaceOnConnect();
    }

    /**
     * Called when a drawer's position changes.
     *
     * @param drawerView  The child view that was moved
     * @param slideOffset The new offset of this drawer within its range, from 0-1
     */
    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    /**
     * Called when a drawer has settled in a completely open state.
     * The drawer is interactive at this point.
     *
     * @param drawerView Drawer view that is now open
     */
    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    /**
     * Called when a drawer has settled in a completely closed state.
     *
     * @param drawerView Drawer view that is now closed
     */
    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        if (drawerView.getId() == R.id.activity_mainLogsDrawerLinear) {
            app_toolbar.getMenu().clear();
            getMenuInflater().inflate(R.menu.main_menu, app_toolbar.getMenu());
        }
    }

    /**
     * Called when the drawer motion state changes. The new state will
     * be one of ,  or .
     *
     * @param newState The new drawer motion state
     */
    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
