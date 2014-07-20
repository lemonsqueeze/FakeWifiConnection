package com.lemonsqueeze.fakewificonnection;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.*;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;


public class Main implements IXposedHookLoadPackage 
{
  private XSharedPreferences pref;
  private LoadPackageParam lpparam;

  public boolean app_enabled()
  {
      boolean master_switch = pref.getBoolean("master", true);
      boolean app_enabled = pref.getBoolean(lpparam.packageName, true);
      return (master_switch && app_enabled);
  }
    
  public void doit_networkinfo(String called, MethodHookParam param) throws Exception
  {	 
//      XposedBridge.log("FakeWifiConnection:" +
//		       " master=" + master_switch +
//		       " " + lpparam.packageName + "=" + app_enabled );
     
      if (!app_enabled())
      {
	  XposedBridge.log("FakeWifiConnection: " + called + ", hack is disabled.");
	  return;
      }
     
      // if we're already on wifi don't interfere.
      if (param.getResult() != null)
      {
	  NetworkInfo network = (NetworkInfo) param.getResult();
	  if (network.getType() == ConnectivityManager.TYPE_WIFI &&
	      network.isConnected())
	  {
	      XposedBridge.log("FakeWifiConnection: " + called + ", on wifi already.");
	      return;
	  }
      }
	
      XposedBridge.log("FakeWifiConnection: " + called + ", faking wifi !");
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

// DONE      
//    private int mNetworkId;
//    private WifiSsid mWifiSsid;
//    private SupplicantState mSupplicantState;

// TODO ?
//    private String mBSSID;
//    private boolean mHiddenSSID;
//    /** Received Signal Strength Indicator */
//    private int mRssi;
//    /** Link speed in Mbps */
//    public static final String LINK_SPEED_UNITS = "Mbps";
//    private int mLinkSpeed;
//    private InetAddress mIpAddress;
//    private String mMacAddress;
      
      XposedHelpers.setIntField((Object)info, "mNetworkId", 1);      
      XposedHelpers.setObjectField((Object)info, "mWifiSsid", createWifiSsid());
      XposedHelpers.setObjectField((Object)info, "mSupplicantState", SupplicantState.ASSOCIATED);
      
      return info;
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
	      {	 XposedBridge.log("FakeWifiConnection: getAllNetworkInfo() called.");   }
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
		  XposedBridge.log("FakeWifiConnection: " + called + " called.");
	  }
      });	 

      // *************************************************************************************      
      // WifiManager targets:
      //   isWifiEnabled()      
      //   getWifiState()
      //   getConnectionInfo()

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
		  XposedBridge.log("FakeWifiConnection: isWifiEnabled() called" +
				   (app_enabled() ? ", faking wifi !" : ""));
		  if (app_enabled())
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
		  XposedBridge.log("FakeWifiConnection: getWifiState() called" +
				   (app_enabled() ? ", faking wifi !" : ""));
		  if (app_enabled())		  
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
		  XposedBridge.log("FakeWifiConnection: getConnectionInfo() called" +
				   (app_enabled() ? ", faking wifi !" : ""));		  
		  if (app_enabled())
		      param.setResult(createWifiInfo());
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
		  XposedBridge.log("FakeWifiConnection: createWifiLock(String) called");
	      }
      });

      // createWifiLock(int, string)
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"createWifiLock", int.class, String.class, new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  XposedBridge.log("FakeWifiConnection: createWifiLock(int, String) called");
	      }
      });
      

      // getConfiguredNetworks()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"getConfiguredNetworks", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  XposedBridge.log("FakeWifiConnection: getConfiguredNetworks() called");
	      }
      });

      
  }
    
}

