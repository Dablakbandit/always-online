package me.johnnywoof.ao.databases;

import java.util.UUID;

public class PlayerData{
	
	public final String	ipAddress;
	public final UUID	uuid;
	
	public PlayerData(String ipAddress, UUID uuid){
		// Preconditions.checkNotNull(uuid);
		if(uuid == null)
			throw new NullPointerException("UUID provided is null.");
		this.uuid = uuid;
		this.ipAddress = ipAddress;
	}
	
	@Override
	public boolean equals(Object o){
		return o == this || (o instanceof PlayerData && ((PlayerData)o).uuid.equals(this.uuid));
	}
	
	@Override
	public int hashCode(){
		return this.uuid.hashCode();
	}
	
}
