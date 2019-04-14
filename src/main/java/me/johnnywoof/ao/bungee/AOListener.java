package me.johnnywoof.ao.bungee;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

import me.johnnywoof.ao.hybrid.AlwaysOnline;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class AOListener implements Listener{
	
	private final Pattern		pat	= Pattern.compile("^[a-zA-Z0-9_-]{3,16}$");	// The regex to verify usernames;
	
	private String				MOTD;
	
	private final BungeeLoader	bungeeLoader;
	
	public AOListener(BungeeLoader bungeeLoader){
		this.bungeeLoader = bungeeLoader;
		this.MOTD = ChatColor.translateAlternateColorCodes('&', this.bungeeLoader.alwaysOnline.config.getProperty("message-motd-offline", "&eMojang servers are down,\\n&ebut you can still connect!"));
		if("null".equals(this.MOTD))
			this.MOTD = null;
	}
	
	// A high priority to allow other plugins to go first
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPreLogin(PreLoginEvent event){
		// Make sure it is not canceled
		if(event.isCancelled())
			return;
		if(AlwaysOnline.MOJANG_OFFLINE_MODE){// Make sure we are in mojang offline mode
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
		if(AlwaysOnline.MOJANG_OFFLINE_MODE && this.MOTD != null){
			ServerPing sp = event.getResponse();
			sp.setDescription(this.MOTD);
			event.setResponse(sp);
		}
	}
	
	@SuppressWarnings("deprecation")
	// Set priority to lowest since we'll be needing to go first
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPost(PostLoginEvent event){
		if(AlwaysOnline.MOJANG_OFFLINE_MODE){
			InitialHandler handler = (InitialHandler)event.getPlayer().getPendingConnection();
			try{
				UUID uuid = this.bungeeLoader.alwaysOnline.database.getUUID(event.getPlayer().getName());
				// Reflection
				Field sf = handler.getClass().getDeclaredField("uniqueId");
				sf.setAccessible(true);
				sf.set(handler, uuid);
				
				sf = handler.getClass().getDeclaredField("offlineId");
				sf.setAccessible(true);
				sf.set(handler, uuid);
				
				Collection<String> g = this.bungeeLoader.getProxy().getConfigurationAdapter().getGroups(event.getPlayer().getName());
				g.addAll(this.bungeeLoader.getProxy().getConfigurationAdapter().getGroups(event.getPlayer().getUniqueId().toString()));
				
				UserConnection userConnection = (UserConnection)event.getPlayer();
				for(String s : g){
					userConnection.addGroups(s);
				}
				this.bungeeLoader.getLogger().info(event.getPlayer().getName() + " successfully logged in while mojang servers were offline!");
				// ProxyServer.getInstance().getLogger().info("Overriding uuid for " + event.getPlayer().getName() + " to " + uuid.toString() + "! New uuid is " + event.getPlayer().getUniqueId().toString());
			}catch(Exception e){// Play it safe, if an error deny the player
				event.getPlayer().disconnect("Sorry, the mojang servers are offline and we can't authenticate you with our own system!");
				this.bungeeLoader.getLogger().warning("Internal error for " + event.getPlayer().getName() + ", preventing login.");
				e.printStackTrace();
			}
			
		}else{
			// If we are not in mojang offline mode, update the player data
			final String username = event.getPlayer().getName();
			final String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
			final UUID uuid = event.getPlayer().getUniqueId();
			this.bungeeLoader.getProxy().getScheduler().runAsync(this.bungeeLoader, new Runnable(){
				@Override
				public void run(){
					AOListener.this.bungeeLoader.alwaysOnline.database.updatePlayer(username, ip, uuid);
				}
			});
		}
	}
	
	/**
	 * Validate username with regular expression
	 *
	 * @param username username for validation
	 * @return true valid username, false invalid username
	 */
	public boolean validate(String username){
		return username != null && pat.matcher(username).matches();
	}
	
}
