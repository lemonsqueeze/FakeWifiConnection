FakeWifiConnection
==================

Make android apps believe Wifi is connected.

Handy when using unofficial connection (usb 3g modem / ppp widget, usb reverse tethering ...) or on 3g connection and some app won't work unless it's on wifi.

Tested on Android KitKat (4.4 RC2), play store working over ppp !

You might want to try [HackConnectivity.apk](http://www.digitalmobile.in/community/threads/fake-wifi-to-play-games-with-wifi-requirement.8461/) as well. It didn't work for me, maybe it works with older android versions.

Install
-------

- Install Xposed Framework.  
  Get installer from http://repo.xposed.info/  
  Open Xposed Installer->Framework->Install  
  Reboot

- Install [FakeWifiConnection.apk](https://raw.github.com/lemonsqueeze/FakeWifiConnection/master/bin/FakeWifiConnection.apk)  
  Open Xposed Installer->Modules, tick FakeWifiConnection  
  Reboot

No easy way to enable/disable hack at this stage, just de/activate module in Xposed for now.

Debugging
---------

For now only getActiveNetworkInfo() is faked. If app is calling other stuff like getNetworkInfo() or getAllNetworkInfo() you can find out with:

`logcat | grep FakeWifiConnection`

Also check Xposed logs:

`logcat | grep Xposed`
