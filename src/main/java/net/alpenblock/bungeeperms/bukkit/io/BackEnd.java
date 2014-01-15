package net.alpenblock.bungeeperms.bukkit.io;

import java.util.List;
import net.alpenblock.bungeeperms.bukkit.Group;
import net.alpenblock.bungeeperms.bukkit.User;

/**
 *
 * @author Alex
 */
public interface BackEnd
{
    public BackEndType getType();
    
    public void load();
    public List<Group> loadGroups();
    public List<User> loadUsers();
    public Group loadGroup(String group);
    public User loadUser(String user);
    public int loadVersion();
    
    public boolean isUserInDatabase(User user);

    public void reloadGroup(Group group);
    public void reloadUser(User user);


    
}
