package me.dablakbandit.ao.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.dablakbandit.ao.NativeExecutor;
import me.dablakbandit.ao.databases.MySQLDatabase;
import me.dablakbandit.ao.velocity.metrics.Metrics;
import me.dablakbandit.ao.databases.Database;
import me.dablakbandit.ao.hybrid.AlwaysOnline;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

@Plugin(id = "alwaysonline", name = "Always Online", version = "6.2.7", url = "https://www.spigotmc.org/resources/alwaysonline.66591/", description = "Keep your server running while mojang is offline, Supports all server versions!", authors = "Dablakbandit")
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
        loadLibs();
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

    private void loadLibs() {
        File libsFolder = new File(this.dataFolder().toFile(), "/libs/");
        if (!libsFolder.exists()) {
            libsFolder.mkdirs();
        }
        for (File file : libsFolder.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                logger.info(file.getName());
                server.getPluginManager().addToClasspath(this, file.toPath());
            }
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception | Error ignored){
        }
    }

    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final Map<Integer, ScheduledTask> scheduledTasks = new HashMap<>();

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

    @Override
    public void notifyOfflineMode(boolean offlineMode) {

    }

    @Override
    public void initMySQL() {
        loadLibs();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return;
        } catch (Exception | Error ignored){
        }

        String url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar";

        File libsFolder = new File(this.dataFolder().toFile(), "/libs/");
        File libFile = new File(libsFolder, "mysql-connector-j-8.0.33.jar");
        try {

            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(false);
            httpURLConnection.connect();

            int repCode = httpURLConnection.getResponseCode();

            if (repCode == 200) {
                try (InputStream inputStream = httpURLConnection.getInputStream(); FileOutputStream fileOutputStream = new FileOutputStream(libFile)) {
                    byte[] b = new byte[1024];
                    int n;
                    while ((n = inputStream.read(b)) != -1) {
                        fileOutputStream.write(b, 0, n);
                    }
                    fileOutputStream.flush();
                }
                if (!sha1(libFile).equals("9e64d997873abc4318620264703d3fdb6b02dd5a")) {
                    libFile.delete();
                    getLogger().error("Failed to download mysql driver");
                }else{
                    logger.info(libFile.getName());
                    server.getPluginManager().addToClasspath(this, libFile.toPath());
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String sha1(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();) {
            byte[] buff = new byte[1024];
            int n;
            while ((n = fis.read(buff)) > 0) {
                arrayOutputStream.write(buff, 0, n);
            }
            final byte[] digest = MessageDigest.getInstance("SHA-1").digest(arrayOutputStream.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (byte aByte : digest) {
                String temp = Integer.toHexString((aByte & 0xFF));
                if (temp.length() == 1) {
                    sb.append("0");
                }
                sb.append(temp);
            }
            return sb.toString();
        }
    }
}
