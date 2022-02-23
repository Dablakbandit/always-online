package me.johnnywoof.ao.velocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.GameProfile;
import me.johnnywoof.ao.hybrid.AlwaysOnline;
import me.johnnywoof.ao.proxy.ProxyListener;
import me.johnnywoof.ao.utils.NMSUtils;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.lang.reflect.Field;
import java.util.UUID;

public class VelocityListener extends ProxyListener {

    private final VelocityLoader velocityLoader;

    public VelocityListener(VelocityLoader velocityLoader) {
        super(velocityLoader);
        this.velocityLoader = velocityLoader;
    }

    // A high priority to allow other plugins to go first
    @Subscribe(order = PostOrder.LAST)
    public void onPreLogin(PreLoginEvent event) {
        if (AlwaysOnline.MOJANG_OFFLINE_MODE) {// Make sure we are in mojang offline mode
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
                    event.setResult(PreLoginEvent.PreLoginComponentResult.allowed());
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
        if (AlwaysOnline.MOJANG_OFFLINE_MODE && this.MOTD != null) {
            ServerPing sp = event.getPing();
            TextComponent component = LegacyComponentSerializer.legacy('&').deserialize(this.MOTD);
            ServerPing serverPing = sp.asBuilder().description(component).build();
            event.setPing(serverPing);
        }
    }

    @SuppressWarnings("deprecation")
    // Set priority to lowest since we'll be needing to go first
    @Subscribe(order = PostOrder.FIRST)
    public void onPost(PostLoginEvent event) {
        if (AlwaysOnline.MOJANG_OFFLINE_MODE) {
            Player player = event.getPlayer();
            try {
                UUID uuid = this.velocityLoader.getAOInstance().database.getUUID(event.getPlayer().getUsername());
                // Reflection

                Field gp = NMSUtils.getFirstFieldOfType(player.getClass(), GameProfile.class);
                GameProfile gameProfile = (GameProfile) gp.get(player);
                Field sf = NMSUtils.getFirstFieldOfType(GameProfile.class, UUID.class);
                sf.set(gameProfile, uuid);

            } catch (Exception e) {// Play it safe, if an error deny the player
                event.getPlayer().disconnect(LegacyComponentSerializer.legacy('&').deserialize("Sorry, the mojang servers are offline and we can't authenticate you with our own system!"));
                this.velocityLoader.getLogger().warn("Internal error for " + event.getPlayer().getUsername() + ", preventing login.");
                e.printStackTrace();
            }

        } else {
            // If we are not in mojang offline mode, update the player data
            final String username = event.getPlayer().getUsername();
            final String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
            final UUID uuid = event.getPlayer().getUniqueId();
            this.velocityLoader.server.getScheduler().buildTask(this.velocityLoader, new Runnable() {
                @Override
                public void run() {
                    VelocityListener.this.velocityLoader.getAOInstance().database.updatePlayer(username, ip, uuid);
                }
            }).schedule();
        }
    }
}
