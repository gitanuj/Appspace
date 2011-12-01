package com.appspace.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class DetectAppLaunchService extends Service {

	private static final String tag = "Service";
	private static final String activityNamePattern = ".*Starting: Intent .*";
	private static final String powerNamePattern = ".*set_screen_state.*";
    private static final String logCatCommand = "logcat ActivityManager:I *:S power:I";
    private static final String clearLogCatCommand = "logcat -c";
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

	private class BackgroundTask extends AsyncTask<Context, String, String> {

		Process p;
		DataOutputStream dos;
		BufferedReader br;
		boolean loop;
		
		@Override
		protected String doInBackground(Context... params) {
			try {
				loop = true;
				Log.i(tag, "service thread started");
				p = Runtime.getRuntime().exec(clearLogCatCommand);
				p.waitFor();
				p = Runtime.getRuntime().exec(logCatCommand);
				dos = new DataOutputStream(p.getOutputStream());
				dos.writeBytes(logCatCommand+"\n");
				dos.flush();
				br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = "", temp = "";
				int start, end;
				
				while(loop) {
					if(br.ready()) {
						line = br.readLine();
						if(line.matches(activityNamePattern)) {
							start = line.indexOf("cmp=");
							temp = line.substring(start);
							end = temp.indexOf(" }");
							temp = temp.substring(4, end);
							
							params[0].sendBroadcast(new Intent(AppspaceReceiver.APP_LAUNCH_DETECTED).putExtra("appName", temp));
						}
						else if(line.matches(powerNamePattern)) {
							char c = line.charAt(line.length()-1);
							if(c == '0') {
								params[0].sendBroadcast(new Intent(AppspaceReceiver.SCREEN_OFF));
							}
							else if(c == '1') {
								params[0].sendBroadcast(new Intent(AppspaceReceiver.SCREEN_ON));
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			loop = false;
			new Thread() {
				public void run() {
					try {
						dos.writeBytes("exit\n");
						dos.flush();
						dos.close();
						br.close();
						p.destroy();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
    	
    }
}
