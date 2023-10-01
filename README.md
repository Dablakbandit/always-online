# always-online
# Overview
Hate mojang servers being offline? Same with me. So to resolve this issue, I have created AlwaysOnline. A plugin that keeps your bukkit, spigot, paper or bungeecord server online while mojang is offline.
# How it works
The plugin is set to a repeating task with a configurable delay, to see if the session servers are offline. If they are, the plugin will go into mojang offline mode. When in this mode, the server will allow players to login if their ip matches from their last authenticated login. If they don't match, it denies them from logging in. The plugin will also deny new players from joining if the mojang authentication servers are down.

Works with: **Bungeecord, Velocity, Spigot, Paper, Bukkit**

https://www.spigotmc.org/resources/alwaysonline.66591/
