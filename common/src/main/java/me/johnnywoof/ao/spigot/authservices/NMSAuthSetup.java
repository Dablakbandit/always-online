package me.johnnywoof.ao.spigot.authservices;

import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.minecraft.BaseMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import me.johnnywoof.ao.spigot.SpigotLoader;
import me.johnnywoof.ao.utils.NMSUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NMSAuthSetup {

    private static Class<?> classMinecraftServer = NMSUtils.getNMSClass("MinecraftServer");
    private static Method getServer = NMSUtils.getMethod(classMinecraftServer, "getServer");
    private static Field sessionService = NMSUtils.getFirstFieldOfType(classMinecraftServer, MinecraftSessionService.class);

    //CLASSIC < 1.16
    private static Field authentificationService = NMSUtils.getFirstFieldOfTypeSilent(classMinecraftServer, YggdrasilAuthenticationService.class);

    //ENVIRONMENT
    private static Class<?> classEnvironment = NMSUtils.getClassSilent("com.mojang.authlib.Environment");
    private static Field environment = NMSUtils.getFirstFieldOfTypeSilent(YggdrasilAuthenticationService.class, classEnvironment);
    private static Field baseAuthentificationService = NMSUtils.getFirstFieldOfTypeSilent(BaseMinecraftSessionService.class, AuthenticationService.class);


    public static void setUp(SpigotLoader spigotLoader) throws Exception {
        Object ms = getServer.invoke(null);
        YggdrasilMinecraftSessionService service = null;
        if(authentificationService == null){
            MinecraftSessionService session = (MinecraftSessionService) sessionService.get(ms);
            YggdrasilAuthenticationService current = (YggdrasilAuthenticationService)baseAuthentificationService.get(session);
            service = new NMSAuthEnvironmentService(current, environment.get(current), spigotLoader.alwaysOnline.database);
        }else{
            service = new NMSAuthService((YggdrasilAuthenticationService) authentificationService.get(ms), spigotLoader.alwaysOnline.database);
        }
        sessionService.set(ms, service);
    }
}
