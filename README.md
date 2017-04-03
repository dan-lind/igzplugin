## igzplugin

This a plugin for [Zorro](http://www.takemoneyfromtherichandgiveittothepoor.com/) which lets you trade with [IG](http://www.ig.com). It implements the [Zorro-Broker-API](http://www.zorro-trader.com/manual/en/brokerplugin.htm).

It borrows heavily from the [Dukascopy plugin for Zorro](https://github.com/juxeii/dztools/), especially the c++ bridge.
Thanks to [juxeii](https://github.com/juxeii) for open sourcing his code

## General installation

1.) Download and install the latest **32-bit** [Java JRE](http://www.oracle.com/technetwork/java/javase/downloads). Make sure it is the 32-bit version(x86 suffix) since the plugin DLL is a 32-bit library. In case you already have a 32-bit JRE installation(check it with *java -version*) you might skip this step.

2.) Add *${yourJREinstallPath}\jre\bin\* and *${yourJREinstallPath}\jre\bin\com.iggroup.api.client* to the **front** of your *Path* environment variable([here](http://www.computerhope.com/issues/ch000549.htm) is a howto).

3.) Install [Zorro](http://www.takemoneyfromtherichandgiveittothepoor.com/download.php) if not already on your machine.

4.) Download the [dukascopy.zip](https://github.com/juxeii/dztools/releases) archive.

5.) Extract the archive into *${yourZorroInstallPath\Plugin}* folder.

## Configuration/Usage

After extracting the dztools archive you should see a *ig.dll* and a folder *ig* in the *Plugin* directory of your Zorro installation.

Go to the *ig* folder and open the *Plugin.properties* file with a text editor.

Here you should adapt the *.cache* path to your local JForex installation path. Be careful with this step, since it may happen that the [Dukascopy API](http://www.dukascopy.com/com.iggroup.api.client/javadoc/com/dukascopy/api/system/IClient.html#setCacheDirectory%28java.io.File%29) **will delete the .cache folder if it is corrupted**. Please make a copy of an instrument to a different location and set the path accordingly. If nothing gets deleted then you can use your complete *.cache* directory.
You can leave the other entries to their default values.

Start Zorro and check if the *Account* drop-down-box shows *IG* as an available broker.
Pick a script of your choice and press *Trade*. If everything is fine you should see that the login to IG has been successful.

The plugin stores its logs to *ig/logs/igzplugin.log*(the default log level is *info*). If you encounter problems open *ig/igzplugin/logback.xml* for configuring the log level. Then change the log level for the file igzplugin-appender to *debug* and save the file. A new Zorro session will now produce a more verbose *igzplugin.log* file which you can use to report errors.

Please use [pastebin](http://pastebin.com/) for uploading the logs.

## Remarks

- This a very early release so **don't expect it to be bug free!**
- Currently **only Forex** is supported(no Stocks, CFDs etc.)
- You WILL need to update the AssetFix with a Symbol name. For instance the EUR/USD name is CS.D.EURUSD.CFD.IP
- The default allowance for IG historic prices is 10 000 quotes per week. Keep this in mind. If your allowance goes to zero, you won't even be able to download quotes for the lookback period.
- Follow discussions for this project on the [forum](http://www.opserver.de/ubb7/ubbthreads.php?ubb=showflat&Number=447697&#Post447697)
