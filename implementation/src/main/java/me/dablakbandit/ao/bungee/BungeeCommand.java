package me.dablakbandit.ao.bungee;

import java.util.logging.Level;

import me.dablakbandit.ao.hybrid.AlwaysOnline;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class BungeeCommand extends Command{
	
	private final BungeeLoader ao;
	
	public BungeeCommand(BungeeLoader ao){
		super("alwaysonline", "alwaysonline.usage", "ao");
		this.ao = ao;
	}
	
	public void execute(CommandSender sender, String[] args){
		if(args.length <= 0){
			this.displayHelp(sender);
		}else{
			AlwaysOnline alwaysOnline = ao.getAOInstance();
			switch(args[0].toLowerCase()){
			case "toggle":
				alwaysOnline.toggleOfflineMode();
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "Mojang offline mode is now " + ((alwaysOnline.getOfflineMode() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled")) + ChatColor.GOLD + "!"));
				if(!alwaysOnline.getOfflineMode()){
					sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "AlwaysOnline will now treat the mojang servers as being online."));
				}else{
					sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "AlwaysOnline will no longer treat the mojang servers as being online."));
				}
				break;
			case "disable":
				alwaysOnline.setCheckSessionStatus(false);
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "AlwaysOnline has been disabled! AlwaysOnline will no longer check to see if the session server is offline."));
				break;
			case "enable":
				alwaysOnline.setCheckSessionStatus(true);
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "AlwaysOnline has been enabled! AlwaysOnline will now check to see if the session server is offline."));
				break;
			case "reload":
				this.ao.alwaysOnline.reload();
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "AlwaysOnline has been reloaded!"));
				break;
			case "debug":
				this.ao.alwaysOnline.printDebugInformation();
				break;
			case "resetcache":
				this.ao.alwaysOnline.database.resetCache();
				this.ao.alwaysOnline.nativeExecutor.log(Level.INFO, "Cache reset'd");
				break;
			default:
				this.displayHelp(sender);
				break;
			}
			this.ao.alwaysOnline.saveState();
			
		}
		
	}
	
	private void displayHelp(CommandSender sender){
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "----------" + ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + "AlwaysOnline " + ChatColor.GRAY + ao.getDescription().getVersion() + "" + ChatColor.GOLD + "]" + ChatColor.GOLD + ""
														+ ChatColor.STRIKETHROUGH + "----------"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "/alwaysonline toggle - " + ChatColor.DARK_GREEN + "Toggles between mojang online mode"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "/alwaysonline enable - " + ChatColor.DARK_GREEN + "Enables the plugin"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "/alwaysonline disable - " + ChatColor.DARK_GREEN + "Disables the plugin"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "/alwaysonline reload - " + ChatColor.DARK_GREEN + "Reloads the configuration file"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "------------------------------"));
	}
	
}
