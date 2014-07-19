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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;


public class Main implements IXposedHookLoadPackage 
{
  XSharedPreferences pref;

  public void doit(LoadPackageParam lpparam, String called, MethodHookParam param) throws Exception
  {	 
      boolean master_switch = pref.getBoolean("master", true);
      boolean app_enabled = pref.getBoolean(lpparam.packageName, true);
     
      XposedBridge.log("FakeWifiConnection:" +
		       " master=" + master_switch +
		       " " + lpparam.packageName + "=" + app_enabled );
     
      if (!(master_switch && app_enabled))
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
      Field typeField = NetworkInfo.class.getDeclaredField("mNetworkType");
      Field connectedField = NetworkInfo.class.getDeclaredField("mState");
      typeField.setAccessible(true);
      connectedField.setAccessible(true);
      typeField.setInt(networkInfo, type);
      connectedField.set(networkInfo, connected == true ? NetworkInfo.State.CONNECTED : NetworkInfo.State.DISCONNECTED);
      return networkInfo;
  }
 
  @Override
  public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable
  {
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
	  {  doit(lpparam, "getActiveNetworkInfo()", param);   }
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
		  doit(lpparam, called, param);
	      else
		  XposedBridge.log("FakeWifiConnection: " + called + " called.");
	  }
      });	 

      // *************************************************************************************      
      // WifiManager targets:
      //   isWifiEnabled()      
      //   getWifiState()
      //   getConnectionInfo()

      // isWifiEnabled()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"isWifiEnabled", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {
		  XposedBridge.log("FakeWifiConnection: isWifiEnabled() called, faking wifi !");
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
		  XposedBridge.log("FakeWifiConnection: getWifiState() called, faking wifi !");
		  param.setResult(WIFI_STATE_ENABLED);
	      }
      });
      
      
      // getConnectionInfo()
      findAndHookMethod("android.net.wifi.WifiManager", lpparam.classLoader, 
			"getConnectionInfo", new XC_MethodHook() 
      {
	  @Override
	  protected void afterHookedMethod(MethodHookParam param) throws Throwable 
	      {	 XposedBridge.log("FakeWifiConnection: getConnectionInfo() called.");   }
      });


  }
    
}

