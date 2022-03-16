package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;

public abstract class AuthEnvironmentService extends YggdrasilMinecraftSessionService{

	protected final YggdrasilMinecraftSessionService oldSessionService;

	public AuthEnvironmentService(Object oldSessionService, YggdrasilAuthenticationService authenticationService, Object enviroment){
		super(authenticationService, (Environment)enviroment);
		this.oldSessionService = (YggdrasilMinecraftSessionService) oldSessionService;
	}
	
}
