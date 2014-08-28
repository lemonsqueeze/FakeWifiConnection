package com.lemonsqueeze.fakewificonnection;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.*;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

//import android.content.Context;
//import android.content.SharedPreferences;
import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.net.DhcpInfo;
import android.util.Log;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import org.apache.http.conn.util.InetAddressUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;


public class Main implements IXposedHookLoadPackage 
{
  private XSharedPreferences pref;
  private LoadPackageParam lpparam;

  // debug info: 0=quiet, 1=log function calls, 2=also dump stack traces
  private final int debug_level = 1;

  public boolean hack_enabled()
  {
      boolean master_switch = pref.getBoolean("master", true);
      boolean app_enabled = pref.getBoolean(lpparam.packageName, false);
      return (master_switch && app_enabled);
  }

  public void dump_stack_trace()
  {
      Log.d("FakeWifiConnection", Log.getStackTraceString(new Exception()));
  }

  public void log(String s)
  {
      if (debug_level < 1)
	  return;
      
      //XposedBridge.log("FakeWifiConnection: " + s);
      Log.d("FakeWifiConnection", s);
  }
    
  public void log_call(String s)
  {
      if (debug_level < 1)
	  return;
      
      //XposedBridge.log("FakeWifiConnection: " + s);
      Log.d("FakeWifiConnection", s);

      if (debug_level > 1)
	  dump_stack_trace();
  }
    
  public void doit_networkinfo(String called, MethodHookParam param) throws Exception
  {	 
//      XposedBridge.log("FakeWifiConnection:" +
//		       " master=" + master_switch +
//		       " " + lpparam.packageName + "=" + hack_enabled );
     
      if (!hack_enabled())
      {
	  log_call(called + ", hack is disabled.");
	  return;
      }
     
      // if we're already on wifi don't interfere.
      if (param.getResult() != null)
      {
	  NetworkInfo network = (NetworkInfo) param.getResult();
	  if (network.getType() == ConnectivityManager.TYPE_WIFI &&
	      network.isConnected())
	  {
	      log_call(called + ", on wifi already.");
	      return;
	  }
      }
	
      log_call(called + ", faking wifi !");
      param.setResult(getFakeNetworkInfo());
 }
	
  public NetworkInfo	getFakeNetworkInfo() throws Exception
  {
      NetworkInfo info = createNetworkInfo(ConnectivityManager.TYPE_WIFI, true);
      return info;
  }

  public NetworkInfo createNetworkInfo(final int type, final boolean connected) throws Exception 
  {
      Constructor<NetworkInfo> ctor = NetworkInfo.class.getDeclaredConstructor(int.class);
      ctor.setAccessible(true);
      NetworkInfo networkInfo = ctor.newInstance(0);
      
      XposedHelpers.setIntField((Object)networkInfo, "mNetworkType", type);
      XposedHelpers.setObjectField((Object)networkInfo, "mTypeName", "WIFI");
      XposedHelpers.setObjectField((Object)networkInfo, "mState", NetworkInfo.State.CONNECTED);
      XposedHelpers.setObjectField((Object)networkInfo, "mDetailedState", NetworkInfo.DetailedState.CONNECTED);
      XposedHelpers.setBooleanField((Object)networkInfo, "mIsAvailable", true);
      return networkInfo;
  }

  public Object createWifiSsid() throws Exception 
  {
      // essentially does
      // WifiSsid ssid = WifiSsid.createFromAsciiEncoded("FakeWifi");
      
      Class cls = XposedHelpers.findClass("android.net.wifi.WifiSsid", lpparam.classLoader);
      Object wifissid = XposedHelpers.callStaticMethod(cls, "createFromAsciiEncoded", "FakeWifi");           
      return wifissid;
  }
    
