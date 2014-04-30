package net.alpenblock.bungeeperms.bukkit.io;

import java.util.List;
import java.util.UUID;
import net.alpenblock.bungeeperms.bukkit.Group;
import net.alpenblock.bungeeperms.bukkit.User;

public interface BackEnd
{
    public BackEndType getType();
    
    public void load();
    public List<Group> loadGroups();
    public List<User> loadUsers();
    public Group loadGroup(String group);
    public User loadUser(String user);
    public User loadUser(UUID user);
    public int loadVersion();
    
    public boolean isUserInDatabase(User user);
    
    public void reloadGroup(Group group);
    public void reloadUser(User user);
}
