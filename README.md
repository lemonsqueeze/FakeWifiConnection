FakeWifiConnection
==================

Make android apps believe Wifi is connected.

Handy in situations where there is a (non wifi) connection, but some app won't do its thing unless it's on wifi
( android running in virtual machine / emulator, 3g but no wifi around, usb 3g modem connection, usb reverse tethering ...)

With this you can for instance access wifi-only features while on 3g.  
If you're used to iOS, it's the android equivalent of [3g Unrestrictor](http://www.3gunrestrictor.com/) or [My3G](http://www.intelliborn.com/my3g.html).

Tested on Android KitKat 4.4.2, play store working over ppp !

No app is faked by default. Open FakeWifiConnection app to enable/disable hack (master switch) and select which apps to fake. Changes take effect immediately (background apps need a reboot).

See also [support thread](http://forum.xda-developers.com/xposed/modules/app-fake-wifi-connection-t2800416) on xda-developers and [Xposed module page](http://repo.xposed.info/module/com.lemonsqueeze.fakewificonnection).

Install
-------

- Install Xposed Framework.  
  Get installer from http://repo.xposed.info/  
  Open Xposed Installer->Framework->Install  
  Reboot

- Install [FakeWifiConnection.apk](https://github.com/lemonsqueeze/FakeWifiConnection/raw/master/FakeWifiConnection/bin/FakeWifiConnection.apk)  
  Open Xposed Installer->Modules, tick FakeWifiConnection  
  Reboot

- Open FakeWifiConnection app to change settings.

Debugging
---------

Debug messages are disabled by default.  
To enable set `debug_level` to 1 with [Preferences Manager](https://play.google.com/store/apps/details?id=fr.simon.marquis.preferencesmanager) and reboot.

`logcat | grep FakeWifiConnection`

Also check Xposed logs:

`logcat | grep Xposed`

Credits
-------

- rovo89 for awesome Xposed Framework
- UI code by hamzahrmalik (Force Fast Scroll)

Released under [GNU GPL License](https://raw.github.com/lemonsqueeze/FakeWifiConnection/master/LICENSE).
