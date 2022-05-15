package me.johnnywoof.ao.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.johnnywoof.ao.NativeExecutor;
import me.johnnywoof.ao.databases.Database;
import me.johnnywoof.ao.databases.MySQLDatabase;
import me.johnnywoof.ao.hybrid.AlwaysOnline;
import me.johnnywoof.ao.velocity.metrics.Metrics;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

@Plugin(id = "alwaysonline", name = "Always Online", version = "${version}", url = "https://www.spigotmc.org/resources/alwaysonline.66591/", description = "Keep your server running while mojang is offline, Supports all server versions!", authors = "Dablakbandit")
public class VelocityLoader implements NativeExecutor {

    public final AlwaysOnline alwaysOnline = new AlwaysOnline(this);
    public final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public VelocityLoader(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    public Logger getLogger() {
        return logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.alwaysOnline.reload();
        CommandMeta meta = this.server.getCommandManager().metaBuilder("alwaysonline").aliases("ao").build();
        this.server.getCommandManager().register(meta, new VelocityCommand(this));

        Metrics metrics = metricsFactory.make(this, 15202);
        Database database = alwaysOnline.getDatabase();
        String databaseType = "FlatFile";
        if(database instanceof MySQLDatabase){
            databaseType = "MySQL";
        }
        String finalDatabaseType = databaseType;
        metrics.addCustomChart(new Metrics.SimplePie("database_type", () -> finalDatabaseType));
    }

    private AtomicInteger taskCounter = new AtomicInteger(0);
    private Map<Integer, ScheduledTask> scheduledTasks = new HashMap<>();

    @Override
    public int runAsyncRepeating(Runnable runnable, long delay, long period, TimeUnit timeUnit) {
        int taskId = taskCounter.getAndIncrement();
        ScheduledTask task = server.getScheduler().buildTask(this, () -> {
            scheduledTasks.remove(taskId);
            runnable.run();
        }).delay(delay, timeUnit).repeat(period, timeUnit).schedule();
        scheduledTasks.put(taskId, task);
        return taskId;
    }

    @Override
    public void cancelTask(int taskID) {
        ScheduledTask task = scheduledTasks.remove(taskID);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void cancelAllOurTasks() {
        for (Map.Entry<Integer, ScheduledTask> taskEntry : scheduledTasks.entrySet()) {
            taskEntry.getValue().cancel();
        }
        scheduledTasks.clear();
    }

    @Override
    public void unregisterAllListeners() {
        server.getEventManager().unregisterListeners(this);
    }

    @Override
    public void log(Level level, String message) {
        if(level == Level.WARNING){
            logger.warn(message);
        }else{
            logger.info(message);
        }
    }

    @Override
    public Path dataFolder() {
        return dataDirectory;
    }

    @Override
    public void disablePlugin() {

    }

    @Override
    public void registerListener() {
        server.getEventManager().register(this, new VelocityListener(this));
    }

    @Override
    public void broadcastMessage(String message) {
        this.server.sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    @Override
    public AlwaysOnline getAOInstance() {
        return alwaysOnline;
    }

    @Override
    public String getVersion() {
        return server.getPluginManager().getPlugin("alwaysonline").get().getDescription().getVersion().get();
    }
}
