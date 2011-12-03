package com.appspace.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// BroadcastReceiver to listen for updates
public class AppspaceReceiver extends BroadcastReceiver {

	public static final String APP_LAUNCH_DETECTED = "com.appspace.main.NEW_APP_LAUNCHED";
	public static final String SCREEN_OFF = "com.appspace.main.SCREEN_OFF";
	public static final String SCREEN_ON = "com.appspace.main.SCREEN_ON";
	private static final int CATEGORY_HIGH_DEMANDING = 1;
	private static final int CATEGORY_MODERATE_DEMANDING = 2;
	private static final int CATEGORY_LOW_DEMANDING = 3;
	private static final int CATEGORY_UNKNOWN = 4;
	private static final float THRESHOLD_CPU_USAGE = 50f;
	private static final int MAX_COUNT = 5;
	private static final int CPU_PROBE_TIME = 1500;
	public static int category = -1;
	public static int screen_state = -1;

	private int category_thread;
	private boolean loop;
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		String action = arg1.getAction();
		loop = false;
        
		// New app launch detected
		if(action.equals(APP_LAUNCH_DETECTED)) {
			screen_state = 1;
			String pname = arg1.getExtras().getString("package");
			AppspaceDbAdapter adapter = new AppspaceDbAdapter(arg0);
			adapter.open();
			category = adapter.fetchPackageCategory(pname);
			adapter.close();
			
			if(category == CATEGORY_HIGH_DEMANDING || category == CATEGORY_MODERATE_DEMANDING || category == CATEGORY_LOW_DEMANDING) {
				setFrequencyRange(category);
			}
			else {
				// Either category not defined or there is no entry for the package
				setFrequencyRange(CATEGORY_UNKNOWN);
			}
		}
		
		// User has unlocked the screen lock or User present
		else if(action.equals(Intent.ACTION_USER_PRESENT)) {
			screen_state = 1;
			if(AppspaceActivity.isMyServiceRunning(arg0)) {
				setFrequencyRange(CATEGORY_UNKNOWN);
			}
		}
		
		// User has turned the screen OFF
		else if(action.equals(SCREEN_OFF)) {
			screen_state = 0;
			// Set frequency to minimum
			SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(DetectAppLaunchService.MIN));
		}
		
		// User has turned the screen ON
		else if(action.equals(SCREEN_ON)) {
			screen_state = 1;
			// Set frequency to minimum+1
			SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(DetectAppLaunchService.MIN+1));
		}
	}
	
	// Method to set the frequency according to app category from DB
	private void setFrequencyRange(int c) {
		category_thread = c;
		new Thread() {
			public synchronized void run() {
				int start=0, stop=0;
				if(category_thread == CATEGORY_HIGH_DEMANDING) {
					start = DetectAppLaunchService.MIN + 2;
					stop = DetectAppLaunchService.MAX;
				}
				else if(category_thread == CATEGORY_MODERATE_DEMANDING) {
					start = DetectAppLaunchService.MIN + 1;
					stop = DetectAppLaunchService.MAX - 1;
				}
				else if(category_thread == CATEGORY_LOW_DEMANDING) {
					start = DetectAppLaunchService.MIN;
					stop = DetectAppLaunchService.MAX - 2;
				}
				else if(category_thread == CATEGORY_UNKNOWN) {
					start = DetectAppLaunchService.MIN;
					stop = DetectAppLaunchService.MAX;
				}
				
				// Set the initial frequency to start
				SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(start));
				
				// Resume the thread calculating CPU usage
				synchronized(DetectAppLaunchService.bt.t){
					DetectAppLaunchService.bt.pleaseWait = false;
					DetectAppLaunchService.bt.t.notify();
				}
				
				int current = start;
				int count = 0;
				loop = true;
				float cpuUsage;
				
				while(loop) {
					
					// Wait according to CPU_PROBE_TIME
					try {
						Thread.sleep(CPU_PROBE_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					if(!loop)
						break;
					
					// Resume/Notify the thread calculating CPU usage
					synchronized(DetectAppLaunchService.bt.t){
						DetectAppLaunchService.bt.pleaseWait = false;
						DetectAppLaunchService.bt.t.notify();
					}
					
					// Read CPU usage
					cpuUsage = DetectAppLaunchService.bt.cpuUsage;
					if(cpuUsage > THRESHOLD_CPU_USAGE) {
						if((current+1 <= stop) && (count < MAX_COUNT)) {
							// Scale up the frequency to next level
							current++;
							SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(current));
							count++;
						}
						else {
							loop = false;
						}
					}
					else {
						if((current-1 >= start && (count < MAX_COUNT))) {
							// Scale down the frequency to previous level
							current--;
							SysFS.setSCALING_SETSPEED(DetectAppLaunchService.freq.get(current));
							count++;
						}
						else {
							loop = false;
						}
					}
				}
			}
		}.start();
	}
}