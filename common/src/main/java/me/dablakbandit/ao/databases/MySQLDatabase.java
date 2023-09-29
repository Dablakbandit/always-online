package me.dablakbandit.ao.databases;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import me.dablakbandit.ao.NativeExecutor;

public class MySQLDatabase implements Database{
	
	private static final String							selectSQLStatement	= "SELECT uuid,ip FROM always_online WHERE name = ?";
	private static final String							insertSQLStatement	= "INSERT INTO always_online (name,ip,uuid) VALUES(?,?,?) ON DUPLICATE KEY UPDATE ip = VALUES(ip), uuid = VALUES(uuid)";
	
	private final String								host;
	private final int									port;
	private final String								database;
	private final String								username;
	private final String								password;
	private final String 								extra;
	
	private final ConcurrentHashMap<String, PlayerData>	cache				= new ConcurrentHashMap<>();
	
	private Statement									statement			= null;
	
	private final NativeExecutor nativeExecutor;
	private int											pingTaskID			= -1;
	
	public MySQLDatabase(NativeExecutor nativeExecutor, String host, int port, String database, String username, String password, String extra) throws SQLException{
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		this.extra = extra;
		this.nativeExecutor = nativeExecutor;
		this.connect();
	}
	
	public void pingDatabase(){
		if(this.statement != null){// Is alive check
			try{
				// Ping the database.
				if(!this.statement.getConnection().isValid(30)){// 30 second timeout
					// Auto-reconnect
					this.connect();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
	}
	
	private void connect() throws SQLException{
		this.close();// Close existing database connections, if one exists.
		this.statement = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + this.extra, this.username, this.password).createStatement();
		if(!this.doesTableExist("always_online")){
			this.statement.executeUpdate("CREATE TABLE `always_online` ( `name` CHAR(16) NOT NULL , `ip` CHAR(15) NOT NULL , `uuid` CHAR(36) NOT NULL , PRIMARY KEY (`name`)) ENGINE = MyISAM; ");
		}
		
		// Manual keep-alive task. Should work with all drivers.
		this.pingTaskID = this.nativeExecutor.runAsyncRepeating(new Runnable(){
			@Override
			public void run(){
				MySQLDatabase.this.pingDatabase();
			}
		}, 0, 1, TimeUnit.MINUTES);// 1 minute
	}
	
	@Override
	public String getIP(String username){
		PlayerData playerData = this.cache.get(username);
		if(playerData != null){
			return playerData.ipAddress;
		}else{
			if(this.loadDataFromSQL(username)){ return this.cache.get(username).ipAddress; }
		}
		return null;
	}
	
	private boolean loadDataFromSQL(String username){
		if(this.statement != null){
			PlayerData playerData = null;
			try{
				PreparedStatement preparedStatement = this.statement.getConnection().prepareStatement(selectSQLStatement);
				preparedStatement.setString(1, username);
				
				ResultSet rs = preparedStatement.executeQuery();
				if(rs.next()){
					playerData = new PlayerData(rs.getString(2), UUID.fromString(rs.getString(1)));
				}
				rs.close();
				preparedStatement.close();
			}catch(SQLException e){
				e.printStackTrace();
			}
			if(playerData != null){
				this.cache.put(username, playerData);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public UUID getUUID(String username){
		PlayerData playerData = this.cache.get(username);
		if(playerData != null){
			return playerData.uuid;
		}else{
			if(this.loadDataFromSQL(username)){ return this.cache.get(username).uuid; }
		}
		return null;
	}
	
	@Override
	public void updatePlayer(String username, String ip, UUID uuid){
		this.cache.put(username, new PlayerData(ip, uuid));
		if(this.statement != null){
			try{
				Connection connection = this.statement.getConnection();
				if(connection != null){
					PreparedStatement preparedStatement = connection.prepareStatement(insertSQLStatement);
					preparedStatement.setString(1, username);
					preparedStatement.setString(2, ip);
					preparedStatement.setString(3, uuid.toString());
					preparedStatement.execute();
					preparedStatement.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void save() throws Exception{
		if(this.statement != null){
			Connection connection = this.statement.getConnection();
			if(connection != null){
				PreparedStatement preparedStatement = connection.prepareStatement(insertSQLStatement);
				int i = 0;
				for(Map.Entry<String, PlayerData> en : this.cache.entrySet()){
					preparedStatement.setString(1, en.getKey());
					preparedStatement.setString(2, en.getValue().ipAddress);
					preparedStatement.setString(3, en.getValue().uuid.toString());
					preparedStatement.addBatch();
					i++;
					if(i == 1000 || i == this.cache.size()){
						preparedStatement.executeBatch(); // Execute every 1000 items or when full.
						i = 0;
					}
				}
				preparedStatement.close();
			}
		}
	}
	
	@Override
	public void resetCache(){
		this.cache.clear();
	}
	
	@Override
	public void close(){
		if(this.pingTaskID != -1){
			this.nativeExecutor.cancelTask(this.pingTaskID);
			this.pingTaskID = -1;
		}
		if(this.statement != null){
			try{
				this.statement.close();
			}catch(SQLException e){
				/* Non-critical error */
			}
			this.statement = null;
			this.cache.clear();
		}
	}
	
	/**
	 * Determines if a table exists
	 *
	 * @param tableName The table name
	 * @return If the table exists
	 */
	private boolean doesTableExist(String tableName) throws SQLException{
		ResultSet rs = this.statement.getConnection().getMetaData().getTables(null, null, tableName, null);
		if(rs.next()){
			rs.close();
			return true;
		}
		rs.close();
		return false;
	}
	
}
