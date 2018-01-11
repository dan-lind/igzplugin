## IG Zorro Plugin

This a plugin for [Zorro](http://www.takemoneyfromtherichandgiveittothepoor.com/) which lets you trade with [IG](http://www.ig.com). It implements the [Zorro-Broker-API](http://www.zorro-trader.com/manual/en/brokerplugin.htm).

It borrows heavily from the [Dukascopy plugin for Zorro](https://github.com/juxeii/dztools/), especially the c++ bridge.
Thanks to [juxeii](https://github.com/juxeii) for open sourcing his code

## General installation

1.) Download and install the latest **32-bit** [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). Make sure it is the 32-bit version(x86 suffix) since the plugin DLL is a 32-bit library. In case you already have a 32-bit JDK installation(check it with *java -version*) you might skip this step.

2.) Add *${yourJDKinstallPath}\jre\bin* and *${yourJDKinstallPath}\jre\bin\client* to the **front** of your *Path* environment variable([here](http://www.computerhope.com/issues/ch000549.htm) is a howto).

3.) Install [Zorro](http://www.takemoneyfromtherichandgiveittothepoor.com/download.php) if not already on your machine.

In most cases, following steps 1-3 should be enough as the IG plugin is now distributed with Zorro.
If there is a new version of the plugin that you want to run, that has not yet been distributed by Zorro, follow steps 4-5 below:

4.) Download the [igplugin.zip](https://github.com/dan-lind/igzplugin/releases) archive.

5.) Extract the archive into *${yourZorroInstallPath\Plugin}* folder.

## Configuration/Usage

### API Keys
After extracting the igplugin archive you should see a *ig.dll* and a folder *ig* in the *Plugin* directory of your Zorro installation.

If you have not done so already, you now need to generate an API key to be able to use the IG API through Zorro.
Refer to [IG Labs Getting Started guide](https://labs.ig.com/gettingstarted) for step by step instructions on how to do this.

Go to the *ig* folder and open the *application.properties* file with a text editor.

Here you should adapt the *plugin.realApiKey* and/or *plugin.demoApiKey* to your match the values of the keys that you generated in the previos step.
You can leave the other entries to their default values.

### Assets
Before you start Zorro you must also update the AssetsFix.csv with correct symbol names for the assets you want to trade.
Refer to [The asset list section](http://zorro-trader.com/manual/en/export.htm) of the Zorro manual for details on this.
The asset names the IG API expects is very different from the ones that Zorro uses by default. For instance the EUR/USD name is CS.D.EURUSD.CFD.IP
You can find the correct symbol names for you favorite assets by using the [IG API Companion](https://labs.ig.com/sample-apps/api-companion/index.html)
Log in with you user name, password and api key, then scroll down to *Market Search*, enter your search String, e.g. DAX. Now look in the response for the "epic" key, e.g  **"epic": "IX.D.DAX.IFD.IP",**


Start Zorro and look in the *Account* drop-down-box. You should see *IG* as an available broker.
Enter your login details, pick a script of your choice and press *Trade*. If everything is fine you should see that the login to IG has been successful.

### Logs
The plugin stores its logs to *ig/logs/igzplugin.log*(the default log level is *info*). If you encounter problems open *ig/igzplugin/logback.xml* for configuring the log level. Then change the log level for the file igzplugin-appender to *debug* and save the file. A new Zorro session will now produce a more verbose *igzplugin.log* file which you can use to report errors.

Please use [pastebin](http://pastebin.com/) for uploading the logs.

### Order reference text
Since version 0.3, the plugin supports the brokerCommand setOrderText. Call it like this in your script
```
brokerCommand(SET_ORDERTEXT,"MyOwnTest");
```
This enables you to set your own references on orders and makes it easier to separating PnL for different strategies in hindsight.
The text can be no more than 19 characters long, and must only contain A-Z, a-z, 0-9, _ and - 

## Remarks

- You WILL need to update the AssetsFix with Symbol names. For instance the EUR/USD name is CS.D.EURUSD.CFD.IP
- The default allowance for IG historic prices is 10 000 quotes per week. If your allowance goes to zero, you won't even be able to download quotes for the lookback period. This means it is not a good idea to use IG as a source for downloading long periods of historic data.
- Keep your historic data as up to date as possible and prefer using the [PRELOAD-flag](http://www.zorro-trader.com/manual/en/mode.htm) in you scripts. Even just filling the lookback will get rate limited if you have many assets and are too far behind on historic data.
- The default quotas for IG Streaming API connections is 40 concurrent connections. The plugin currently uses 2 streams per asset, which means so can at a maximum trade 20 assets at the same time. Contact IG if you need to raise your limit.

## Feedback
- Feel free to suggest improvements, post issues or suggest pull requests here on Github
- Follow discussions for this project on the [Zorro forum](http://www.opserver.de/ubb7/ubbthreads.php?ubb=showflat&Number=465410#Post465410)
- Find me on Twitter, @SweetSpotDan
