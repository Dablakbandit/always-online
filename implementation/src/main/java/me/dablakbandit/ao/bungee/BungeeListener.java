package me.dablakbandit.ao.bungee;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import me.dablakbandit.ao.proxy.ProxyListener;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class BungeeListener extends ProxyListener implements Listener{

	private final BungeeLoader	bungeeLoader;
	
	public BungeeListener(BungeeLoader bungeeLoader){
		super(bungeeLoader);
		this.bungeeLoader = bungeeLoader;
		this.MOTD = ChatColor.translateAlternateColorCodes('&', this.bungeeLoader.getAOInstance().config.getProperty("message-motd-offline", "&eMojang servers are down,\\n&ebut you can still connect!"));
		if("null".equals(this.MOTD))
			this.MOTD = null;
	}
	
	// A high priority to allow other plugins to go first
	@EventHandler(priority = 65)
	public void onPreLogin(PreLoginEvent event){
		// Make sure it is not canceled
		if(event.isCancelled())
			return;
		if(bungeeLoader.getAOInstance().getOfflineMode()){// Make sure we are in mojang offline mode
			// Verify if the name attempting to connect is even verified
			if(!this.validate(event.getConnection().getName())){
				event.setCancelReason(this.bungeeLoader.alwaysOnline.config.getProperty("message-kick-invalid", "Invalid username. Hacking?"));
				event.setCancelled(true);
				return;
				
			}
			// Initialize our hacky stuff
			InitialHandler handler = (InitialHandler)event.getConnection();
			// Get the connecting ip
			final String ip = handler.getAddress().getAddress().getHostAddress();
			// Get last known ip
			final String lastip = this.bungeeLoader.alwaysOnline.database.getIP(event.getConnection().getName());
			if(lastip == null){// If null the player connecting is new
				event.setCancelReason(this.bungeeLoader.alwaysOnline.config.getProperty("message-kick-new", "We can not let you join because the mojang servers are offline!"));
				event.setCancelled(true);
				this.bungeeLoader.getLogger().info("Denied " + event.getConnection().getName() + " from logging in cause their ip [" + ip + "] has never connected to this server before!");
			}else{
				if(ip.equals(lastip)){// If it matches set handler to offline mode, so it does not authenticate player with mojang
					this.bungeeLoader.getLogger().info("Skipping session login for player " + event.getConnection().getName() + " [Connected ip: " + ip + ", Last ip: " + lastip + "]!");
					handler.setOnlineMode(false);
					UUID uuid = this.bungeeLoader.alwaysOnline.database.getUUID(event.getConnection().getName());
					handler.setUniqueId(uuid);
				}else{// Deny the player from joining
					this.bungeeLoader.getLogger().info("Denied " + event.getConnection().getName() + " from logging in cause their ip [" + ip + "] does not match their last ip!");
					handler.setOnlineMode(true);
					event.setCancelReason(this.bungeeLoader.alwaysOnline.config.getProperty("message-kick-ip", "We can not let you join since you are not on the same computer you logged on before!"));
					event.setCancelled(true);
				}
			}
		}
	}
	
	// Set priority to highest to almost guaranteed to have our MOTD displayed
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPing(ProxyPingEvent event){
		if(bungeeLoader.getAOInstance().getOfflineMode() && this.MOTD != null){
			ServerPing sp = event.getResponse();
			sp.setDescription(this.MOTD);
			event.setResponse(sp);
		}
	}
	
	@SuppressWarnings("deprecation")
	// Set priority to lowest since we'll be needing to go first
	@EventHandler(priority = -65)
	public void onPost(PostLoginEvent event){
        if (!bungeeLoader.getAOInstance().getOfflineMode()) {
            // If we are not in mojang offline mode, update the player data
            final String username = event.getPlayer().getName();
            final String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
            final UUID uuid = event.getPlayer().getUniqueId();
            this.bungeeLoader.getProxy().getScheduler().runAsync(this.bungeeLoader, new Runnable(){
                @Override
                public void run(){
                    BungeeListener.this.bungeeLoader.alwaysOnline.database.updatePlayer(username, ip, uuid);
                }
            });
        }
    }

}
