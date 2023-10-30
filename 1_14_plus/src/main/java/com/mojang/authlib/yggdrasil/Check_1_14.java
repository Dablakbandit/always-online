package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.NMSUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Check_1_14 {

    private static final Class<?> classMinecraftServer = NMSUtils.getNMSClass("MinecraftServer");

    private static final Method getServer = NMSUtils.getMethod(classMinecraftServer, "getServer");
    private static final Field sessionService = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, MinecraftSessionService.class);

    private static final Field authentificationService = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, YggdrasilAuthenticationService.class);

    public static boolean valid(){
        return sessionService != null && authentificationService != null;
    }

    public static void setup(IAlwaysOnline alwaysOnline) throws Exception{
        Object ms = getServer.invoke(null);
        YggdrasilMinecraftSessionService oldSessionService = (YggdrasilMinecraftSessionService) sessionService.get(ms);
        Object service = new NMSAuthService(alwaysOnline, oldSessionService, (YggdrasilAuthenticationService) authentificationService.get(ms), alwaysOnline.getDatabase());
        sessionService.set(ms, service);
    }
}
