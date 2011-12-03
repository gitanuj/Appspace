package com.appspace.main;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;

// Background service
public class DetectAppLaunchService extends Service {

	private int cycle = 720;
	private int probe_time = 5000; // In mSec
	private int counter=0;
	private int cycle_id=0;
	private int mEXTRA_LEVEL;
	private int mEXTRA_VOLTAGE;
	private String PERFORMANCE = "performance";
	private String POWERSAVE = "powersave";
	private String ONDEMAND = "ondemand";
	private String USERSPACE = "userspace";
	private String LOG_FILE = "log.txt";
	private BufferedWriter bw;
	private String eol = System.getProperty("line.separator");
	
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){

		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				mEXTRA_LEVEL = extras.getInt(BatteryManager.EXTRA_LEVEL);
				mEXTRA_VOLTAGE = extras.getInt(BatteryManager.EXTRA_VOLTAGE);
			}
		}
	};
	
	
    public static BackgroundTask bt;
    public static ArrayList<String> freq;
    public static int MIN, MAX;
    
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		// Fetch one time CPU info before doing anything
		freq = new ArrayList<String>();
      
		int minFrequency = Integer.parseInt(SysFS.getSCALING_MIN_FREQ());
		int maxFrequency = Integer.parseInt(SysFS.getSCALING_MAX_FREQ());
		String all = SysFS.getSCALING_AVAILABLE_FREQUENCIES();
		int temp;
		String temp1;
		StringTokenizer st = new StringTokenizer(all, " ");
		while(st.hasMoreTokens()) {
			temp1 = st.nextToken();
			temp = Integer.parseInt(temp1);
			if((temp >= minFrequency) && (temp <= maxFrequency)) {
				freq.add(temp1);
			}
		}
		MIN = 0;
		MAX = freq.size()-1;
		
		// Spawn a superuser process to get root access
		SysFS.spawnSuperuserProcess();
		
		// Register Broadcast Receiver
		this.registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		// Initialize Buffered Writer
		try {
			bw = new BufferedWriter(new OutputStreamWriter(openFileOutput(LOG_FILE, MODE_WORLD_READABLE)));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		// Prepare and execute BackgroundTask
		bt = new BackgroundTask();
		bt.execute(getApplicationContext(), null, null);
		
		// Here is the testing suite
		new Thread() {
			public void run() {
				// Starting notification
				NotificationManager nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
				Notification n = new Notification(R.drawable.ic_launcher, "Appspace testing started, don't hesitate to use your mobile in the meantime :)", System.currentTimeMillis());
				n.setLatestEventInfo(getApplicationContext(), "Appspace", "Testing started", PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), AppspaceActivity.class), 0));
				nm.notify(1, n);
				
				while(cycle_id < 4) {
					counter = 0;
					try {
						if(cycle_id == 0) {
							SysFS.setSCALING_GOVERNOR(PERFORMANCE);
							bw.write(PERFORMANCE + eol);
							
							while(counter < cycle) {
								bw.append(counter + " " + SysFS.getSCALING_CUR_FREQ() + " " + mEXTRA_LEVEL + " " + mEXTRA_VOLTAGE + " " + AppspaceReceiver.category + " " + AppspaceReceiver.screen_state + " " + eol);
								counter++;
								try {
									Thread.sleep(probe_time);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						else if(cycle_id == 1) {
							SysFS.setSCALING_GOVERNOR(POWERSAVE);
							bw.append(POWERSAVE + eol);
							
							while(counter < cycle) {
								bw.append(counter + " " + SysFS.getSCALING_CUR_FREQ() + " " + mEXTRA_LEVEL + " " + mEXTRA_VOLTAGE + " " + AppspaceReceiver.category + " " + AppspaceReceiver.screen_state + " " + eol);
								counter++;
								try {
									Thread.sleep(probe_time);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						else if(cycle_id == 2) {
							SysFS.setSCALING_GOVERNOR(ONDEMAND);
							bw.append(ONDEMAND + eol);
							
							while(counter < cycle) {
								bw.append(counter + " " + SysFS.getSCALING_CUR_FREQ() + " " + mEXTRA_LEVEL + " " + mEXTRA_VOLTAGE + " " + AppspaceReceiver.category + " " + AppspaceReceiver.screen_state + " " + eol);
								counter++;
								try {
									Thread.sleep(probe_time);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						else if(cycle_id == 3) {
							SysFS.setSCALING_GOVERNOR(USERSPACE);
							bw.append(USERSPACE + eol);
							
							while(counter < cycle) {
								bw.append(counter + " " + SysFS.getSCALING_CUR_FREQ() + " " + mEXTRA_LEVEL + " " + mEXTRA_VOLTAGE + " " + AppspaceReceiver.category + " " + AppspaceReceiver.screen_state + " " + eol);
								counter++;
								try {
									Thread.sleep(probe_time);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						cycle_id++;
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// Display finished notification
				n = new Notification(R.drawable.ic_launcher, "Appspace testing complete", System.currentTimeMillis());
				n.setLatestEventInfo(getApplicationContext(), "Appspace", "Testing completed", PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), AppspaceActivity.class), 0));
				nm.notify(1, n);
				
				stopService(new Intent(getApplicationContext(), DetectAppLaunchService.class));
			}
		}.start();
		
		// The service has to be persistent
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// Stop any thread in execution
		cycle_id = counter = 800;
		
		// Unregister Broadcast Receiver
		unregisterReceiver(mBatInfoReceiver);
		
		// Close the BufferedWriter
		try {
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Destroy BackgroundTask
		bt.cancel(true);
		
		// Kill superuser process
		SysFS.killSuperuserProcess();
	}
}
