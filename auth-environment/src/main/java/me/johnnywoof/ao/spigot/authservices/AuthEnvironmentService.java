package me.johnnywoof.ao.spigot.authservices;

import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;

public abstract class AuthEnvironmentService extends YggdrasilMinecraftSessionService{

	public AuthEnvironmentService(YggdrasilAuthenticationService authenticationService, Object enviroment){
		super(authenticationService, (Environment)enviroment);
	}
	
}