  public WifiInfo createWifiInfo() throws Exception 
  {
      // WifiInfo info = new WifiInfo();      
      WifiInfo info = (WifiInfo) XposedHelpers.newInstance(WifiInfo.class);

// NEEDED ?
//    private boolean mHiddenSSID;
//    private int mRssi;	/** Received Signal Strength Indicator */
//    public static final String LINK_SPEED_UNITS = "Mbps";	/** Link speed in Mbps */
//    private int mLinkSpeed;

      IPInfo ip = getIPInfo();            
      XposedHelpers.setIntField((Object)info, "mNetworkId", 1);      
      XposedHelpers.setObjectField((Object)info, "mWifiSsid", createWifiSsid());
      XposedHelpers.setObjectField((Object)info, "mSupplicantState", SupplicantState.COMPLETED);
      XposedHelpers.setObjectField((Object)info, "mBSSID", "66:55:44:33:22:11");
      XposedHelpers.setObjectField((Object)info, "mMacAddress", "11:22:33:44:55:66");
      XposedHelpers.setObjectField((Object)info, "mIpAddress", ip.addr);
      
      return info;
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
      return result;
  }
    
      
  public DhcpInfo createDhcpInfo() throws Exception
  {      
      DhcpInfo i = new DhcpInfo();
      IPInfo ip = getIPInfo();
      i.ipAddress = ip.ip_hex;
      i.netmask = ip.netmask_hex;
      i.dns1 = 0x04040404;
      i.dns2 = 0x08080808;
      // gateway, leaseDuration, serverAddress

      String s = ("ip address: " + String.format("%x", i.ipAddress) +
		  " netmask: /" + i.netmask +
		  "dns1: " + String.format("%x", i.dns1) +
		  "dns2: " + String.format("%x", i.dns2));
      log(s);
      
      return i;
  }
    
  @Override
  public void handleLoadPackage(final LoadPackageParam lpp) throws Throwable
  {
      lpparam = lpp;
      XposedBridge.log("FakeWifiConnection: Loaded app: " + lpparam.packageName);
	
      pref = new XSharedPreferences(Main.class.getPackage().getName(), "pref");
      
      XposedHelpers.findAndHookMethod((Class)Activity.class, "onResume", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	  {  pref.reload();  }
      });	

      // *************************************************************************************
      // ConnectivityManager targets:
      //   getActiveNetworkInfo()
      //   getNetworkInfo()
      //   getAllNetworkInfo()		 
      
      // getActiveNetworkInfo()
      findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			"getActiveNetworkInfo", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	  {  doit_networkinfo("getActiveNetworkInfo()", param);   }
      });
      
      // getAllNetworkInfo()
      findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			"getAllNetworkInfo", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("getAllNetworkInfo() called.");
	      }
      });
	 
      // getNetworkInfo(int)
      findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			"getNetworkInfo", int.class, new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	  {	 
	      int network_type = (Integer) param.args[0];
	      String called = "getNetworkInfo(" + network_type + ")";
	      
	      if (network_type == ConnectivityManager.TYPE_WIFI)
		  doit_networkinfo(called, param);
	      else
		  log_call(called + " called.");
	  }
      });	 

      // *************************************************************************************      
      // WifiManager targets:
      //   isWifiEnabled()      
      //   getWifiState()
      //   getConnectionInfo()
      //   getDhcpInfo()

      // TODO do we need these:
      //   createWifiLock(string)
      //   createWifiLock(int, string)
      //   getConfiguredNetworks()
      //      for WifiConfiguration ...

      // isWifiEnabled()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"isWifiEnabled", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("isWifiEnabled() called" +
			   (hack_enabled() ? ", faking wifi !" : ""));
		  
		  if (hack_enabled())
		      param.setResult(true);
	      }
      });

      // getWifiState()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"getWifiState", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("getWifiState() called" +
			   (hack_enabled() ? ", faking wifi !" : ""));
		  
		  if (hack_enabled())		  
		      param.setResult(WifiManager.WIFI_STATE_ENABLED);
	      }
      });
      
      
      // getConnectionInfo()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"getConnectionInfo", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("getConnectionInfo() called" +
			   (hack_enabled() ? ", faking wifi !" : ""));
		  
		  if (hack_enabled())
		      param.setResult(createWifiInfo());
	      }
      });

      // getDhcpInfo()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"getDhcpInfo", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("getDhcpInfo() called" +
			   (hack_enabled() ? ", faking wifi !" : ""));
		  
		  if (hack_enabled())
		      param.setResult(createDhcpInfo());
	      }
      });
      
      // *************************************************************************************
      // debug only
      
      // createWifiLock(string)
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"createWifiLock", String.class, new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("createWifiLock(String) called");
	      }
      });

      // createWifiLock(int, string)
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"createWifiLock", int.class, String.class, new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("createWifiLock(int, String) called");
	      }
      });
      

      // getConfiguredNetworks()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"getConfiguredNetworks", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  log_call("getConfiguredNetworks() called");
	      }
      });

      
  }
    
}

