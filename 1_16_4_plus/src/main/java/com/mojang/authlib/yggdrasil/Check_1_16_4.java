package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.Environment;
import com.mojang.authlib.minecraft.BaseMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.NMSUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class Check_1_16_4 {

    private static final Class<?> classMinecraftServer = NMSUtils.getNMSClass("MinecraftServer");
    private static final Field sessionService = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, MinecraftSessionService.class);
    private static final Method getServer = NMSUtils.getMethod(classMinecraftServer, "getServer");
    private static final Class<?> servicesClass = NMSUtils.getClassSilent("net.minecraft.server.Services");
    private static final Field fieldServices = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, servicesClass);

    private static final Field servicesSessionService = NMSUtils.getFirstFieldOfTypeSilent(servicesClass, MinecraftSessionService.class);

    //ENVIRONMENT
    private static final Class<?> classEnvironment = NMSUtils.getClassSilent("com.mojang.authlib.Environment");
    private static final Class<?> baseMinecraftSessionService = NMSUtils.getClassSilent("com.mojang.authlib.minecraft.BaseMinecraftSessionService");
    private static final Field environment = NMSUtils.getFirstFieldOfTypeSilent(YggdrasilAuthenticationService.class, classEnvironment);
    private static final Field baseAuthentificationService = NMSUtils.getFirstFieldOfTypeSilent(baseMinecraftSessionService, AuthenticationService.class);
    public static boolean valid(){
        return baseAuthentificationService != null;
    }

    public static void setup(IAlwaysOnline alwaysOnline) throws Exception{
        Object ms = getServer.invoke(null);
        YggdrasilMinecraftSessionService oldSessionService = (YggdrasilMinecraftSessionService)servicesSessionService.get(fieldServices.get(ms));
        YggdrasilAuthenticationService current = (YggdrasilAuthenticationService)baseAuthentificationService.get(oldSessionService);
        Object service = new NMSAuthEnvironmentService(alwaysOnline, oldSessionService, current, (Environment) environment.get(current), alwaysOnline.getDatabase());
        if(servicesClass == null){
            sessionService.set(ms, service);
        }else{
            //servicesSessionService.set(, service);
            Object oldService = fieldServices.get(ms);
            List<Field> fields = NMSUtils.getFields(servicesClass);
            fields = fields.stream().filter(field -> !Modifier.isStatic(field.getModifiers())).collect(Collectors.toList());
            Object[] objects = new Object[fields.size()];
            Constructor<?> constructor = NMSUtils.getConstructor(servicesClass, objects.length);
            objects[0] = service;
            for (int i = 1; i < fields.size(); i++) {
                objects[i] = fields.get(i).get(oldService);
            }
            Object customService = constructor.newInstance(objects);
            fieldServices.set(ms, customService);
        }
    }
}
