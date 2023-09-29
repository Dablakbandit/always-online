package me.dablakbandit.ao.databases;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class FileDatabase implements Database{
	
	private final ConcurrentHashMap<String, PlayerData>	cache	= new ConcurrentHashMap<>();
	
	private final Path									savedData;
	
	public FileDatabase(Path savedData){
		this.savedData = savedData;
	}
	
	public String getIP(String username){
		PlayerData playerData = this.cache.get(username);
		if(playerData != null){
			return playerData.ipAddress;
		}else{
			try{
				playerData = this.loadPlayerData(username);
			}catch(IOException e){
				e.printStackTrace();
			}
			if(playerData != null){
				this.cache.put(username, playerData);
				return playerData.ipAddress;
			}
		}
		return null;
		
	}
	
	public UUID getUUID(String username){
		PlayerData playerData = this.cache.get(username);
		if(playerData != null){
			return playerData.uuid;
		}else{
			try{
				playerData = this.loadPlayerData(username);
			}catch(IOException e){
				e.printStackTrace();
			}
			if(playerData != null){
				this.cache.put(username, playerData);
				return playerData.uuid;
			}
		}
		return null;
	}
	
	public void updatePlayer(String username, String ip, UUID uuid){
		this.cache.put(username, new PlayerData(ip, uuid));
		try{
			this.save();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void save() throws Exception{
		List<String> existingLines;
		if(Files.isReadable(this.savedData)){// File exists check is included
			existingLines = Files.readAllLines(this.savedData, StandardCharsets.UTF_8);
			ArrayList<String> toRemove = new ArrayList<>();
			for(String key : this.cache.keySet()){
				for(String line : existingLines){
					if(line.startsWith(key + "|")){
						toRemove.add(line);
					}
				}
			}
			existingLines.removeAll(toRemove);
			toRemove.clear();
		}else{
			existingLines = Collections.emptyList();
		}
		
		PrintWriter w = new PrintWriter(Files.newBufferedWriter(this.savedData, StandardCharsets.UTF_8));
		for(String line : existingLines){
			w.println(line);
		}
		
		for(Map.Entry<String, PlayerData> en : this.cache.entrySet()){
			w.println(en.getKey() + "|" + en.getValue().ipAddress + "|" + en.getValue().uuid.toString());
		}
		w.close();
	}
	
	@Override
	public void resetCache(){
		this.cache.clear();
	}
	
	@Override
	public void close(){
		/* Nothing to see here */
		this.cache.clear();
	}
	
	private PlayerData loadPlayerData(String username) throws IOException{
		if(Files.notExists(this.savedData))
			Files.createFile(this.savedData);
		BufferedReader br = Files.newBufferedReader(this.savedData, StandardCharsets.UTF_8);
		String l;
		String startWithKey = username + "|";
		PlayerData playerData = null;
		while((l = br.readLine()) != null){
			if(l.startsWith(startWithKey)){
				String[] data = l.split(Pattern.quote("|"));
				playerData = new PlayerData(data[1], UUID.fromString(data[2]));
				break;
			}
		}
		br.close();
		return playerData;
	}
	
}
