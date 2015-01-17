package com.lemonsqueeze.fakewificonnection;

import de.robv.android.xposed.*;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
//import android.content.SharedPreferences;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.ScanResult;
import android.net.DhcpInfo;
import android.util.Log;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import org.apache.http.conn.util.InetAddressUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.lemonsqueeze.fakewificonnection.HookManager;

public class Main implements IXposedHookLoadPackage
{
  private XSharedPreferences pref;
  private LoadPackageParam lpparam;
    
  private HookManager hook_manager = new HookManager();

  // debug level: 0=quiet, 1=log function calls, 2=also dump stack traces.
  // install 'Preferences Manager' to change default (0)
  private int debug_level()
  {
      if (pref == null)
	  return 1;
      return pref.getInt("debug_level", 0);
  }

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
      if (debug_level() < 1)
	  return;

      //XposedBridge.log("FakeWifiConnection: " + s);
      Log.d("FakeWifiConnection", lpparam.packageName + " " + s);
      //Log.d("FakeWifiConnection", s);
  }

  public void log_call(String s)
  {
      int debug = debug_level();
      if (debug < 1)
	  return;

      //XposedBridge.log("FakeWifiConnection: " + s);
      Log.d("FakeWifiConnection", lpparam.packageName + " " + s);
      //Log.d("FakeWifiConnection", s);
      
      if (debug > 1)
	  dump_stack_trace();
  }

  public void doit_networkinfo(String called, MethodHookParam param) throws Exception
  {
      if (!hack_enabled())
      {
	  log_call(called + ", hack disabled");
	  return;
      }

      // if we're already on wifi don't interfere.
      if (param.getResult() != null)
      {
	  NetworkInfo network = (NetworkInfo) param.getResult();
	  if (network.getType() == ConnectivityManager.TYPE_WIFI &&
	      network.isConnected())
	  {
	      log_call(called + ", on wifi already");
	      return;
	  }
      }
	
      log_call(called + ", faking wifi");
      param.setResult(getFakeNetworkInfo());
  }

  private static Object[] push(Object[] array, Object item)
  {
      Object[] longer = new Object[array.length + 1];
      System.arraycopy(array, 0, longer, 0, array.length);
      longer[array.length] = item;
      return longer;
  }

  public void doit_allnetworkinfo(String called, MethodHookParam param) throws Exception
  {
      if (!hack_enabled())
      {
	  log_call(called + ", hack disabled");
	  return;
      }

      NetworkInfo[] networks = (NetworkInfo[]) param.getResult();
      int i;			// wifi networkinfo index
      boolean wifi_found = false;
      for (i = 0; i < networks.length; i++)
	  if (networks[i].getType() == ConnectivityManager.TYPE_WIFI)
	  {  wifi_found = true;  break;  }

      // if we're already on wifi don't interfere.
      if (wifi_found && networks[i].isConnected())
      {
	  log_call(called + ", on wifi already.");
	  return;
      }

      log_call(called + ", faking wifi");
      if (wifi_found)
	  networks[i] = getFakeNetworkInfo();
      else
	  networks = (NetworkInfo[]) push(networks, getFakeNetworkInfo());
      param.setResult(networks);
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

      IPInfo ip = getIPInfo();
      InetAddress addr = (ip != null ? ip.addr : null);
      XposedHelpers.setIntField((Object)info, "mNetworkId", 1);
      XposedHelpers.setObjectField((Object)info, "mWifiSsid", createWifiSsid());
      XposedHelpers.setObjectField((Object)info, "mSupplicantState", SupplicantState.COMPLETED);
      XposedHelpers.setObjectField((Object)info, "mBSSID", "66:55:44:33:22:11");
      XposedHelpers.setObjectField((Object)info, "mMacAddress", "11:22:33:44:55:66");
      XposedHelpers.setObjectField((Object)info, "mIpAddress", addr);
      XposedHelpers.setIntField((Object)info, "mLinkSpeed", 65);  // Mbps
      XposedHelpers.setIntField((Object)info, "mRssi", -50);

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

  public ScanResult create_scan_result() throws Exception
  {
      Class wifissid_class = XposedHelpers.findClass("android.net.wifi.WifiSsid", lpparam.classLoader);      
      Constructor<ScanResult> ctor = ScanResult.class.getDeclaredConstructor(
	  wifissid_class, String.class, String.class, int.class, int.class, long.class);
      ctor.setAccessible(true);
      return ctor.newInstance(createWifiSsid(),		// SSID
			      "66:55:44:33:22:11",	// BSSID      
			      "[WPA-PSK-TKIP+CCMP][WPA2-PSK-TKIP-CCMP][ESS]", // capabilities
			      -50,			// level   (RSSI)
			      2462,			// frequency (MHz)   channel 11
			      SystemClock.elapsedRealtime() * 1000);  // timestamp
  }  


  public List<ScanResult> create_scan_results() throws Exception
  {
      List<ScanResult> l = new ArrayList<ScanResult>();
      l.add(create_scan_result());
      return l;
  }

  // Same as XposedHelper's findAndHookMethod() but shows error msg instead of throwing exception
  // (and returns void)
  private void hook_method(Class<?> clazz, String methodName, Object... parameterTypesAndCallback)
  {
      try
      {   XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);  }
      catch (NoSuchMethodError e)
      {   log("couldn't hook method " + methodName);   }
  }

  // idem
  private void hook_method(String className, ClassLoader classLoader, String methodName,
				  Object... parameterTypesAndCallback)
  {
      try
      {   XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);  }
      catch (NoSuchMethodError e)
      {   log("couldn't hook method " + methodName);   }
  }
  
  @Override
  public void handleLoadPackage(final LoadPackageParam lpp) throws Throwable
  {
      pref = new XSharedPreferences(Main.class.getPackage().getName(), "pref");      
      lpparam = lpp;
      log("Loaded app: " + lpparam.packageName);	
      
      hook_method((Class)Activity.class, "onResume", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {   pref.reload();  }
      });

      // *************************************************************************************
      // ConnectivityManager targets:
      //   getActiveNetworkInfo()
      //   getActiveNetworkInfoForUid( int )		UNDOCUMENTED
      //   getProvisioningOrActiveNetworkInfo()		UNDOCUMENTED
      //   getActiveNetworkInfoUnfiltered()		UNDOCUMENTED
      //   getNetworkInfo( int )
      //   getAllNetworkInfo()
      //   isActiveNetworkMetered()
      //   requestRouteToHost()
      //   getActiveLinkProperties()			UNDOCUMENTED
      //   getLinkProperties( int )
      
      // ConnectivityManager api, including undocumented stuff:
      // http://androidxref.com/4.4.2_r2/xref/frameworks/base/services/java/com/android/server/ConnectivityService.java
      
      // getActiveNetworkInfo()
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getActiveNetworkInfo", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {   doit_networkinfo("getActiveNetworkInfo()", param);   }
      });
      
      // getActiveNetworkInfoForUid(int)		UNDOCUMENTED
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getActiveNetworkInfoForUid", int.class, new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      int uid = (Integer) param.args[0];
	      String called = "getActiveNetworkInfoForUid(" + uid + ")";
	      doit_networkinfo(called, param);
	  }
      });

      // getProvisioningOrActiveNetworkInfo()		UNDOCUMENTED
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getProvisioningOrActiveNetworkInfo", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {  doit_networkinfo("getProvisioningOrActiveNetworkInfo()", param);   }
      });

      /*  NOT FOUND ...
      // getActiveNetworkInfoUnfiltered()		UNDOCUMENTED
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getActiveNetworkInfoUnfiltered", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {  doit_networkinfo("getActiveNetworkInfoUnfiltered()", param);   }
      });
      */
      
      // getAllNetworkInfo()
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getAllNetworkInfo", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {   doit_allnetworkinfo("getAllNetworkInfo()", param);   }
      });
	
      // getNetworkInfo(int)
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
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
		  log_call(called + " called");
	  }
      });

      // isActiveNetworkMetered()
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "isActiveNetworkMetered", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      if (!hack_enabled())
	      {	  log_call("isActiveNetworkMetered(), hack is disabled.");  return;  }
	      
	      log_call("isActiveNetworkMetered(), faking wifi !");
	      param.setResult(false);
	  }
      });


      // requestRouteToHost(int, int)		LOG ONLY
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "requestRouteToHost", int.class, int.class, new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      int network_type = (Integer) param.args[0];
	      int host_addr = (Integer) param.args[1];
	      String called = "requestRouteToHost(" + network_type + ", " + host_addr + ")";
	      
	      log_call(called + " called.");
	  }
      });

      // getActiveLinkProperties()		LOG ONLY
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getActiveLinkProperties", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      log_call("getActiveLinkProperties() called.");
	  }
      });

      // getLinkProperties(int)			LOG ONLY
      hook_method("android.net.ConnectivityManager", lpparam.classLoader,
		  "getLinkProperties", int.class, new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      int network_type = (Integer) param.args[0];
	      log_call("getLinkProperties(" + network_type + ") called.");
	  }
      });

      
      
      // *************************************************************************************
      // WifiManager targets:
      //   isWifiEnabled()
      //   getWifiState()
      //   getConnectionInfo()
      //   getDhcpInfo()
      //   startscan()
      //   getScanResults()

      // debug only:
      //   createWifiLock(string)
      //   createWifiLock(int, string)
      //   getConfiguredNetworks()
      //      for WifiConfiguration ...

      // isWifiEnabled()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "isWifiEnabled", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      log_call("isWifiEnabled(), " + (hack_enabled() ? "faking wifi" : "called"));
	      if (hack_enabled())
		  param.setResult(true);
	  }
      });

      // getWifiState()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "getWifiState", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      log_call("getWifiState(), " + (hack_enabled() ? "faking wifi" : "called"));
	      if (hack_enabled())
		  param.setResult(WifiManager.WIFI_STATE_ENABLED);
	  }
      });


      // getConnectionInfo()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "getConnectionInfo", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      log_call("getConnectionInfo(), " + (hack_enabled() ? "faking wifi" : "called"));
	      if (hack_enabled())
		  param.setResult(createWifiInfo());
	  }
      });

      // getDhcpInfo()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "getDhcpInfo", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {
	      boolean doit = hack_enabled() && getIPInfo() != null;
	      log_call("getDhcpInfo(), " + (doit ? "faking wifi" : "called"));
	      if (doit)
		  param.setResult(createDhcpInfo());
	  }
      });

      // startScan()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "startScan", new XC_MethodHook()
      {
	  @Override
	  protected void beforeHookedMethod(MethodHookParam param) throws Throwable
	  {
	      boolean doit = hack_enabled();
	      log_call("startScan(), " + (doit ? "faking wifi" : "called"));
	      if (doit)
	      {
		  param.setResult(true);	// cancel orig call
		  hook_manager.call(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	      }
	  }
      });

      // getScanResults()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "getScanResults", new XC_MethodHook()
      {
	  @Override
	  protected void beforeHookedMethod(MethodHookParam param) throws Throwable
	  {
	      boolean doit = hack_enabled();
	      log_call("getScanResults(), " + (doit ? "faking wifi" : "called"));
	      if (doit)
		  param.setResult(create_scan_results());	// cancel orig call
	  }
      });
      
      
      // *************************************************************************************
      // debug only

      // createWifiLock(string)
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "createWifiLock", String.class, new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {   log_call("createWifiLock(String) called");   }
      });

      // createWifiLock(int, string)
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "createWifiLock", int.class, String.class, new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {   log_call("createWifiLock(int, String) called");    }
      });

      
      // getConfiguredNetworks()
      hook_method("android.net.wifi.WifiManager", lpparam.classLoader,
		  "getConfiguredNetworks", new XC_MethodHook()
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable
	  {   log_call("getConfiguredNetworks() called");     }
      });


      // *************************************************************************************
      // Misc stuff
      
      // intent receivers which need overriding

      // registerReceiver(BroadcastReceiver, IntentFilter)
      //   Context.registerReceiver() is abstract, so hook ContextWrapper
      //   which Activity is derived of (and Service also).
      // FIXME
      //   - we let everything through
      //   - there's this one also:
      //       abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
      //                                        String broadcastPermission, Handler scheduler)      
      hook_method("android.content.ContextWrapper", lpparam.classLoader,
		  "registerReceiver", BroadcastReceiver.class, IntentFilter.class, new XC_MethodHook()
      {
	  @Override
	  protected void beforeHookedMethod(MethodHookParam param) throws Throwable
	  {
	      BroadcastReceiver receiver = (BroadcastReceiver) param.args[0];
	      IntentFilter filter = (IntentFilter) param.args[1];
	      int nactions = filter.countActions();
	      if (filter.hasAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
	      {
		  log_call("registerReceiver(SCAN_RESULTS) called");
		  // Always let it through.
		  // Otherwise have to keep track of them so unregister doesn't throw exception.
		  hook_manager.add(filter, receiver, (Context) param.thisObject);
	      }
	  }
      });

      // unregisterReceiver(BroadcastReceiver)
      hook_method("android.content.ContextWrapper", lpparam.classLoader,
		  "unregisterReceiver", BroadcastReceiver.class, new XC_MethodHook()
      {
	  @Override
	  protected void beforeHookedMethod(MethodHookParam param) throws Throwable
	  {
	      BroadcastReceiver receiver = (BroadcastReceiver) param.args[0];
	      hook_manager.remove(receiver);
	  }
      });      
      
  }

}

