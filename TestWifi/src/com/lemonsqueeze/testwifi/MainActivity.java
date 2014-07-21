package com.lemonsqueeze.testwifi;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;


public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
		
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo net = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);		
		String s = "Network Info (WIFI):";
		s += "\nisConnected: " + net.isConnected();
		s += "\ntype: " + net.getTypeName();
		
		net = cm.getActiveNetworkInfo();
		s += "\n\nActive Network Info:";
		if (net == null)
			s += "\nnull";
		else
			s += "\ntype: " + net.getTypeName();

		s += "\n\nWifiManager:";
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		s += "\nisWifiEnabled(): " + wm.isWifiEnabled();
		s += "\ngetWifiState(): " + wm.getWifiState() +
			 "   (DISABLED=1  ENABLED=3)"; 
		s += "\ngetConnectionInfo(): " + wm.getConnectionInfo();
		
		try 
		{
			WifiLock lock = wm.createWifiLock("tag");
			if (!lock.isHeld())
				lock.acquire();
			s += "\n\nWifi lock: " + (lock.isHeld() ? "OK" : "BAD");
			lock.release();
		} catch(Exception e) {
			s += "\n\nWifi lock: FAILED";
		}
		
		TextView text = new TextView(this);
		text.setText(s);
        setContentView(text);
    }
}
