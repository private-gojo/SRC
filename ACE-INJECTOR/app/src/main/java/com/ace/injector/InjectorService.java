package com.ace.injector;

import android.os.*;
import android.content.*;
import android.app.*;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;

import android.os.Build.VERSION;
import android.annotation.*;
import java.lang.reflect.*;
import android.graphics.*;
import java.io.*;
import java.net.*;
import android.preference.*;
import android.net.*;
import java.util.TimerTask;
import java.util.Timer;
import com.ultrasshservice.config.Settings;
import com.ace.injectorpro.R;


public class InjectorService extends Service
{
    //private final IBinder mBinder = new LocalBinderInjector();
    private NotificationManager notificationManager;
    private final String CHANNEL_ID = "harlies_channel_1";
    private final String CHANNEL_NAME = "harlies_channel";
    private static boolean mNotificationAlwaysVisible = false;
    private Settings mConfig;
    private static Socket output = null;
    private SharedPreferences mPref;
    private Thread mThread;
    private Socket input;
    private ServerSocket listen_socket;
    private Handler mHandler;
    private Builder builder;

    public class LocalBinderInjector extends Binder {

        public InjectorService getService() {
            return InjectorService.this;
        }
    }
    @Override
    public IBinder onBind(Intent p1)
    {

        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mConfig = new Settings(this);
        mHandler = new Handler();
        builder = new Builder(this);
        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        String action = intent.getAction();
        if (action == "START")
        {
            if (mThread != null)
            {
                mThread.interrupt();
            }
            mThread = new Thread(ssl, "mThread");
            mThread.start();
            /*new Timer().schedule(new TimerTask(){
			 @Override
			 public void run(){
			 mHandler.post(new Runnable() {
			 @Override
			 public void run() {

			 }
			 });
			 }
			 }, 0,1000);*/

        }
        else if (action == "STOP")
        {
			if (mThread != null)
            {
                mThread.interrupt();
                stopForeground(true);
                stopSelf();
            }
        }
        return 1;
    }

    Runnable http = new Runnable() {
        @Override
        public void run()
        {
            try
            {
                int portaLocal = Integer.parseInt(mConfig.getPrivString(Settings.PORTA_LOCAL_KEY));
                listen_socket = new ServerSocket(portaLocal);
                listen_socket.setReuseAddress(true);
                while (true)
                {
                    input = listen_socket.accept();
                    input.setSoTimeout(0);
                    //output = new HTTPSupport(InjectorService.this,input).socket();
                    if (input != null)
                    {
                        input.setKeepAlive(true);
                    }
                    if (output != null)
                    {
                        output.setKeepAlive(true);
                    }
                    if (output == null)
                    {
                        output.close();
                    }
                    else if (output.isConnected())
                    { 
                        /* sc1 = new HTTPThread(InjectorService.this,input, output, true);
                         sc2 = new HTTPThread(InjectorService.this,output, input, false);
                         sc1.setDaemon(true);
                         sc1.start();
                         sc2.setDaemon(true);
                         sc2.start();*/
                    }
                }
            }
            catch (Exception e)
            {

            }
        }
    };
    Runnable ssl = new Runnable() {
        @Override
        public void run()
        {
            /*  if(mPref.getBoolean("setTimer",false)){
             showNotification(HarliesMain.showNotifi(), "Remaining Time", mNotificationAlwaysVisible, System.currentTimeMillis());
             }else{*/
            showNotification("Injection service is running", "SocksHttp V2", mNotificationAlwaysVisible, System.currentTimeMillis());
            // }
        }
    };


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void lpNotificationExtras(Builder builder) {
        builder.setCategory("service");
        builder.setLocalOnly(true);
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void jbNotificationExtras(boolean z, Builder builder) {
        if (z) {
            try {
                Class[] clsArr = new Class[1];
                clsArr[0] = Integer.TYPE;
                Method method = builder.getClass().getMethod("setPriority", clsArr);
                Object[] objArr = new Object[1];
                objArr[0] = new Integer(-2);
                method.invoke(builder, objArr);
                clsArr = new Class[1];
                clsArr[0] = Boolean.TYPE;
                method = builder.getClass().getMethod("setUsesChronometer", clsArr);
                objArr = new Object[1];
                objArr[0] = new Boolean(true);
                method.invoke(builder, objArr);
            } catch (Exception e) {
            }
        }
    }



    private void showNotification(String str, String str2, boolean z, long j) {
        NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
        Builder builder = new Builder(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel1 = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel1.setDescription(CHANNEL_NAME);
            channel1.enableLights(true);
            channel1.setLightColor(Color.RED);
            notificationManager.createNotificationChannel(channel1);
            builder = new Builder(this, CHANNEL_ID);
        }
        else
        {
            builder = new Builder(this);
        }
        builder.setContentTitle(str2).
            setContentText(str).
            setOnlyAlertOnce(true).
            setOngoing(true).
            setSmallIcon(R.drawable.ic_launcher);
        if (j != ((long) 0)) {
            builder.setWhen(j);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            jbNotificationExtras(z, builder);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lpNotificationExtras(builder);
        }
        if (!(str2 == null || str2.equals(""))) {
            builder.setTicker(str2);
        }
        int notificationId = 2;
        Notification notification = builder.getNotification();
        notificationManager.notify(notificationId, notification);
        startForeground(notificationId, notification);
    }
}
