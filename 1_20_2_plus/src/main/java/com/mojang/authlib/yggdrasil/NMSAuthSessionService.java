package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import me.dablakbandit.ao.databases.Database;
import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.NMSUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Proxy;
import java.util.UUID;
import java.util.logging.Level;

public class NMSAuthSessionService extends YggdrasilMinecraftSessionService {

	private final IAlwaysOnline alwaysOnline;
	private final Database database;

	private final YggdrasilMinecraftSessionService oldSessionService;

	private final Method fetchProfile1;
	private final Method fetchProfile2;

	public NMSAuthSessionService(IAlwaysOnline alwaysOnline, YggdrasilMinecraftSessionService oldSessionService, ServicesKeySet servicesKeySet, Proxy proxy, Environment enviroment, Database database){
		super(servicesKeySet, proxy, enviroment);
		this.alwaysOnline = alwaysOnline;
		this.oldSessionService = oldSessionService;
		this.database = database;
		this.fetchProfile1 = NMSUtils.getMethod(oldSessionService.getClass(), "fetchProfile", GameProfile.class, boolean.class);
		this.fetchProfile2 = NMSUtils.getMethod(oldSessionService.getClass(), "fetchProfile", UUID.class, boolean.class);
	}


	public ProfileResult hasJoinedServer(String profileName, String serverId, InetAddress address) throws AuthenticationUnavailableException {
		if(alwaysOnline.getOfflineMode()) {
			UUID uuid = this.database.getUUID(profileName);
			if(uuid != null){
				return new ProfileResult(new GameProfile(uuid, profileName));
			}else{
				alwaysOnline.getNativeExecutor().log(Level.INFO, profileName + " " + "never joined this server before when mojang servers were online. Denying their access.");
				throw new AuthenticationUnavailableException("Mojang servers are offline and we can't authenticate the player with our own system.");
			}
		}else{
			return super.hasJoinedServer(profileName, serverId, address);
		}
	}


	public ProfileResult fetchProfile(GameProfile profile, boolean requireSecure) {
		try {
			return (ProfileResult) fetchProfile1.invoke(oldSessionService, profile, requireSecure);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}


	public ProfileResult fetchProfile(UUID profileId, boolean requireSecure) {
		try {
			return (ProfileResult) fetchProfile2.invoke(oldSessionService, profileId, requireSecure);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
