package me.dablakbandit.ao.proxy;

import me.dablakbandit.ao.NativeExecutor;

import java.util.regex.Pattern;

public abstract class ProxyListener {

    protected final Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_-]{3,16}$");	// The regex to verify usernames;

    protected String MOTD;

    protected NativeExecutor executor;

    public ProxyListener(NativeExecutor executor) {
        this.executor = executor;
    }


    /**
     * Validate username with regular expression
     *
     * @param username username for validation
     * @return true valid username, false invalid username
     */
    protected boolean validate(String username){
        return username != null && usernamePattern.matcher(username).matches();
    }
}
