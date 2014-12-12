package com.lemonsqueeze.testwifi;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.DhcpInfo;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;

import java.net.NetworkInterface;
import java.net.InetAddress;
import org.apache.http.conn.util.InetAddressUtils;
import java.util.*;
import android.net.*;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
		
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo net = cm.getActiveNetworkInfo();
		String s = "[ Active Network Info ]";
		if (net == null)
			s += "\nnull";
		else
			s += "\ntype: " + net.getTypeName();
		s += "\nmetered: " + (cm.isActiveNetworkMetered() ? "yes" : "no");		

		net = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);		
		s += "\n\n[ Network Info (WIFI) ]";
		s += "\nisConnected: " + net.isConnected();
		s += "\nisAvailable: " + net.isAvailable();
		s += "\ntype: " + net.getTypeName();
		
		
		s += "\n\n[ WifiManager ]";
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		s += "\nisWifiEnabled(): " + wm.isWifiEnabled();
		s += "\ngetWifiState(): " + wm.getWifiState() +
			 "   (DISABLED=1  ENABLED=3)"; 
		s += "\ngetConnectionInfo(): " + wm.getConnectionInfo();
        s += "\ngetDhcpInfo(): " + wm.getDhcpInfo();
		
		try 
		{
			WifiLock lock = wm.createWifiLock("tag");
			if (!lock.isHeld())
				lock.acquire();
			s += "\nWifi lock: " + (lock.isHeld() ? "OK" : "BAD");
			lock.release();
		} catch(Exception e) {
			s += "\n\nWifi lock: FAILED";
		}

        s += "\n\n[ IP functions test ]\n";		
		IPInfo info = getIPInfo();
	    s += "ip address: " + info.ip + 
		     String.format("  ( 0x%08x )", info.ip_hex);
	    
	    //byte b[] = info.addr.getAddress();
		//s += ",  [" + String.format("%02x %02x %02x %02x] ) ", b[0], b[1], b[2], b[3]); 
		
		s += "\n netmask: " + String.format("0x%08x", info.netmask_hex);
		
		TextView text = new TextView(this);
		text.setText(s);
        setContentView(text);
    }
	

	public static class IPInfo
	{
		NetworkInterface intf;
		InetAddress addr;
		String ip;
		int ip_hex;
		int netmask_hex;
	}

	// get current ip and netmask
	public static IPInfo getIPInfo()
	{
		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces)
			{
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs)
				{
					if (!addr.isLoopbackAddress())
					{
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (isIPv4)
						{
							IPInfo info = new IPInfo();
							info.addr = addr;
							info.intf = intf;
							info.ip = sAddr;
							info.ip_hex = InetAddress_to_hex(addr);
							info.netmask_hex = netmask_to_hex(intf.getInterfaceAddresses().get(0).getNetworkPrefixLength());
							return info;
						}
					}
				}
			}
		} catch (Exception ex) { } // for now eat exceptions
		return null;
	}

	public static int netmask_to_hex(int netmask_slash)
	{
		int r = 0;
		int b = 1;
		for (int i = 0; i < netmask_slash;  i++, b = b << 1)
			r |= b;
		return r;
	}      

	// for DhcpInfo 
	private static int InetAddress_to_hex(InetAddress a)
	{
		int result = 0;
		byte b[] = a.getAddress();		
		for (int i = 0; i < 4; i++)
			result |= (b[i] & 0xff) << (8 * i);			  
//	  result |= (b[i] & 0xff) << (24 - (8 * i));		
//	  result += b[i] << (24 - (8 * i));		
		return result;
	}
	
	// bad
	/*
	private static int InetAddress_to_hex(InetAddress a)
	{
		int result = 0;
		byte b[] = a.getAddress();		
		for (int i = 0; i < 4; i++) 
    		result += b[i] << (24 - (8 * i));		
		return result;
	}
	*/
	
}


	
