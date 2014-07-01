package com.lemonsqueeze.fakewificonnection;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

//import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class Main implements IXposedHookLoadPackage {
	
 public NetworkInfo	getFakeNetworkInfo() throws Exception
 {
	NetworkInfo info = createNetworkInfo(ConnectivityManager.TYPE_WIFI, true);
	// XposedBridge.log("FakeWifiConnection: fakeNetworkInfo.isConnected(): " + info.isConnected());
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
	 
	//  Context context ...
	//  ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	//  NetworkInfo info = cm.getActiveNetworkInfo();
	//  "android.net.ConnectivityManager"
	 
	// targets:
	// getActiveNetworkInfo()
	// getNetworkInfo()
	// getActiveNetworkInfoForUid()
	// getAllNetworkInfo()
		 
	 
	 // getActiveNetworkInfo()
	 findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			 "getActiveNetworkInfo", new XC_MethodHook() 
	   {
	     @Override
         protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
         {
	    	 XposedBridge.log("FakeWifiConnection: getActiveNetworkInfo(), faking wifi !");
	    	 param.setResult(getFakeNetworkInfo());
         }
	   });

	 // getAllNetworkInfo()
	 findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			 "getAllNetworkInfo", new XC_MethodHook() 
	   {
	     @Override
         protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
         {
	    	 XposedBridge.log("FakeWifiConnection: getAllNetworkInfo() called.");
         }
	   });

	 
	 // getNetworkInfo(int)
	 findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader, 
			 "getNetworkInfo", int.class, new XC_MethodHook() 
	   {
	     @Override
	     protected void beforeHookedMethod(MethodHookParam param) throws Throwable 
	     {	 
	    	 int network_type = (Integer) param.args[0];
	    	 
	    	 if (network_type == ConnectivityManager.TYPE_WIFI)
	    	 {
	    		 XposedBridge.log("FakeWifiConnection: getNetworkInfo(" + network_type + "), faking wifi !");
	    		 param.setResult(getFakeNetworkInfo());
	    	 }
	    	 else
	    		 XposedBridge.log("FakeWifiConnection: getNetworkInfo(" + network_type + ") called.");
	     }
	   });
	 
		 
 }
}







