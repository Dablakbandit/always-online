package me.johnnywoof.ao.spigot.authservices;

import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.minecraft.BaseMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.NMSAuthEnvironmentService;
import com.mojang.authlib.yggdrasil.NMSAuthService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import me.johnnywoof.ao.spigot.SpigotLoader;
import me.johnnywoof.ao.utils.NMSUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class NMSAuthSetup {

    private static Class<?> classMinecraftServer = NMSUtils.getNMSClass("MinecraftServer");
    private static Method setUsesAuthentication = NMSUtils.getMethodSilent(classMinecraftServer, "setUsesAuthentication", boolean.class);
    private static Method getServer = NMSUtils.getMethod(classMinecraftServer, "getServer");
    private static Field sessionService = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, MinecraftSessionService.class);
    private static Class<?> servicesClass = NMSUtils.getClassSilent("net.minecraft.server.Services");
    private static Field fieldServices = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, servicesClass);
    private static Field servicesSessionService = NMSUtils.getFirstFieldOfTypeSilent(servicesClass, MinecraftSessionService.class);

    //CLASSIC < 1.16
    private static Field authentificationService = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, YggdrasilAuthenticationService.class);

    //ENVIRONMENT
    private static Class<?> classEnvironment = NMSUtils.getClassSilent("com.mojang.authlib.Environment");
    private static Field environment = NMSUtils.getFirstFieldOfTypeSilent(YggdrasilAuthenticationService.class, classEnvironment);
    private static Field baseAuthentificationService = NMSUtils.getFirstFieldOfTypeSilent(BaseMinecraftSessionService.class, AuthenticationService.class);


    public static void setUp(SpigotLoader spigotLoader) throws Exception {
        Object ms = getServer.invoke(null);


        Object oldSessionService;
        if(sessionService != null){
            oldSessionService = sessionService.get(ms);
        }else{
            oldSessionService = servicesSessionService.get(fieldServices.get(ms));
        }

        if(authentificationService == null){
            YggdrasilAuthenticationService current = (YggdrasilAuthenticationService)baseAuthentificationService.get(oldSessionService);
            Object service = new NMSAuthEnvironmentService(spigotLoader.getAOInstance(), oldSessionService, current, environment.get(current), spigotLoader.alwaysOnline.database);
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
        }else{
            Object service = new NMSAuthService(spigotLoader.getAOInstance(), oldSessionService, (YggdrasilAuthenticationService) authentificationService.get(ms), spigotLoader.alwaysOnline.database);
            sessionService.set(ms, service);
        }

    }

    public static void setOnlineMode(boolean onlineMode){
        if(servicesClass != null && setUsesAuthentication != null){
            try {
                Object ms = getServer.invoke(null);
                setUsesAuthentication.invoke(ms, onlineMode);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
