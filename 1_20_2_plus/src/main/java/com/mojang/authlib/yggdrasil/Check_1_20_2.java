package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.NMSUtils;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Check_1_20_2 {
    private static final Class<?> classMinecraftServer = NMSUtils.getNMSClass("MinecraftServer");
    private static final Method getServer = NMSUtils.getMethod(classMinecraftServer, "getServer");
    private static final Class<?> servicesClass = NMSUtils.getClassSilent("net.minecraft.server.Services");
    private static final Field fieldServices = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, servicesClass);

    private static final Field servicesSessionService = NMSUtils.getFirstFieldOfTypeSilent(servicesClass, MinecraftSessionService.class);
    public static boolean valid(){
        return NMSUtils.getClassSilent("com.mojang.authlib.yggdrasil.ServicesKeySet") != null;
    }

    public static void setup(IAlwaysOnline alwaysOnline) throws Exception{
        MinecraftServer minecraftServer = (MinecraftServer)getServer.invoke(null);
        YggdrasilMinecraftSessionService oldSessionService = (YggdrasilMinecraftSessionService)servicesSessionService.get(fieldServices.get(minecraftServer));
    }
}
