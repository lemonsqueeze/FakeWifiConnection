FakeWifiConnection
==================

Make android apps believe Wifi is connected.

Handy in situations where there is a (non wifi) connection, but some app won't do its thing unless it's on wifi
( android running in virtual machine / emulator, 3g but no wifi around, usb 3g modem connection, usb reverse tethering ...)

Tested on Android KitKat 4.4.2, play store working over ppp !

No app is faked by default. Open FakeWifiConnection app to enable/disable hack (master switch) and select which apps to fake. Changes take effect immediately (background apps need a reboot).

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

- Open FakeWifiConnection app to change settings.

Debugging
---------

`logcat | grep FakeWifiConnection`

Also check Xposed logs:

`logcat | grep Xposed`

Credits
-------

- rovo89 for awesome Xposed Framework
- UI code by hamzahrmalik (Force Fast Scroll)

Released under [GNU GPL License](https://raw.github.com/lemonsqueeze/FakeWifiConnection/master/LICENSE).
