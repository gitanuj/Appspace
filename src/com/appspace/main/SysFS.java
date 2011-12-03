package com.appspace.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;

// Class providing methods to manipulate cpufreq files
public class SysFS {
	
	private static final String tag = "SysFS";
	
	// Path to the cpufreq directory of the android kernel
	private static String PATH_TO_CPUFREQ = "/sys/devices/system/cpu/cpu0/cpufreq/";
	
	// Names of files related to CPU
	private static String SCALING_SETSPEED = PATH_TO_CPUFREQ + "scaling_setspeed";
	private static String SCALING_AVAILABLE_FREQUENCIES = PATH_TO_CPUFREQ + "scaling_available_frequencies";
	private static String SCALING_CUR_FREQ = PATH_TO_CPUFREQ + "scaling_cur_freq";
	private static String SCALING_GOVERNOR = PATH_TO_CPUFREQ + "scaling_governor";
	private static String SCALING_MAX_FREQ = PATH_TO_CPUFREQ + "scaling_max_freq";
	private static String SCALING_MIN_FREQ = PATH_TO_CPUFREQ + "scaling_min_freq";
	
	// Superuser process
	private static Process p;
	private static DataOutputStream dos;
	
	// Spawn the superuser process
	public static boolean spawnSuperuserProcess() {
		try {
			p = Runtime.getRuntime().exec("su");
			dos = new DataOutputStream(p.getOutputStream());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	// Kill the superuser process
	public static void killSuperuserProcess() {
		new Thread() {
			public void run() {
				try {
					dos.writeBytes("exit\n");
					dos.flush();
					dos.close();
					p.destroy();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
	}

	// Private methods
	private static String readFile(String path) {
		String ans = "";
		StringBuffer str = new StringBuffer("");
		try {
			File f = new File(path);
			BufferedReader br = new BufferedReader(new FileReader(f));
			while((ans = br.readLine()) != null)
			{
				str.append(ans);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return str.toString();
	}
	private static boolean isCompatible() {
		boolean res = false;
		if(getSCALING_GOVERNOR().equalsIgnoreCase("userspace"))
			res = true;
		return res;
	}
	
	// Public methods
	public static String getSCALING_AVAILABLE_FREQUENCIES() {
		return readFile(SCALING_AVAILABLE_FREQUENCIES);
	}
	public static String getSCALING_CUR_FREQ() {
		return readFile(SCALING_CUR_FREQ);
	}
	public static String getSCALING_GOVERNOR() {
		return readFile(SCALING_GOVERNOR);
	}
	public static String getSCALING_MAX_FREQ() {
		return readFile(SCALING_MAX_FREQ);
	}
	public static String getSCALING_MIN_FREQ() {
		return readFile(SCALING_MIN_FREQ);
	}
	public static boolean setSCALING_SETSPEED(String sCALING_SETSPEED) {
		if(isCompatible()) {
			try {
				dos.writeBytes("echo \"" + sCALING_SETSPEED + "\" > " + SCALING_SETSPEED + "\n");
				dos.flush();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		else {
			Log.i(tag, "Please change to userspace governor");
			return false;
		}
	}
}

