package com.appspace.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

// BackgroundTask to perform persistent tasks
public class BackgroundTask extends AsyncTask<Context, String, String> {

	private static final String tag = "backgroundtask";
	private static final String activityNamePattern = ".*Starting: Intent .*";
	private static final String powerNamePattern = ".*set_screen_state.*";
    private static final String logCatCommand = "logcat ActivityManager:I *:S power:I";
    private static final String clearLogCatCommand = "logcat -c";
	Process p;
	DataOutputStream dos;
	BufferedReader br;
	boolean loop, loop_cpu;
	
	// Thread to constantly calculate CPU usage
	Thread t = new Thread() {
		public void run() {
			String l;
			RandomAccessFile raf;
			StringTokenizer st;
			int time1=0, time2=0, total1=0, total2=0, temp=0;
			float cpuUsage=0.0f;
			loop_cpu = true;
			try {
				raf = new RandomAccessFile(new File("/proc/stat"), "r");
				while(loop || loop_cpu) {
					while(loop_cpu) {
						l = raf.readLine();
						st = new StringTokenizer(l, " ");
						for(int i=0; st.hasMoreTokens(); i++) {
							if(i == 0) {
								st.nextToken();
								continue;
							}
							else if(i == 1 || i== 3) {
								time2 += Integer.parseInt(st.nextToken());
							}
							else {
								total2 += Integer.parseInt(st.nextToken());
							}
						}
						total2 += time2;
						
						temp = (int)(((float)(time2-time1)/(float)(total2-total1))*1000);
						cpuUsage = temp/10f;
						
						Log.i(tag, "CPU usage = "+cpuUsage+" %");
						
						time1 = time2;
						total1 = total2;
						time2 = total2 = 0;
						raf.seek(0);
						
						sleep(1000);
					}
				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			};
		}
	};
	
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
			
			// Calculate CPU Usage
			t.start();
			
			// Detect App launch
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
		loop = loop_cpu = false;
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
