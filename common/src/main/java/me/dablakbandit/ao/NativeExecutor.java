package me.dablakbandit.ao;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import me.dablakbandit.ao.hybrid.IAlwaysOnline;

public interface NativeExecutor{
	
	int runAsyncRepeating(Runnable runnable, long delay, long period, TimeUnit timeUnit);
	
	void cancelTask(int taskID);
	
	void cancelAllOurTasks();
	
	void unregisterAllListeners();
	
	void log(Level level, String message);
	
	Path dataFolder();
	
	void disablePlugin();
	
	void registerListener();
	
	void broadcastMessage(String message);
	
	IAlwaysOnline getAOInstance();

	String getVersion();

	void notifyOfflineMode(boolean offlineMode);
	
}
