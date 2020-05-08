package me.johnnywoof.ao.hybrid;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import me.johnnywoof.ao.NativeExecutor;
import me.johnnywoof.ao.databases.Database;
import me.johnnywoof.ao.databases.FileDatabase;
import me.johnnywoof.ao.databases.MySQLDatabase;
import me.johnnywoof.ao.utils.CheckMethods;

public class AlwaysOnline{
	
	public static boolean		MOJANG_OFFLINE_MODE	= false, CHECK_SESSION_STATUS = true;
	
	public Database				database			= null;
	public Properties			config;
	
	public final NativeExecutor	nativeExecutor;
	private Path				stateFile;
	
	public AlwaysOnline(NativeExecutor nativeExecutor){
		this.nativeExecutor = nativeExecutor;
	}
	
	public void disable(){
		if(this.database != null){
			this.nativeExecutor.log(Level.INFO, "Saving data...");
			try{
				this.database.save();
				this.nativeExecutor.log(Level.INFO, "Closing database connections/streams...");
				this.database.close();
				this.database = null;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public void reload(){
		// Close the database
		this.disable();
		this.nativeExecutor.log(Level.INFO, "Loading configuration...");
		
		Path dataFolder = this.nativeExecutor.dataFolder();
		Path configFile = dataFolder.resolve("config.properties");
		Path oldConfigFile = dataFolder.resolve("config.yml");
		try{
			if(Files.notExists(dataFolder))
				Files.createDirectory(dataFolder);
			// Save default configuration
			if(Files.notExists(configFile)){
				// New config file doesn't exist but the old one does.
				if(Files.exists(oldConfigFile)){
					this.nativeExecutor.log(Level.WARNING, "Detected an old configuration file. Please update the new file config.properties");
					Files.move(oldConfigFile, dataFolder.resolve("obsolete_config.yml"));
				}
				// First time the plugin is running. Copy the configuration file to the data folder.
				InputStream in = this.getClass().getResourceAsStream("/config.properties");
				Files.write(configFile, ByteStreams.toByteArray(in));
				in.close();
			}
			
			// Load the configuration file.
			this.config = new Properties();
			InputStream in = Files.newInputStream(configFile, StandardOpenOption.READ);
			this.config.load(in);
			in.close();
			
			// Read the state.txt file and assign variables
			this.stateFile = dataFolder.resolve("state.txt");
			if(Files.isReadable(this.stateFile)){
				String data = new String(Files.readAllBytes(this.stateFile), StandardCharsets.UTF_8);
				if(data.contains(":")){
					String[] d = data.split(Pattern.quote(":"));
					CHECK_SESSION_STATUS = Boolean.parseBoolean(d[0]);
					MOJANG_OFFLINE_MODE = Boolean.parseBoolean(d[1]);
					this.nativeExecutor.log(Level.INFO, "Successfully loaded previous state variables!");
				}
			}
		}catch(IOException e){
			e.printStackTrace();
			this.nativeExecutor.log(Level.INFO, "Failed to load configuration file. Aborting...");
			this.nativeExecutor.disablePlugin();
			return;
		}
		
		if(Integer.valueOf(this.config.getProperty("config_version", "5")) < 5){
			this.nativeExecutor.log(Level.WARNING, "*-*-*-*-*-*-*-*-*-*-*-*-*-*");
			this.nativeExecutor.log(Level.WARNING, "Your configuration file is out of date!");
			this.nativeExecutor.log(Level.WARNING, "Please consider deleting it for a fresh new generated copy!");
			this.nativeExecutor.log(Level.WARNING, "Once done, do /alwaysonline reload");
			this.nativeExecutor.log(Level.WARNING, "*-*-*-*-*-*-*-*-*-*-*-*-*-*");
		}
		
		// No negative numbers.
		int checkInterval = Math.max(0, Integer.valueOf(this.config.getProperty("check-interval", "30")));
		if(checkInterval < 15){
			this.nativeExecutor.log(Level.WARNING, "Your check-interval is less than 15 seconds." + " This may cause issues and is recommended to be set to a higher number.");
		}
		
		// Kill any existing threads or listeners in case of a re-load
		this.nativeExecutor.cancelAllOurTasks();
		this.nativeExecutor.unregisterAllListeners();
		if(Boolean.parseBoolean(this.config.getProperty("use_mysql", "false"))){
			this.nativeExecutor.log(Level.INFO, "Loading MySQL database...");
			try{
				this.database = new MySQLDatabase(	this.nativeExecutor, this.config.getProperty("host", "127.0.0.1"), Integer.parseInt(this.config.getProperty("port", "3306")), this.config.getProperty("database-name", "minecraft"), this.config.getProperty("database-username", "root"),
													this.config.getProperty("database-password", "password"));
			}catch(SQLException e){
				this.nativeExecutor.log(Level.WARNING, "Failed to load the MySQL database, falling back to file database.");
				e.printStackTrace();
				this.database = new FileDatabase(dataFolder.resolve("playerData.txt"));
			}
		}else{
			this.nativeExecutor.log(Level.INFO, "Loading file database...");
			this.database = new FileDatabase(dataFolder.resolve("playerData.txt"));
			
		}
		this.nativeExecutor.log(Level.INFO, "Database is ready to go!");
		this.nativeExecutor.registerListener();
		this.nativeExecutor.runAsyncRepeating(new MojangSessionCheck(this), 0, checkInterval, TimeUnit.SECONDS);
	}
	
	public void saveState(){
		try{
			Files.write(this.stateFile, (CHECK_SESSION_STATUS + ":" + MOJANG_OFFLINE_MODE).getBytes(StandardCharsets.UTF_8));
		}catch(IOException e){
			this.nativeExecutor.log(Level.WARNING, "Failed to save state. This error can be safely ignored. [" + e.getMessage() + "]");
		}
	}
	
	public void printDebugInformation(){
		this.nativeExecutor.log(Level.INFO, "Session HEAD check: " + CheckMethods.directSessionServerStatus(new Gson()));
		this.nativeExecutor.log(Level.INFO, "Help page check: " + CheckMethods.mojangHelpPage());
		this.nativeExecutor.log(Level.INFO, "Mojang offline mode: " + MOJANG_OFFLINE_MODE);
		this.nativeExecutor.log(Level.INFO, "Check status: " + CHECK_SESSION_STATUS);
	}
	
}
