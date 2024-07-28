package me.dablakbandit.ao.hybrid;

import me.dablakbandit.ao.NativeExecutor;
import me.dablakbandit.ao.databases.Database;

public interface IAlwaysOnline {
    boolean getOfflineMode();
    boolean isDebug();

    NativeExecutor getNativeExecutor();

    Database getDatabase();
}
