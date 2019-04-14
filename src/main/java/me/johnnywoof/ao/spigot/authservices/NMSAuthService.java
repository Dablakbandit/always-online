package me.johnnywoof.ao.spigot.authservices;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import me.johnnywoof.ao.databases.Database;
import me.johnnywoof.ao.hybrid.AlwaysOnline;
import me.johnnywoof.ao.spigot.SpigotLoader;
import me.johnnywoof.ao.utils.Reflection;

public class NMSAuthService extends YggdrasilMinecraftSessionService{
	
	private final Database database;
	
	public NMSAuthService(YggdrasilAuthenticationService authenticationService, Database database){
		super(authenticationService);
		this.database = database;
	}
	
	private GameProfile runSuper(GameProfile user, String serverId, InetAddress address){
		try{
			MethodHandle handle = MethodHandles.lookup().findSpecial(YggdrasilMinecraftSessionService.class, "hasJoinedServer", MethodType.methodType(GameProfile.class, GameProfile.class, String.class, InetAddress.class), NMSAuthService.class);
			return (GameProfile)handle.invokeWithArguments(this, user, serverId, address);
		}catch(Exception e){
			e.printStackTrace();
		}catch(Throwable throwable){
			throwable.printStackTrace();
		}
		return null;
	}
	
	private GameProfile runSuper(GameProfile user, String serverId){
		try{
			MethodHandle handle = MethodHandles.lookup().findSpecial(YggdrasilMinecraftSessionService.class, "hasJoinedServer", MethodType.methodType(GameProfile.class, GameProfile.class, String.class), NMSAuthService.class);
			return (GameProfile)handle.invokeWithArguments(this, user, serverId);
		}catch(Exception e){
			e.printStackTrace();
		}catch(Throwable throwable){
			throwable.printStackTrace();
		}
		return null;
	}
	
	public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException{
		if(AlwaysOnline.MOJANG_OFFLINE_MODE){
			UUID uuid = this.database.getUUID(user.getName());
			if(uuid != null){
				return new GameProfile(uuid, user.getName());
			}else{
				SpigotLoader.getPlugin(SpigotLoader.class).log(Level.INFO, user.getName() + " " + "never joined this server before when mojang servers were online. Denying their access.");
				throw new AuthenticationUnavailableException("Mojang servers are offline and we can't authenticate the player with our own system.");
			}
		}else{
			return runSuper(user, serverId, address);
		}
	}
	
	public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException{
		if(AlwaysOnline.MOJANG_OFFLINE_MODE){
			UUID uuid = this.database.getUUID(user.getName());
			if(uuid != null){
				return new GameProfile(uuid, user.getName());
			}else{
				SpigotLoader.getPlugin(SpigotLoader.class).log(Level.INFO, user.getName() + " " + "never joined this server before when mojang servers were online. Denying their access.");
				throw new AuthenticationUnavailableException("Mojang servers are offline and we can't authenticate the player with our own system.");
			}
		}else{
			return runSuper(user, serverId);
		}
	}
	
	private static Class<?>					minecraftServer			= Reflection.getMinecraftClass("MinecraftServer");
	private static Reflection.MethodInvoker	getServer				= Reflection.getMethod(minecraftServer, "getServer");
	private static Reflection.FieldAccessor	authentificationService	= Reflection.getField(minecraftServer, YggdrasilAuthenticationService.class, 0);
	private static Reflection.FieldAccessor	sessionService			= Reflection.getField(minecraftServer, MinecraftSessionService.class, 0);
	
	public static void setUp(SpigotLoader spigotLoader) throws Exception{
		Object ms = getServer.invoke(minecraftServer);
		sessionService.set(ms, new NMSAuthService((YggdrasilAuthenticationService)authentificationService.get(ms), spigotLoader.alwaysOnline.database));
	}
	
}
