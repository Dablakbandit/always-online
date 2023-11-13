package me.dablakbandit.ao.update;

import me.dablakbandit.ao.NativeExecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class UpdateChecker{
	
	private static final UpdateChecker updateChecker = new UpdateChecker();
	
	public static UpdateChecker getInstance(){
		return updateChecker;
	}

	private String			latest;
	private NativeExecutor nativeExecutor;
	private int schedule = -1;
	private UpdateChecker(){
		
	}
	
	public void start(NativeExecutor nativeExecutor){
		this.latest = nativeExecutor.getVersion();
		this.nativeExecutor = nativeExecutor;
		schedule = nativeExecutor.runAsyncRepeating(this::checkUpdate, 1, 1440, TimeUnit.MINUTES);
	}

	public void checkUpdate(){
		int latestVersion = Integer.parseInt(latest.replaceAll("[^0-9]", ""));
		nativeExecutor.log(Level.INFO, "Checking update for AlwaysOnline v" + latest);
		try{
			URL checkURL = new URL("https://api.spigotmc.org/legacy/update.php?resource=66591");
			URLConnection con = checkURL.openConnection();
			con.setConnectTimeout(2000);
			String new_version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
			int new_version_number = Integer.parseInt(new_version .replaceAll("[^0-9]", ""));
			if(new_version_number > latestVersion){
				latest = new_version;
				nativeExecutor.log(Level.INFO, "Plugin AlwaysOnline has been updated to v" + new_version + ", please update your server.");
			}
		}catch(Exception e){
			nativeExecutor.log(Level.WARNING, "Unable to check update for AlwaysOnline v" + latest);
		}
	}

	public int getSchedule() {
		return schedule;
	}
}
