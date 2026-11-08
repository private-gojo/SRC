package com.ace.injector.util;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import android.content.Context;
import android.support.v7.app.AppCompatDelegate;
import com.ace.injectorpro.R;
import com.ultrasshservice.config.Settings;



public class MyIP_Address extends AppCompatActivity
{


	public String data, ip, city, state, country, isp;
    JSONObject jsonObject;
    String s;
    TextView TxCity, TxIp, TxState, TxCountry, TxIsp;
	private int night_mode;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ip);

		TxCity = (TextView)findViewById(R.id.Txcity);
		//Status = (TextView)findViewById(R.id.status);
        TxIp = (TextView)findViewById(R.id.TextView4);
		TxState = (TextView)findViewById(R.id.Txstate);
        TxCountry = (TextView)findViewById(R.id.Txcountry);
        TxIsp = (TextView)findViewById(R.id.Txisp);

        GetIp2 myAs=new GetIp2();
        myAs.execute();
		setModoNoturno(this);
		//Date
		/*Date = (TextView) findViewById(R.id.txtDate);
		 Calendar c = Calendar.getInstance();
		 SimpleDateFormat df = new SimpleDateFormat("E, MMM dd, yyyy", Locale.getDefault());    
		 String date = df.format(c.getTime());
		 Date.setText(date);*/
	}
	public void setModoNoturno(Context text) {
        if (new Settings(this).getModoNoturno().equals("on")) {
            night_mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            night_mode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(night_mode);
    }

	class GetIp2 extends AsyncTask<String, Void, String>
    {
        //String s = null;

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            TxIp.setText(ip);
            TxCity.setText(city);
            TxState.setText(state);
            TxCountry.setText(country);
            TxIsp.setText(isp);
			// textView.setText(ip+city+state+country);
			//  mapView.animate();
        }

        @Override
        protected String doInBackground(String... params)
		{

            try
            {
                URL whatismyip = new URL("https://checkip.amazonaws.com");
                BufferedReader input = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
                ip = input.readLine();

                HttpURLConnection connection = (HttpURLConnection) new URL("http://ip-api.com/json/" + ip).openConnection();
                connection.setReadTimeout(3000);
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK)
				{
                    throw new IOException("HTTP error code: " + responseCode);
                }
                InputStream in = new java.io.BufferedInputStream(connection.getInputStream());
                //s=readStream(in,10000);
                s = convertStreamToString(in);
                jsonObject = new JSONObject(s);

                city = jsonObject.getString("city");
                isp = jsonObject.getString("org");
                state = jsonObject.getString("regionName");
                country = jsonObject.getString("country");

                Log.d("MainActivity", "Call reached here");

                if (in != null)
				{
                    // Converts Stream to String with max length of 500.
                    Log.d("MainActivity call 2", s);
                }
            }
			catch (Exception e)
			{
                e.printStackTrace();
            }

            return s;
        }

        public String convertStreamToString(InputStream is)
		{
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            try
			{
                while ((line = reader.readLine()) != null)
				{
                    sb.append(line).append('\n');
                }
            }
			catch (IOException e)
			{
                e.printStackTrace();
            }
			finally
			{
                try
				{
                    is.close();
                }
				catch (IOException e)
				{
                    e.printStackTrace();
                }
            }

            return sb.toString();
        }

    }

}
