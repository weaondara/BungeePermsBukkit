package net.alpenblock.bungeeperms.bukkit.io;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.alpenblock.bungeeperms.bukkit.BungeePerms;
import net.alpenblock.bungeeperms.bukkit.Config;
import net.alpenblock.bungeeperms.bukkit.Debug;
import net.alpenblock.bungeeperms.bukkit.Group;
import net.alpenblock.bungeeperms.bukkit.Mysql;
import net.alpenblock.bungeeperms.bukkit.MysqlConfig;
import net.alpenblock.bungeeperms.bukkit.PermissionsManager;
import net.alpenblock.bungeeperms.bukkit.Server;
import net.alpenblock.bungeeperms.bukkit.User;
import net.alpenblock.bungeeperms.bukkit.World;
import org.bukkit.plugin.Plugin;

public class MySQLBackEnd implements BackEnd
{
    private org.bukkit.Server server;
    private Config config;
    private Debug debug;
    private Plugin plugin;
    private Mysql mysql;
    
    private MysqlConfig permsconf;
    private String table;
    private String tablePrefix;
    
    public MySQLBackEnd(org.bukkit.Server server, Config conf, Debug d)
    {
        this.server=server;
        config=conf;
        debug=d;
        
        loadConfig();
        
        mysql=new Mysql(conf,d,"bungeeperms");
        mysql.connect();
        
        table=tablePrefix+"permissions";
        
        permsconf=new MysqlConfig(mysql,table);
        permsconf.createTable();
    }
    private void loadConfig()
    {
        tablePrefix=config.getString("tablePrefix", "bungeeperms_");
    }
    
    @Override
    public BackEndType getType()
    {
        return BackEndType.MySQL;
    }
    
