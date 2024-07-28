package com.mojang.authlib.yggdrasil;

import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.NMSUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;

public class Check_1_20_2 {
    private static final Class<?> classMinecraftServer = NMSUtils.getNMSClass("MinecraftServer");
    private static final Class<?> classServices = NMSUtils.getClassSilent("net.minecraft.server.Services");
    private static final Class<?> classYggdrasilMinecraftSessionService = NMSUtils.getClassSilent("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");
    private static final Class<?> classServicesKeySet = NMSUtils.getClassSilent("com.mojang.authlib.yggdrasil.ServicesKeySet");
    private static final Class<?> classMinecraftClient = NMSUtils.getClassSilent("com.mojang.authlib.minecraft.client.MinecraftClient");
    private static final Class<?> classMinecraftSessionService = NMSUtils.getClassSilent("com.mojang.authlib.minecraft.MinecraftSessionService");

    private static final Method getServer = NMSUtils.getMethod(classMinecraftServer, "getServer");

    private static final Field fieldServices = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, classServices);

    private static final Field fieldServicesSessionService = NMSUtils.getFirstFieldOfTypeSilent(classServices, classMinecraftSessionService);
    private static final Field fieldServicesKeySet = NMSUtils.getFirstFieldOfTypeSilent(classYggdrasilMinecraftSessionService, classServicesKeySet);
    private static final Field fieldMinecraftClient = NMSUtils.getFirstFieldOfTypeSilent(classYggdrasilMinecraftSessionService, classMinecraftClient);
    private static final Field fieldProxy = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftClient, Proxy.class);

    private static Constructor<?> conServices;

    static {
        try {
            conServices = classServices.getConstructors()[0];
        } catch (Exception e) {
        }
    }

    public static boolean valid(){
        return NMSUtils.getClassSilent("com.mojang.authlib.yggdrasil.ServicesKeySet") != null;
    }

    public static void setup(IAlwaysOnline alwaysOnline) throws Exception{
        MinecraftServer minecraftServer = (MinecraftServer)getServer.invoke(null);
        Services services = (Services) fieldServices.get(minecraftServer);
        YggdrasilMinecraftSessionService oldSessionService = (YggdrasilMinecraftSessionService) fieldServicesSessionService.get(services);
        ServicesKeySet servicesKeySet = (ServicesKeySet) fieldServicesKeySet.get(oldSessionService);
        Object minecraftClient = fieldMinecraftClient.get(oldSessionService);
        Proxy proxy = (Proxy) fieldProxy.get(minecraftClient);

        NMSAuthSessionService service = new NMSAuthSessionService(alwaysOnline, oldSessionService, servicesKeySet, proxy, YggdrasilEnvironment.PROD.getEnvironment(), alwaysOnline.getDatabase());

        Object[] objects = new Object[conServices.getParameterCount()];
        objects[0] = service;

        for (int i = 1; i < conServices.getParameterCount(); i++) {
            objects[i] = NMSUtils.getFirstFieldOfType(classServices, conServices.getParameterTypes()[i]).get(services);
        }

        Object newServices = conServices.newInstance(objects);
        fieldServices.set(minecraftServer, newServices);
    }

}
