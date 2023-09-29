package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import me.dablakbandit.ao.databases.Database;
import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.NMSUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;

public class NMSAuthEnvironmentService extends YggdrasilMinecraftSessionService {

	private final YggdrasilMinecraftSessionService oldSessionService;
	private final IAlwaysOnline alwaysOnline;
	private final Database database;
	private final Method fillGameProfile;
	private final Method fillProfileProperties;
	
	public NMSAuthEnvironmentService(IAlwaysOnline alwaysOnline, YggdrasilMinecraftSessionService oldSessionService, YggdrasilAuthenticationService authenticationService, Environment enviroment, Database database){
		super(authenticationService, enviroment);
		this.oldSessionService = oldSessionService;
		this.alwaysOnline = alwaysOnline;
		this.database = database;
		this.fillGameProfile = NMSUtils.getMethod(oldSessionService.getClass(), "fillGameProfile", GameProfile.class, boolean.class);
		this.fillProfileProperties = NMSUtils.getMethod(oldSessionService.getClass(), "fillProfileProperties", GameProfile.class, boolean.class);
	}
	
	private GameProfile runSuper(GameProfile user, String serverId, InetAddress address){
		try{
			MethodHandle handle = MethodHandles.lookup().findSpecial(YggdrasilMinecraftSessionService.class, "hasJoinedServer", MethodType.methodType(GameProfile.class, GameProfile.class, String.class, InetAddress.class), NMSAuthEnvironmentService.class);
			return (GameProfile)handle.invokeWithArguments(this, user, serverId, address);
		} catch(Throwable throwable){
			throwable.printStackTrace();
		}
		return null;
	}
	
	private GameProfile runSuper(GameProfile user, String serverId){
		try{
			MethodHandle handle = MethodHandles.lookup().findSpecial(YggdrasilMinecraftSessionService.class, "hasJoinedServer", MethodType.methodType(GameProfile.class, GameProfile.class, String.class), NMSAuthEnvironmentService.class);
			return (GameProfile)handle.invokeWithArguments(this, user, serverId);
		} catch(Throwable throwable){
			throwable.printStackTrace();
		}
		return null;
	}
	
	public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException{
		if(alwaysOnline.getOfflineMode()){
			UUID uuid = this.database.getUUID(user.getName());
			if(uuid != null){
				return new GameProfile(uuid, user.getName());
			}else{
				alwaysOnline.getNativeExecutor().log(Level.INFO, user.getName() + " " + "never joined this server before when mojang servers were online. Denying their access.");
				throw new AuthenticationUnavailableException("Mojang servers are offline and we can't authenticate the player with our own system.");
			}
		}else{
			return runSuper(user, serverId, address);
		}
	}
	
	public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException{
		if(alwaysOnline.getOfflineMode()){
			UUID uuid = this.database.getUUID(user.getName());
			if(uuid != null){
				return new GameProfile(uuid, user.getName());
			}else{
				alwaysOnline.getNativeExecutor().log(Level.INFO, user.getName() + " " + "never joined this server before when mojang servers were online. Denying their access.");
				throw new AuthenticationUnavailableException("Mojang servers are offline and we can't authenticate the player with our own system.");
			}
		}else{
			return runSuper(user, serverId);
		}
	}


	protected GameProfile fillGameProfile(GameProfile profile, boolean requireSecure) {
		try {
			return (GameProfile) fillGameProfile.invoke(oldSessionService, profile, requireSecure);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
		try {
			return (GameProfile) fillProfileProperties.invoke(oldSessionService, profile, requireSecure);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

}
