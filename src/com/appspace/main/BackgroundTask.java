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

// BackgroundTask to perform persistent tasks
public class BackgroundTask extends AsyncTask<Context, String, String> {

	private static final String CPUConfigFile = "/proc/stat";
	private static final String activityNamePattern = ".*Starting: Intent .*";
	private static final String powerNamePattern = ".*set_screen_state.*";
    private static final String logCatCommand = "logcat ActivityManager:I *:S power:I";
    private static final String clearLogCatCommand = "logcat -c";
	private Process p;
	private DataOutputStream dos;
	private BufferedReader br;
	public boolean loop,loop_cpu;
	public float cpuUsage;
	public boolean pleaseWait = true;
	
	// Thread to constantly calculate CPU usage
	public Thread t = new Thread() {
		public void run() {
			String l;
			RandomAccessFile raf;
			StringTokenizer st;
			int time1=0, time2=0, total1=0, total2=0;
			cpuUsage=0.0f;
			loop_cpu = true;
			try {
				raf = new RandomAccessFile(new File(CPUConfigFile), "r");
				while(loop_cpu) {
					l = raf.readLine();
					// Tokenize the line
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
					
					cpuUsage = (((float)(time2-time1)/(float)(total2-total1))*100);
					time1 = time2;
					total1 = total2;
					time2 = total2 = 0;
					raf.seek(0);
					
					// Thread must wait here
					synchronized(Thread.currentThread()) {
						while(pleaseWait) {
							Thread.currentThread().wait();
						}
					}
					// Thread must resume once it is notified
					pleaseWait = true;
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
			// Set the loop variable for detecting app launch
			loop = true;
			
			// Clear the logcat
			p = Runtime.getRuntime().exec(clearLogCatCommand);
			p.waitFor();
			p = Runtime.getRuntime().exec(logCatCommand);
			dos = new DataOutputStream(p.getOutputStream());
			dos.writeBytes(logCatCommand+"\n");
			dos.flush();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "", temp = "";
			int start, end;
			String lastApp="";
			
			// Calculate CPU Usage
			t.start();
			
			// Detect App launch
			while((line = br.readLine()) != null && loop) {
				if(line.matches(activityNamePattern)) {
					start = line.indexOf("cmp=");
					temp = line.substring(start);
					end = temp.indexOf("/");
					temp = temp.substring(4, end);
					
					// Send signal to receiver that a new app launch has been detected
					if(!temp.equals(lastApp))
						params[0].sendBroadcast(new Intent(AppspaceReceiver.APP_LAUNCH_DETECTED).putExtra("package", temp));
					
					lastApp = temp;
				}
				else if(line.matches(powerNamePattern)) {
					char c = line.charAt(line.length()-1);
					if(c == '0') {
						// Send signal to receiver that screen has been turned off
						params[0].sendBroadcast(new Intent(AppspaceReceiver.SCREEN_OFF));
					}
					else if(c == '1') {
						// Send signal to receiver that screen has been turned on
						params[0].sendBroadcast(new Intent(AppspaceReceiver.SCREEN_ON));
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
		// Resume the CPU usage calculating thread
		synchronized(t){
			pleaseWait = false;
			t.notify();
		}
		// Stop any thread running
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
