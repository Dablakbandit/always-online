package me.johnnywoof.ao.hybrid;

import java.util.logging.Level;

import com.google.gson.Gson;

import me.johnnywoof.ao.utils.CheckMethods;

public class MojangSessionCheck implements Runnable{
	
	private final AlwaysOnline	alwaysOnline;
	private final boolean		useHeadSessionServer, mojangServerStatus;
	private final int			totalCheckMethods;
	private final String		messageMojangOffline, messageMojangOnline;
	private final Gson			gson;
	
	public MojangSessionCheck(AlwaysOnline alwaysOnline){
		this.alwaysOnline = alwaysOnline;
		int methodCount = 0;
		this.mojangServerStatus = Boolean.parseBoolean(this.alwaysOnline.config.getProperty("mojang-server-status", "false"));
		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Mojang help page check: " + this.mojangServerStatus);
		if(this.mojangServerStatus)
			methodCount++;
		boolean headCheck = Boolean.parseBoolean(this.alwaysOnline.config.getProperty("http-head-session-server", "false"));
		if(methodCount == 0 && !headCheck){
			this.alwaysOnline.nativeExecutor.log(Level.WARNING, "No check methods have been enabled in the configuration. " + "Going to enable the head session server check.");
			headCheck = true;
		}
		this.useHeadSessionServer = headCheck;
		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Head Session server check: " + this.useHeadSessionServer);
		if(this.useHeadSessionServer){
			methodCount++;
			this.gson = new Gson();
		}else{
			this.gson = null;
		}
		
		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Total check methods active: " + methodCount);
		this.totalCheckMethods = methodCount;
		this.messageMojangOffline = this.alwaysOnline.config.getProperty("message-mojang-offline", "&5[&2AlwaysOnline&5]&a Mojang servers are now offline!");
		this.messageMojangOnline = this.alwaysOnline.config.getProperty("message-mojang-online", "&5[&2AlwaysOnline&5]&a Mojang servers are now online!");
	}
	
	@Override
	public void run(){
		if(!AlwaysOnline.CHECK_SESSION_STATUS)
			return;
		int downServiceReport = 0;
		if(this.useHeadSessionServer && !CheckMethods.directSessionServerStatus(this.gson))
			downServiceReport++;
		if(this.mojangServerStatus && !CheckMethods.mojangHelpPage())
			downServiceReport++;
		if(downServiceReport >= this.totalCheckMethods){// Offline
			if(!AlwaysOnline.MOJANG_OFFLINE_MODE){
				AlwaysOnline.MOJANG_OFFLINE_MODE = true;
				this.alwaysOnline.saveState();
				this.alwaysOnline.nativeExecutor.log(Level.INFO, "Mojang servers appear to be offline. Enabling mojang offline mode...");
				if(!"null".equals(this.messageMojangOffline))
					this.alwaysOnline.nativeExecutor.broadcastMessage(this.messageMojangOffline);
			}
		}else{// Online
			if(AlwaysOnline.MOJANG_OFFLINE_MODE){
				AlwaysOnline.MOJANG_OFFLINE_MODE = false;
				this.alwaysOnline.saveState();
				this.alwaysOnline.nativeExecutor.log(Level.INFO, "Mojang servers appear to be online. Disabling mojang offline mode...");
				if(!"null".equals(this.messageMojangOnline))
					this.alwaysOnline.nativeExecutor.broadcastMessage(this.messageMojangOnline);
			}
		}
	}
	
}
