package net.alpenblock.bungeeperms.bukkit.io;

import java.util.UUID;

public interface UUIDPlayerDB 
{
    public UUID getUUID(String player);
    public String getPlayerName(UUID uuid);
    public void update(UUID uuid, String player);
}
