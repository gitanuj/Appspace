package com.appspace.main;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DetectAppLaunchService extends Service {

	private static final String tag = "Service";
    public static BackgroundTask bt;
    
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(tag, "service started");
		SysFS.spawnSuperuserProcess();
		bt = new BackgroundTask();
		bt.execute(this, null, null);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		bt.cancel(true);
		SysFS.killSuperuserProcess();
		Log.i(tag, "service destroyed");
	}
}
