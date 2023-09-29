package me.dablakbandit.ao.velocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.GameProfile;
import me.dablakbandit.ao.proxy.ProxyListener;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VelocityListener extends ProxyListener {

    private final VelocityLoader velocityLoader;

    public VelocityListener(VelocityLoader velocityLoader) {
        super(velocityLoader);
        this.velocityLoader = velocityLoader;
    }

    // A high priority to allow other plugins to go first
    @Subscribe(order = PostOrder.LAST)
    public void onPreLogin(PreLoginEvent event) {
        if (velocityLoader.getAOInstance().getOfflineMode()) {// Make sure we are in mojang offline mode
            // Verify if the name attempting to connect is even verified
            if (!this.validate(event.getUsername())) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacy('&').deserialize(this.velocityLoader.alwaysOnline.config.getProperty("message-kick-invalid", "Invalid username. Hacking?"))));
                return;

            }
            // Get the connecting ip
            final String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
            // Get last known ip
            final String lastip = this.velocityLoader.alwaysOnline.database.getIP(event.getUsername());


            if (lastip == null) {// If null the player connecting is new
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacy('&').deserialize(this.velocityLoader.alwaysOnline.config.getProperty("message-kick-new", "We can not let you join because the mojang servers are offline!"))));
                this.velocityLoader.getLogger().info("Denied " + event.getUsername() + " from logging in cause their ip [" + ip + "] has never connected to this server before!");
            } else {
                if (ip.equals(lastip)) {// If it matches set handler to offline mode, so it does not authenticate player with mojang
                    this.velocityLoader.getLogger().info("Skipping session login for player " + event.getUsername() + " [Connected ip: " + ip + ", Last ip: " + lastip + "]!");
                    event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                    //handler.setOnlineMode(false);
                } else {// Deny the player from joining
                    this.velocityLoader.getLogger().info("Denied " + event.getUsername() + " from logging in cause their ip [" + ip + "] does not match their last ip!");
                    //handler.setOnlineMode(true);
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacy('&').deserialize(this.velocityLoader.alwaysOnline.config.getProperty("message-kick-ip", "We can not let you join since you are not on the same computer you logged on before!"))));
                }
            }
        }
    }

    // Set priority to highest to almost guaranteed to have our MOTD displayed
    @Subscribe(order = PostOrder.LAST)
    public void onPing(ProxyPingEvent event) {
        if (velocityLoader.getAOInstance().getOfflineMode() && this.MOTD != null) {
            ServerPing sp = event.getPing();
            TextComponent component = LegacyComponentSerializer.legacy('&').deserialize(this.MOTD);
            ServerPing serverPing = sp.asBuilder().description(component).build();
            event.setPing(serverPing);
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        if (velocityLoader.getAOInstance().getOfflineMode()) {
            try {
                GameProfile current = event.getGameProfile();
                UUID uuid = this.velocityLoader.getAOInstance().database.getUUID(current.getName());

                event.setGameProfile(new GameProfile(uuid, current.getName(), current.getProperties()));
            } catch (Exception e) {
                this.velocityLoader.getLogger().warn("Internal error for " + event.getGameProfile().getName() + "");
                e.printStackTrace();
            }
        } else {
            // If we are not in mojang offline mode, update the player data
            this.velocityLoader.server.getScheduler().buildTask(this.velocityLoader, new Runnable() {
                @Override
                public void run() {
                    if(event.getConnection().isActive()) {
                        String username = event.getUsername();
                        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
                        UUID uuid = event.getGameProfile().getId();
                        VelocityListener.this.velocityLoader.getAOInstance().database.updatePlayer(username, ip, uuid);
                    }
                }
            }).delay(1, TimeUnit.SECONDS).schedule();
        }
    }

}
