package com.appspace.main;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

// Background service
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
		
		// Spawn a superuser process to get root access
		SysFS.spawnSuperuserProcess();
		
		// Prepare and execute BackgroundTask
		bt = new BackgroundTask();
		bt.execute(this, null, null);
		
		// The service has to be persistent
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// Destroy BackgroundTask
		bt.cancel(true);
		
		// Kill superuser process
		SysFS.killSuperuserProcess();
		Log.i(tag, "service destroyed");
	}
}
