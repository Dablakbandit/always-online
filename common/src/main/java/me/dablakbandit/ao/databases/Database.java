package me.dablakbandit.ao.databases;

import java.net.InetAddress;
import java.util.UUID;

public interface Database{
	
	String getIP(String username);
	
	UUID getUUID(String username);
	
	/**
	 * Caches the information and updates the database.
	 * Highly recommended to run async
	 *
	 * @param username The username
	 * @param ip       The IP
	 * @param uuid     The UUID
	 */
	void updatePlayer(String username, String ip, UUID uuid);
	
	/**
	 * Saves the data.
	 * Can be safely ran asynchronously
	 */
	void save() throws Exception;
	
	/**
	 * Resets the database cache. Mainly used for debugging.
	 */
	void resetCache();
	
	/**
	 * Closes the database connections/streams
	 * This method does not save any data
	 */
	void close();

	default boolean isValidIP(String ip) {
		try {
			InetAddress.getByName(ip);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
}
