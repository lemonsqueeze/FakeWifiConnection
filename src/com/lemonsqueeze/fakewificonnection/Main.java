package com.lemonsqueeze.fakewificonnection;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

//import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class Main implements IXposedHookLoadPackage {

 public void doit(String called, MethodHookParam param) throws Exception
 {	 
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
 public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
	 // XposedBridge.log("FakeWifiConnection: Loaded app: " + lpparam.packageName);
	 
	// targets:
	// getActiveNetworkInfo()
	// getNetworkInfo()
	// getAllNetworkInfo()		 
	 
	 // getActiveNetworkInfo()
	 findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			 "getActiveNetworkInfo", new XC_MethodHook() 
	   {
	     @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
	    	 doit("getActiveNetworkInfo()", param);
         }
	   });

	 // getAllNetworkInfo()
	 findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			 "getAllNetworkInfo", new XC_MethodHook() 
	   {
	     @Override
         protected void afterHookedMethod(MethodHookParam param) throws Throwable 
         {
	    	 XposedBridge.log("FakeWifiConnection: getAllNetworkInfo() called.");
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
	    		 doit(called, param);
	    	 else
	    		 XposedBridge.log("FakeWifiConnection: " + called + " called.");
	     }
	   });
	 
 }
}







