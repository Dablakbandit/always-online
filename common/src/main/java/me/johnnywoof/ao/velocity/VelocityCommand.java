package me.johnnywoof.ao.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import me.johnnywoof.ao.bungee.BungeeLoader;
import me.johnnywoof.ao.hybrid.AlwaysOnline;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.logging.Level;

public class VelocityCommand implements SimpleCommand {

    private final VelocityLoader ao;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacy('&');

    public VelocityCommand(VelocityLoader ao) {
        this.ao = ao;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("alwaysonline.usage");
    }

    @Override
    public void execute(final Invocation invocation) {
        execute(invocation.source(), invocation.arguments());
    }

    public void execute(CommandSource sender, String[] args) {
        if (args.length <= 0) {
            this.displayHelp(sender);
        } else {
            switch (args[0].toLowerCase()) {
                case "toggle":
                    AlwaysOnline.MOJANG_OFFLINE_MODE = !AlwaysOnline.MOJANG_OFFLINE_MODE;
                    sendMessage(sender, ChatColor.GOLD + "Mojang offline mode is now " + ((AlwaysOnline.MOJANG_OFFLINE_MODE ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled")) + ChatColor.GOLD + "!");
                    if (!AlwaysOnline.MOJANG_OFFLINE_MODE) {
                        sendMessage(sender, ChatColor.GOLD + "AlwaysOnline will now treat the mojang servers as being online.");
                    } else {
                        sendMessage(sender, ChatColor.GOLD + "AlwaysOnline will no longer treat the mojang servers as being online.");
                    }
                    break;
                case "disable":
                    AlwaysOnline.CHECK_SESSION_STATUS = false;
                    sendMessage(sender, ChatColor.GOLD + "AlwaysOnline has been disabled! AlwaysOnline will no longer check to see if the session server is offline.");
                    break;
                case "enable":
                    AlwaysOnline.CHECK_SESSION_STATUS = true;
                    sendMessage(sender, ChatColor.GOLD + "AlwaysOnline has been enabled! AlwaysOnline will now check to see if the session server is offline.");
                    break;
                case "reload":
                    this.ao.alwaysOnline.reload();
                   	sendMessage(sender, ChatColor.GOLD + "AlwaysOnline has been reloaded!");
                    break;
                case "debug":
                    this.ao.alwaysOnline.printDebugInformation();
                    break;
                case "resetcache":
                    this.ao.alwaysOnline.database.resetCache();
                    this.ao.alwaysOnline.nativeExecutor.log(Level.INFO, "Cache reset");
                    break;
                default:
                    this.displayHelp(sender);
                    break;
            }
            this.ao.alwaysOnline.saveState();

        }

    }

    private void displayHelp(CommandSource source) {

       sendMessage(source, ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "----------" + ChatColor.GOLD + "[" + ChatColor.DARK_GREEN + "AlwaysOnline " + ChatColor.GRAY + "${version}" + ChatColor.GOLD + "]" + ChatColor.GOLD + ""
        + ChatColor.STRIKETHROUGH + "----------");
       sendMessage(source, ChatColor.GOLD + "/alwaysonline toggle - " + ChatColor.DARK_GREEN + "Toggles between mojang online mode");
       sendMessage(source, ChatColor.GOLD + "/alwaysonline enable - " + ChatColor.DARK_GREEN + "Enables the plugin");
       sendMessage(source, ChatColor.GOLD + "/alwaysonline disable - " + ChatColor.DARK_GREEN + "Disables the plugin");
       sendMessage(source, ChatColor.GOLD + "/alwaysonline reload - " + ChatColor.DARK_GREEN + "Reloads the configuration file");
       sendMessage(source, ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "------------------------------");
    }

    private void sendMessage(CommandSource source, String message) {
		source.sendMessage(legacy.deserialize(message));
    }

}
