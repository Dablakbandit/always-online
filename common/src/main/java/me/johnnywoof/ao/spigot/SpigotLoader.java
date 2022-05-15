package me.johnnywoof.ao.spigot;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import me.johnnywoof.ao.databases.Database;
import me.johnnywoof.ao.databases.MySQLDatabase;
import me.johnnywoof.ao.spigot.authservices.NMSAuthSetup;
import me.johnnywoof.ao.spigot.metrics.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import me.johnnywoof.ao.NativeExecutor;
import me.johnnywoof.ao.hybrid.AlwaysOnline;

public class SpigotLoader extends JavaPlugin implements NativeExecutor{
	
	public final AlwaysOnline alwaysOnline = new AlwaysOnline(this);
	
	@Override
	public void onEnable(){
		this.alwaysOnline.reload();
		// Native enable setup
		
		if(!this.getServer().getOnlineMode()){
			this.getLogger().info("This server is running in offline mode, so this plugin will have no use on this server!");
			this.getLogger().info("If you are running bungeecord, please put AlwaysOnline in the bungeecord plugins directory.");
			this.getLogger().info("If you are not running bungeecord, then please remove AlwaysOnline.");
			this.getPluginLoader().disablePlugin(this);
			return;
		}
		
		try{
			this.getLogger().info("Setting up NMS authentication service...");
			NMSAuthSetup.setUp(this);
		}catch(Exception e){
			e.printStackTrace();
			this.getLogger().severe("Failed to override the authentication handler. Due to possible security risks, the server will now shut down.");
			this.getLogger().severe("If this issue persists, please contact the author (" + this.getDescription().getAuthors() + ") and remove " + this.getDescription().getName() + " from your server temporarily.");
			this.getServer().shutdown();
		}

		Metrics metrics = new Metrics(this, 15201);
		Database database = alwaysOnline.getDatabase();
		String databaseType = "FlatFile";
		if(database instanceof MySQLDatabase){
			databaseType = "MySQL";
		}
		String finalDatabaseType = databaseType;
		metrics.addCustomChart(new Metrics.SimplePie("database_type", () -> finalDatabaseType));
	}
	
	@Override
	public void onDisable(){
		this.alwaysOnline.disable();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(args.length != 1){
			this.displayHelp(sender);
		}else{
			String pluginName = this.getDescription().getName();
			switch(args[0].toLowerCase()){
			case "toggle":
				AlwaysOnline.MOJANG_OFFLINE_MODE = !AlwaysOnline.MOJANG_OFFLINE_MODE;
				sender.sendMessage(ChatColor.GOLD + "Mojang offline mode is now " + ((AlwaysOnline.MOJANG_OFFLINE_MODE ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled")) + ChatColor.GOLD + "!");
				if(!AlwaysOnline.MOJANG_OFFLINE_MODE){
					sender.sendMessage(ChatColor.GOLD + pluginName + " will now treat the mojang servers as being online.");
				}else{
					sender.sendMessage(ChatColor.GOLD + pluginName + " will no longer treat the mojang servers as being online.");
					
				}
				break;
			case "disable":
				AlwaysOnline.CHECK_SESSION_STATUS = false;
				sender.sendMessage(ChatColor.GOLD + pluginName + " has been disabled! " + pluginName + " will no longer check to see if the session server is offline.");
				break;
			case "enable":
				AlwaysOnline.CHECK_SESSION_STATUS = true;
				sender.sendMessage(ChatColor.GOLD + pluginName + " has been enabled! " + pluginName + " will now check to see if the session server is offline.");
				break;
			case "reload":
				// TODO Add support?
				sender.sendMessage(ChatColor.RED + "The reload command is not supported when running " + pluginName + " with spigot.");
				break;
			case "debug":
				if(sender instanceof Player)
					sender.sendMessage(ChatColor.GREEN + "Check console for debug information");
				this.alwaysOnline.printDebugInformation();
				break;
			case "resetcache":
				this.alwaysOnline.database.resetCache();
				this.getLogger().info("Cache reset'd");
				break;
			default:
				this.displayHelp(sender);
				break;
			}
			this.alwaysOnline.saveState();
		}
		return true;
		
	}
	
	private void displayHelp(CommandSender sender){
		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "----------" + ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + "AlwaysOnline " + ChatColor.GRAY + this.getDescription().getVersion() + "" + ChatColor.GOLD + "]" + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "----------");
		sender.sendMessage(ChatColor.GOLD + "/alwaysonline toggle - " + ChatColor.DARK_GREEN + "Toggles between mojang online mode");
		sender.sendMessage(ChatColor.GOLD + "/alwaysonline enable - " + ChatColor.DARK_GREEN + "Enables the plugin");
		sender.sendMessage(ChatColor.GOLD + "/alwaysonline disable - " + ChatColor.DARK_GREEN + "Disables the plugin");
		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "------------------------------");
	}
	
	@Override
	public int runAsyncRepeating(Runnable runnable, long delay, long period, TimeUnit timeUnit){
		return this.getServer().getScheduler().runTaskTimerAsynchronously(this, runnable, (timeUnit.toSeconds(delay) * 20), (timeUnit.toSeconds(period) * 20)).getTaskId();
	}
	
	@Override
	public void cancelTask(int taskID){
		this.getServer().getScheduler().cancelTask(taskID);
	}
	
	@Override
	public void cancelAllOurTasks(){
		this.getServer().getScheduler().cancelTasks(this);
	}
	
	@Override
	public void unregisterAllListeners(){
		HandlerList.unregisterAll(this);
	}
	
	@Override
	public void log(Level level, String message){
		this.getLogger().log(level, message);
	}
	
	@Override
	public Path dataFolder(){
		return this.getDataFolder().toPath();
	}
	
	@Override
	public void disablePlugin(){
		this.getServer().getPluginManager().disablePlugin(this);
	}
	
	@Override
	public void registerListener(){
		this.getServer().getPluginManager().registerEvents(new AOListener(this), this);
	}
	
	@Override
	public void broadcastMessage(String message){
		this.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
	}
	
	@Override
	public AlwaysOnline getAOInstance(){
		return this.alwaysOnline;
	}

	@Override
	public String getVersion() {
		return getDescription().getVersion();
	}

}
