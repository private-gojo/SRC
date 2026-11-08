package com.ultrasshservice.tunnel;

import android.content.*;
import com.ultrasshservice.util.*;
import java.io.*;
import com.ultrasshservice.config.*;
import com.ultrasshservice.tunnel.vpn.*;
import com.ultrasshservice.logger.*;

public class DNSTunnelThread extends Thread {

	private Context mContext;
	private static final String DNS_BIN = "libdns";
	private Process dnsProcess;
	private File filedns;
	private Settings mConfig;

	public DNSTunnelThread(Context context) {
		mContext = context;
		mConfig = new Settings(context);

	}

	@Override
	public void run(){
		try {

			SharedPreferences slowprefs = mConfig.getPrefsPrivate();


			String mDns = mConfig.getPrivString(Settings.DNS_KEY);
			String mchave = mConfig.getPrivString(Settings.CHAVE_KEY);
			String mnameserver = mConfig.getPrivString(Settings.NAMESERVER_KEY);
			 String chave = slowprefs.getString(Settings.CHAVE_KEY, mDns);
			 String nameserver = slowprefs.getString(Settings.NAMESERVER_KEY, mchave);
			 String dns = slowprefs.getString(Settings.DNS_KEY, mnameserver);
			StringBuilder cmd1 = new StringBuilder();
			filedns = CustomNativeLoader.loadNativeBinary(mContext, DNS_BIN, new File(mContext.getFilesDir(),DNS_BIN));

			if (filedns == null){
				throw new IOException("DNS bin not found");
			}

			cmd1.append(filedns.getCanonicalPath());
			cmd1.append(" -udp "+ dns + ":53   -pubkey "+ chave + " " + nameserver + " 127.0.0.1:2222");
			dnsProcess = Runtime.getRuntime().exec(cmd1.toString());

			StreamGobbler.OnLineListener onLineListener = new StreamGobbler.OnLineListener(){
				@Override
				public void onLine(String log){
					//SkStatus.logInfo("<b>DNS Client: </b>" + log);
				}
			};
			StreamGobbler stdoutGobbler = new StreamGobbler(dnsProcess.getInputStream(), onLineListener);
			StreamGobbler stderrGobbler = new StreamGobbler(dnsProcess.getErrorStream(), onLineListener);

			stdoutGobbler.start();
			stderrGobbler.start();

			dnsProcess.waitFor();		
		} catch (IOException e) {
			//SkStatus.logInfo("SlowDNS: " + e);
		}catch (InterruptedException e){
			//SkStatus.logInfo("SlowDNS: " + e);
		}

	}

	@Override
	public void interrupt(){
		if (dnsProcess != null)
			dnsProcess.destroy();
		try {
			if (filedns != null)
				VpnUtils.killProcess(filedns);
		} catch (Exception e) {}

		dnsProcess = null;
		filedns = null;
		super.interrupt();
	}

}