    @Override
    public void load()
    {
		//load from table
		permsconf.load();
    }
    @Override
    public List<Group> loadGroups()
    {
        List<Group> ret=new ArrayList<>();
        
        List<String> groups=permsconf.getSubNodes("groups");
		for(String g:groups)
		{
			ret.add(loadGroup(g));
		}
        Collections.sort(ret);
        
        return ret;
    }
    @Override
    public List<User> loadUsers()
    {
        List<User> ret=new ArrayList<>();
        
        List<String> users=permsconf.getSubNodes("users");
		for(String u:users)
		{
			ret.add(loadUser(u));
		}
        
        return ret;
    }
    @Override
    public Group loadGroup(String group)
    {
        MysqlConfig permsconf=new MysqlConfig(mysql,table);
        
        ResultSet res=mysql.returnQuery("SELECT `key`,`value` FROM `"+table+"` WHERE `key` LIKE 'groups."+group+"%' ORDER BY id ASC");
        
        permsconf.fromResult(res);
        
        if(!permsconf.keyExists("groups."+group))
        {
            return null;
        }
        
        List<String> inheritances=permsconf.getListString("groups."+group+".inheritances", new ArrayList<String>());
        List<String> permissions=permsconf.getListString("groups."+group+".permissions", new ArrayList<String>());
        boolean isdefault=permsconf.getBoolean("groups."+group+".default",false);
        int rank=permsconf.getInt("groups."+group+".rank", 1000);
        int weight=permsconf.getInt("groups."+group+".weight", 1000);
        String ladder=permsconf.getString("groups."+group+".ladder", "default");
        String display=permsconf.getString("groups."+group+".display", "");
        String prefix=permsconf.getString("groups."+group+".prefix", "");
        String suffix=permsconf.getString("groups."+group+".suffix", "");

        //per server perms
        Map<String,Server> servers=new HashMap<>();
        for(String server:permsconf.getSubNodes("groups."+group+".servers"))
        {
            List<String> serverperms=permsconf.getListString("groups."+group+".servers."+server+".permissions", new ArrayList<String>());
            String sdisplay=permsconf.getString("groups."+group+".servers."+server+".display", "");
            String sprefix=permsconf.getString("groups."+group+".servers."+server+".prefix", "");
            String ssuffix=permsconf.getString("groups."+group+".servers."+server+".suffix", "");

            //per server world perms
            Map<String,World> worlds=new HashMap<>();
            for(String world:permsconf.getSubNodes("groups."+group+".servers."+server+".worlds"))
            {
                List<String> worldperms=permsconf.getListString("groups."+group+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>());
                String wdisplay=permsconf.getString("groups."+group+".servers."+server+".worlds."+world+".display", "");
                String wprefix=permsconf.getString("groups."+group+".servers."+server+".worlds."+world+".prefix", "");
                String wsuffix=permsconf.getString("groups."+group+".servers."+server+".worlds."+world+".suffix", "");

                World w=new World(world,worldperms,wdisplay,wprefix,wsuffix);
                worlds.put(world, w);
            }

            servers.put(server, new Server(server,serverperms,worlds,sdisplay,sprefix,ssuffix));
        }

        Group g=new Group(group, inheritances, permissions, servers, rank, weight, ladder, isdefault, display, prefix, suffix);
        
        return g;
    }
    @Override
    public User loadUser(String user)
    {
        MysqlConfig permsconf=new MysqlConfig(mysql,table);
        
        ResultSet res=mysql.returnQuery("SELECT `key`,`value` FROM `"+table+"` WHERE `key` LIKE 'users."+user+"%' ORDER BY id ASC");
        
        permsconf.fromResult(res);
        
        if(!permsconf.keyExists("users."+user))
        {
            return null;
        }
        
        //load user from database
        List<String> sgroups=permsconf.getListString("users."+user+".groups", new ArrayList<String>());
        List<Group> lgroups=new ArrayList<>();
        for(String s:sgroups)
        {
            Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
            if(g!=null)
            {
                lgroups.add(g);
            }
        }
        List<String> extrapermissions=permsconf.getListString("users."+user+".permissions", new ArrayList<String>());

        Map<String,List<String>> serverperms=new HashMap<>();
        Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
        for(String server:permsconf.getSubNodes("users."+user+".servers"))
        {
            //per server perms
            serverperms.put(server, permsconf.getListString("users."+user+".servers."+server+".permissions", new ArrayList<String>()));

            //per server world perms
            Map<String,List<String>> worldperms=new HashMap<>();
            for(String world:permsconf.getSubNodes("users."+user+".servers."+server+".worlds"))
            {
                worldperms.put(world, permsconf.getListString("users."+user+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>()));
            }
            serverworldperms.put(server, worldperms);
        }

        UUID uuid=BungeePerms.getInstance().getPermissionsManager().getUUIDPlayerDB().getUUID(user);
        User u=new User(user, uuid, lgroups, extrapermissions, serverperms,serverworldperms);
        return u;
    }
    @Override
    public User loadUser(UUID user)
    {
        MysqlConfig permsconf=new MysqlConfig(mysql,table);
        
        ResultSet res=mysql.returnQuery("SELECT `key`,`value` FROM `"+table+"` WHERE `key` LIKE 'users."+user+"%' ORDER BY id ASC");
        
        permsconf.fromResult(res);
        
        if(!permsconf.keyExists("users."+user))
        {
            return null;
        }
        
        //load user from database
        List<String> sgroups=permsconf.getListString("users."+user+".groups", new ArrayList<String>());
        List<Group> lgroups=new ArrayList<>();
        for(String s:sgroups)
        {
            Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
            if(g!=null)
            {
                lgroups.add(g);
            }
        }
        List<String> extrapermissions=permsconf.getListString("users."+user+".permissions", new ArrayList<String>());

        Map<String,List<String>> serverperms=new HashMap<>();
        Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
        for(String server:permsconf.getSubNodes("users."+user+".servers"))
        {
            //per server perms
            serverperms.put(server, permsconf.getListString("users."+user+".servers."+server+".permissions", new ArrayList<String>()));

            //per server world perms
            Map<String,List<String>> worldperms=new HashMap<>();
            for(String world:permsconf.getSubNodes("users."+user+".servers."+server+".worlds"))
            {
                worldperms.put(world, permsconf.getListString("users."+user+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>()));
            }
            serverworldperms.put(server, worldperms);
        }

        String username=BungeePerms.getInstance().getPermissionsManager().getUUIDPlayerDB().getPlayerName(user);
        User u=new User(username, user, lgroups, extrapermissions, serverperms,serverworldperms);
        return u;
    }
    @Override
    public int loadVersion()
    {
         return permsconf.getInt("version", 1);
    }

    @Override
    public boolean isUserInDatabase(User user)
    {
        return permsconf.keyExists("users."+user.getName());
    }

