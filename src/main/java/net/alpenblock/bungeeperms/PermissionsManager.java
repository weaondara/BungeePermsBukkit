package net.alpenblock.bungeeperms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.alpenblock.bungeeperms.io.BackEnd;
import net.alpenblock.bungeeperms.io.BackEndType;
import net.alpenblock.bungeeperms.io.MySQL2BackEnd;
import net.alpenblock.bungeeperms.io.MySQLBackEnd;
import net.alpenblock.bungeeperms.io.MySQLUUIDPlayerDB;
import net.alpenblock.bungeeperms.io.NoneUUIDPlayerDB;
import net.alpenblock.bungeeperms.io.UUIDPlayerDB;
import net.alpenblock.bungeeperms.io.UUIDPlayerDBType;
import net.alpenblock.bungeeperms.io.YAMLBackEnd;
import net.alpenblock.bungeeperms.io.YAMLUUIDPlayerDB;
import net.alpenblock.bungeeperms.platform.PlatformPlugin;
import net.alpenblock.bungeeperms.platform.Sender;

public class PermissionsManager
{

    private final PlatformPlugin plugin;
    private final BPConfig config;
    private final Debug debug;
    private boolean enabled = false;

    @Getter
    @Setter
    private BackEnd backEnd;
    @Getter
    private UUIDPlayerDB UUIDPlayerDB;

    private List<Group> groups;
    private List<User> users;
    private int permsversion;

    public PermissionsManager(PlatformPlugin p, BPConfig conf, Debug d)
    {
        plugin = p;
        config = conf;
        debug = d;

        //config
        loadConfig();

        //perms
        loadPerms();
    }

    public final void loadConfig()
    {
        BackEndType bet = config.getBackEndType();
        if (bet == BackEndType.YAML)
        {
            backEnd = new YAMLBackEnd();
        }
        else if (bet == BackEndType.MySQL)
        {
            backEnd = new MySQLBackEnd();
        }
        else if (bet == BackEndType.MySQL2)
        {
            backEnd = new MySQL2BackEnd();
        }

        UUIDPlayerDBType updbt = config.getUUIDPlayerDBType();
        if (updbt == UUIDPlayerDBType.None)
        {
            UUIDPlayerDB = new NoneUUIDPlayerDB();
        }
        else if (updbt == UUIDPlayerDBType.YAML)
        {
            UUIDPlayerDB = new YAMLUUIDPlayerDB();
        }
        else if (updbt == UUIDPlayerDBType.MySQL)
        {
            UUIDPlayerDB = new MySQLUUIDPlayerDB();
        }
    }

    public final void loadPerms()
    {
        BungeePerms.getLogger().info("loading permissions ...");

        //load database
        backEnd.load();

        //load all groups
        groups = backEnd.loadGroups();

        users = new ArrayList<>();

        //load permsversion
        permsversion = backEnd.loadVersion();

        BungeePerms.getLogger().info("permissions loaded");
    }

    public void enable()
    {
        if (enabled)
        {
            return;
        }

        //load online players; allows reload
        for (Sender s : BungeePerms.getInstance().getPlugin().getPlayers())
        {
            if (config.isUseUUIDs())
            {
                getUser(s.getUUID());
            }
            else
            {
                getUser(s.getName());
            }
        }

        enabled = true;
    }

    public void disable()
    {
        if (!enabled)
        {
            return;
        }
        enabled = false;
    }

    public void reload()
    {
        disable();

        //config
        loadConfig();

        //perms
        loadPerms();

        enable();
    }

    public synchronized void validateUsersGroups()
    {
        //group check - remove inheritances
        for (int i = 0; i < groups.size(); i++)
        {
            Group group = groups.get(i);
            List<String> inheritances = group.getInheritances();
            for (int j = 0; j < inheritances.size(); j++)
            {
                if (getGroup(inheritances.get(j)) == null)
                {
                    inheritances.remove(j);
                    j--;
                }
            }
            backEnd.saveGroupInheritances(group);
        }
        //perms recalc and bukkit perms update
        for (Group g : groups)
        {
            g.recalcPerms();

            //send bukkit update info
            BungeePerms.getInstance().getNetworkNotifier().reloadGroup(g);
        }

        //user check
        for (int i = 0; i < users.size(); i++)
        {
            User u = users.get(i);
            for (int j = 0; j < u.getGroups().size(); j++)
            {
                if (getGroup(u.getGroups().get(j).getName()) == null)
                {
                    u.getGroups().remove(j);
                    j--;
                }
            }
            backEnd.saveUserGroups(u);
        }

        //perms recalc and bukkit perms update
        for (User u : users)
        {
            u.recalcPerms();

            //send bukkit update info
            BungeePerms.getInstance().getNetworkNotifier().reloadUser(u);
        }

        //user groups check - backEnd
        List<User> backendusers = backEnd.loadUsers();
        for (int i = 0; i < backendusers.size(); i++)
        {
            User u = backendusers.get(i);
            for (int j = 0; j < u.getGroups().size(); j++)
            {
                if (getGroup(u.getGroups().get(j).getName()) == null)
                {
                    u.getGroups().remove(j);
                    j--;
                }
            }
            backEnd.saveUserGroups(u);
        }
    }