    @Override
    public void reloadGroup(Group group)
    {
        MysqlConfig permsconf=new MysqlConfig(mysql,table);
        
        ResultSet res=mysql.returnQuery("SELECT `key`,`value` FROM `"+table+"` WHERE `key` LIKE 'groups."+group.getName()+"%' ORDER BY id ASC");
        
        permsconf.fromResult(res);
        
        try 
        {
            res.close();
        } catch (Exception ex) { }
        
        //load group from database
        List<String> inheritances=permsconf.getListString("groups."+group.getName()+".inheritances", new ArrayList<String>());
        List<String> permissions=permsconf.getListString("groups."+group.getName()+".permissions", new ArrayList<String>());
        boolean isdefault=permsconf.getBoolean("groups."+group.getName()+".default",false);
        int rank=permsconf.getInt("groups."+group.getName()+".rank", 1000);
        int weight=permsconf.getInt("groups."+group.getName()+".weight", 1000);
        String ladder=permsconf.getString("groups."+group.getName()+".ladder", "default");
        String display=permsconf.getString("groups."+group.getName()+".display", "");
        String prefix=permsconf.getString("groups."+group.getName()+".prefix", "");
        String suffix=permsconf.getString("groups."+group.getName()+".suffix", "");

        //per server perms
        Map<String,Server> servers=new HashMap<>();
        for(String server:permsconf.getSubNodes("groups."+group.getName()+".servers"))
        {
            List<String> serverperms=permsconf.getListString("groups."+group.getName()+".servers."+server+".permissions", new ArrayList<String>());
            String sdisplay=permsconf.getString("groups."+group.getName()+".servers."+server+".display", "");
            String sprefix=permsconf.getString("groups."+group.getName()+".servers."+server+".prefix", "");
            String ssuffix=permsconf.getString("groups."+group.getName()+".servers."+server+".suffix", "");

            //per server world perms
            Map<String,World> worlds=new HashMap<>();
            for(String world:permsconf.getSubNodes("groups."+group.getName()+".servers."+server+".worlds"))
            {
                List<String> worldperms=permsconf.getListString("groups."+group.getName()+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>());
                String wdisplay=permsconf.getString("groups."+group.getName()+".servers."+server+".worlds."+world+".display", "");
                String wprefix=permsconf.getString("groups."+group.getName()+".servers."+server+".worlds."+world+".prefix", "");
                String wsuffix=permsconf.getString("groups."+group.getName()+".servers."+server+".worlds."+world+".suffix", "");

                World w=new World(world,worldperms,wdisplay,wprefix,wsuffix);
                worlds.put(world, w);
            }

            servers.put(server, new Server(server,serverperms,worlds,sdisplay,sprefix,ssuffix));
        }
        
        group.setInheritances(inheritances);
        group.setPerms(permissions);
        group.setIsdefault(isdefault);
        group.setRank(rank);
        group.setWeight(weight);
        group.setLadder(ladder);
        group.setDisplay(display);
        group.setPrefix(prefix);
        group.setSuffix(suffix);
        group.setServers(servers);
    }
    @Override
    public void reloadUser(User user) 
    {
        MysqlConfig permsconf=new MysqlConfig(mysql,table);
        
        ResultSet res=mysql.returnQuery("SELECT `key`,`value` FROM `"+table+"` WHERE `key` LIKE 'users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+"%' ORDER BY id ASC");
        
        permsconf.fromResult(res);
        
        try 
        {
            res.close();
        } catch (Exception ex) { }
        
        //load user from database
        List<String> sgroups=permsconf.getListString("users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+".groups", new ArrayList<String>());
        List<Group> lgroups=new ArrayList<>();
        for(String s:sgroups)
        {
            Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
            if(g!=null)
            {
                lgroups.add(g);
            }
        }
        List<String> extrapermissions=permsconf.getListString("users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+".permissions", new ArrayList<String>());

        Map<String,List<String>> serverperms=new HashMap<>();
        Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
        for(String server:permsconf.getSubNodes("users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+".servers"))
        {
            //per server perms
            serverperms.put(server, permsconf.getListString("users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+".servers."+server+".permissions", new ArrayList<String>()));

            //per server world perms
            Map<String,List<String>> worldperms=new HashMap<>();
            for(String world:permsconf.getSubNodes("users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+".servers."+server+".worlds"))
            {
                worldperms.put(world, permsconf.getListString("users."+(BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? user.getUUID().toString() : user.getName())+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>()));
            }
            serverworldperms.put(server, worldperms);
        }

        user.setGroups(lgroups);
        user.setExtraPerms(extrapermissions);
        user.setServerPerms(serverperms);
        user.setServerWorldPerms(serverworldperms);
    }
}