    public synchronized Group getMainGroup(User player)
    {
        if (player == null)
        {
            throw new NullPointerException("player is null");
        }
        if (player.getGroups().isEmpty())
        {
            return null;
        }
        Group ret = player.getGroups().get(0);
        for (int i = 1; i < player.getGroups().size(); i++)
        {
            if (player.getGroups().get(i).getWeight() < ret.getWeight())
            {
                ret = player.getGroups().get(i);
            }
        }
        return ret;
    }

    public synchronized Group getNextGroup(Group group)
    {
        List<Group> laddergroups = getLadderGroups(group.getLadder());

        for (int i = 0; i < laddergroups.size(); i++)
        {
            if (laddergroups.get(i).getRank() == group.getRank())
            {
                if (i + 1 < laddergroups.size())
                {
                    return laddergroups.get(i + 1);
                }
                else
                {
                    return null;
                }
            }
        }
        throw new IllegalArgumentException("group ladder does not exist (anymore)");
    }

    public synchronized Group getPreviousGroup(Group group)
    {
        List<Group> laddergroups = getLadderGroups(group.getLadder());

        for (int i = 0; i < laddergroups.size(); i++)
        {
            if (laddergroups.get(i).getRank() == group.getRank())
            {
                if (i > 0)
                {
                    return laddergroups.get(i - 1);
                }
                else
                {
                    return null;
                }
            }
        }
        throw new IllegalArgumentException("group ladder does not exist (anymore)");
    }

    public synchronized List<Group> getLadderGroups(String ladder)
    {
        List<Group> ret = new ArrayList<>();

        for (Group g : groups)
        {
            if (g.getLadder().equalsIgnoreCase(ladder))
            {
                ret.add(g);
            }
        }

        Collections.sort(ret);

        return ret;
    }

    public synchronized List<String> getLadders()
    {
        List<String> ret = new ArrayList<>();

        for (Group g : groups)
        {
            if (!ret.contains(g.getLadder()))
            {
                ret.add(g.getLadder());
            }
        }

        return ret;
    }

    public synchronized List<Group> getDefaultGroups()
    {
        List<Group> ret = new ArrayList<>();
        for (Group g : groups)
        {
            if (g.isDefault())
            {
                ret.add(g);
            }
        }
        return ret;
    }

    public synchronized Group getGroup(String groupname)
    {
        for (Group g : groups)
        {
            if (g.getName().equalsIgnoreCase(groupname))
            {
                return g;
            }
        }
        return null;
    }

    public synchronized User getUser(String usernameoruuid)
    {
        UUID uuid = Statics.parseUUID(usernameoruuid);
        if (config.isUseUUIDs())
        {
            if (uuid != null)
            {
                return getUser(uuid);
            }
        }

        for (User u : users)
        {
            if (u.getName().equalsIgnoreCase(usernameoruuid))
            {
                return u;
            }
        }

        //load user from database
        User u = null;
        if (config.isUseUUIDs())
        {
            if (uuid == null)
            {
                uuid = UUIDPlayerDB.getUUID(usernameoruuid);
            }
            if (uuid != null)
            {
                u = backEnd.loadUser(uuid);
            }
        }
        else
        {
            u = backEnd.loadUser(usernameoruuid);
        }
        if (u != null)
        {
            users.add(u);
            return u;
        }

        return null;
    }

    public synchronized User getUser(UUID uuid)
    {
        for (User u : users)
        {
            if (u.getUUID() != null && u.getUUID().equals(uuid))
            {
                return u;
            }
        }

        //load user from database
        User u = backEnd.loadUser(uuid);
        if (u != null)
        {
            users.add(u);
            return u;
        }

        return null;
    }

    public List<Group> getGroups()
    {
        return Collections.unmodifiableList(groups);
    }

    public List<User> getUsers()
    {
        return Collections.unmodifiableList(users);
    }

//internal functions    
    public void reloadUser(String user)
    {
        User u = getUser(user);
        if (u == null)
        {
            debug.log("User " + user + " not found!!!");
            return;
        }
        backEnd.reloadUser(u);
        u.recalcPerms();
    }

    public void reloadUser(UUID uuid)
    {
        User u = getUser(uuid);
        if (u == null)
        {
            debug.log("User " + uuid + " not found!!!");
            return;
        }
        backEnd.reloadUser(u);
        u.recalcPerms();
    }

    public void reloadGroup(String group)
    {
        Group g = getGroup(group);
        if (g == null)
        {
            debug.log("Group " + group + " not found!!!");
            return;
        }
        backEnd.reloadGroup(g);
        Collections.sort(groups);
        for (Group gr : groups)
        {
            gr.recalcPerms();
        }
        for (User u : users)
        {
            u.recalcPerms();
        }
    }

    public void reloadUsers()
    {
        for (User u : users)
        {
            backEnd.reloadUser(u);
            u.recalcPerms();
        }
    }

    public void reloadGroups()
    {
        for (Group g : groups)
        {
            backEnd.reloadGroup(g);
        }
        Collections.sort(groups);
        for (Group g : groups)
        {
            g.recalcPerms();
        }
        for (User u : users)
        {
            u.recalcPerms();
        }
    }

    public void addUserToCache(User u)
    {
        users.add(u);
    }

    public void removeUserFromCache(User u)
    {
        users.remove(u);
    }

    public void addGroupToCache(Group g)
    {
        groups.add(g);
    }

    public void removeGroupFromCache(Group g)
    {
        groups.remove(g);
    }
}
