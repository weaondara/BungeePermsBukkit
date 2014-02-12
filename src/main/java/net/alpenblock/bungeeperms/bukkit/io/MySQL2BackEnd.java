package net.alpenblock.bungeeperms.bukkit.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alpenblock.bungeeperms.bukkit.BungeePerms;
import net.alpenblock.bungeeperms.bukkit.Config;
import net.alpenblock.bungeeperms.bukkit.Debug;
import net.alpenblock.bungeeperms.bukkit.Group;
import net.alpenblock.bungeeperms.bukkit.Mysql;
import net.alpenblock.bungeeperms.bukkit.Server;
import net.alpenblock.bungeeperms.bukkit.User;
import net.alpenblock.bungeeperms.bukkit.World;
import net.alpenblock.bungeeperms.bukkit.io.mysql2.EntityType;
import net.alpenblock.bungeeperms.bukkit.io.mysql2.MysqlPermEntity;
import net.alpenblock.bungeeperms.bukkit.io.mysql2.MysqlPermsAdapter2;
import net.alpenblock.bungeeperms.bukkit.io.mysql2.ValueEntry;
import org.bukkit.plugin.Plugin;

public class MySQL2BackEnd implements BackEnd
{
    private org.bukkit.Server server;
    private Config config;
    private Debug debug;
    private Plugin plugin;
    private Mysql mysql;
    
    private MysqlPermsAdapter2 adapter;
    private String table;
    private String tablePrefix;
    
    public MySQL2BackEnd(org.bukkit.Server server, Config conf, Debug d)
    {
        this.server=server;
        config=conf;
        debug=d;
        
        loadConfig();
        
        mysql=new Mysql(conf,d,"bungeeperms");
        mysql.connect();
        
        table=tablePrefix+"permissions2";
        
        adapter=new MysqlPermsAdapter2(mysql,table);
        adapter.createTable();
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
		//permsconf.load();
    }
    @Override
    public List<Group> loadGroups()
    {
        List<Group> ret=new ArrayList<>();
        
        List<String> groups=adapter.getGroups();
		for(String g:groups)
		{
            Group group=loadGroup(g);
			ret.add(group);
		}
        Collections.sort(ret);
        
        return ret;
    }
    @Override
    public List<User> loadUsers()
    {
        List<User> ret=new ArrayList<>();
        
        List<String> users=adapter.getUsers();
		for(String u:users)
		{
			User user=loadUser(u);
			ret.add(user);
		}
        
        return ret;
    }
    @Override
    public Group loadGroup(String group)
    {
        MysqlPermEntity mpe = adapter.getGroup(group);
        if(mpe.getName()==null)
        {
            return null;
        }
        
        List<String> inheritances=getValue(mpe.getData("inheritances"));
        boolean isdefault=getFirstValue(mpe.getData("default"),false);
        int rank=getFirstValue(mpe.getData("rank"), 1000);
        String ladder=getFirstValue(mpe.getData("ladder"), "default");
        String display=getFirstValue(mpe.getData("display"), "");
        String prefix=getFirstValue(mpe.getData("prefix"), "");
        String suffix=getFirstValue(mpe.getData("suffix"), "");

        //perms
        List<ValueEntry> permdata = mpe.getData("permissions");
        if(permdata==null)
        {
            permdata=new ArrayList<>();
        }
        List<String> globalperms=new ArrayList<>();
        List<String> foundservers=new ArrayList<>();

        //globalperms
        for(ValueEntry e:permdata)
        {
            //check for servers 
            if(e.getServer()!=null)
            {
                if(!foundservers.contains(e.getServer().toLowerCase()))
                {
                    foundservers.add(e.getServer().toLowerCase());
                }
            }

            //is global perm
            else
            {
                globalperms.add(e.getValue());
            }
        }

        //server perms
        Map<String,Server> servers=new HashMap<>();
        for(String server:foundservers)
        {
            List<String> serverperms=new ArrayList<>();
            List<String> foundworlds=new ArrayList<>();
            for(ValueEntry e:permdata)
            {
                if(e.getServer()!=null && e.getServer().equalsIgnoreCase(server))
                {
                    //check for worlds 
                    if(e.getWorld()!=null)
                    {
                        if(!foundworlds.contains(e.getWorld().toLowerCase()))
                        {
                            foundworlds.add(e.getWorld().toLowerCase());
                        }
                    }

                    //is server perm
                    else
                    {
                        serverperms.add(e.getValue());
                    }
                }
            }

            //world perms
            Map<String,World> worlds=new HashMap<>();
            for(String world:foundservers)
            {
                List<String> worldperms=new ArrayList<>();
                for(ValueEntry e:permdata)
                {
                    if(e.getServer()!=null && e.getServer().equalsIgnoreCase(server) && e.getWorld()!=null && e.getWorld().equalsIgnoreCase(world))
                    {
                        worldperms.add(e.getValue());
                    }
                }

                World w=new World(world.toLowerCase(),worldperms,null,null,null);
                worlds.put(world.toLowerCase(), w);
            }

            Server s=new Server(server,serverperms,worlds,null,null,null);
            servers.put(server.toLowerCase(),s);
        }

        // display props for servers and worlds
        for(Map.Entry<String, Server> server:servers.entrySet())
        {
            String sdisplay=getFirstValue(mpe.getData("display"), server.getKey(), "");
            String sprefix=getFirstValue(mpe.getData("prefix"), server.getKey(), "");
            String ssuffix=getFirstValue(mpe.getData("suffix"), server.getKey(), "");
            server.getValue().setDisplay(sdisplay);
            server.getValue().setPrefix(sprefix);
            server.getValue().setSuffix(ssuffix);

            for(Map.Entry<String, World> world:server.getValue().getWorlds().entrySet())
            {
                String wdisplay=getFirstValue(mpe.getData("display"), server.getKey(), world.getKey(), "");
                String wprefix=getFirstValue(mpe.getData("prefix"), server.getKey(), world.getKey(), "");
                String wsuffix=getFirstValue(mpe.getData("suffix"), server.getKey(), world.getKey(), "");
                world.getValue().setDisplay(wdisplay);
                world.getValue().setPrefix(wprefix);
                world.getValue().setSuffix(wsuffix);
            }
        }

        Group g=new Group(group, inheritances, globalperms, servers, rank, ladder, isdefault, display, prefix, suffix);
        return g;
    }
    @Override
    public User loadUser(String user) 
    {
        MysqlPermEntity mpe = adapter.getUser(user);
        if(mpe.getName()==null)
        {
            return null;
        }
        
        List<String> sgroups=getValue(mpe.getData("groups"));
        List<Group> lgroups=new ArrayList<>();
        for(String s:sgroups)
        {
            Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
            if(g!=null)
            {
                lgroups.add(g);
            }
        }


        //perms
        List<ValueEntry> permdata = mpe.getData("permissions");
        if(permdata==null)
        {
            permdata=new ArrayList<>();
        }
        List<String> globalperms=new ArrayList<>();
        Map<String,List<String>> serverperms=new HashMap<>();
        Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
        for(ValueEntry e:permdata)
        {
            if(e.getServer()==null)
            {
                globalperms.add(e.getServer());
            }
            else if(e.getWorld()==null)
            {
                List<String> server = serverperms.get(e.getServer().toLowerCase());
                if(server==null)
                {
                    server=new ArrayList<>();
                    serverperms.put(e.getServer().toLowerCase(), server);
                }
                server.add(e.getValue());
            }
            else
            {
                Map<String, List<String>> server = serverworldperms.get(e.getServer().toLowerCase());
                if(server==null)
                {
                    server=new HashMap<>();
                    serverworldperms.put(e.getServer().toLowerCase(), server);
                }

                List<String> world = server.get(e.getWorld().toLowerCase());
                if(world==null)
                {
                    world=new ArrayList<>();
                    server.put(e.getWorld().toLowerCase(), world);
                }
                world.add(e.getValue());
            }
        }

        User u=new User(user, lgroups, globalperms, serverperms,serverworldperms);
        return u;
    }
    @Override
    public int loadVersion()
    {
        MysqlPermEntity mpe = adapter.getVersion();
        int version=getFirstValue(mpe.getData("version"),2);
        return version;
    }

    @Override
    public boolean isUserInDatabase(User user)
    {
        return adapter.isInBD(user.getName(), EntityType.User);
    }
    
    //helper functions
    private List<String> getValue(List<ValueEntry> values)
    {
        if(values==null)
        {
            return new ArrayList<>();
        }
        List<String> ret=new ArrayList<>();
        for(ValueEntry e:values)
        {
            ret.add(e.getValue());
        }
        
        return ret;
    }
    private String getFirstValue(List<ValueEntry> values, String def)
    {
        if(values==null || values.isEmpty())
        {
            return def;
        }
        for(ValueEntry e:values)
        {
            if(e.getServer() == null && e.getWorld()==null)
            {
                return e.getValue();
            }
        }
        return def;
    }
    private String getFirstValue(List<ValueEntry> values, String server, String def)
    {
        if(values==null || values.isEmpty())
        {
            return def;
        }
        for(ValueEntry e:values)
        {
            if(e.getServer() != null && e.getServer().equalsIgnoreCase(server) && e.getWorld()==null)
            {
                return e.getValue();
            }
        }
        return def;
    }
    private String getFirstValue(List<ValueEntry> values, String server, String world, String def)
    {
        if(values==null || values.isEmpty())
        {
            return def;
        }
        for(ValueEntry e:values)
        {
            if(e.getServer() != null && e.getServer().equalsIgnoreCase(server) && e.getWorld()!=null && e.getWorld().equalsIgnoreCase(world))
            {
                return e.getValue();
            }
        }
        return def;
    }
    private boolean getFirstValue(List<ValueEntry> values, boolean def)
    {
        if(values==null || values.isEmpty())
        {
            return def;
        }
        try
        {
            return Boolean.parseBoolean(values.get(0).getValue());
        }
        catch(Exception e)
        {
            return def;
        }
    }
    private int getFirstValue(List<ValueEntry> values, int def)
    {
        if(values==null || values.isEmpty())
        {
            return def;
        }
        try
        {
            return Integer.parseInt(values.get(0).getValue());
        }
        catch(Exception e)
        {
            return def;
        }
    }

    

    @Override
    public void reloadGroup(Group group)
    {
        MysqlPermEntity mpe = adapter.getGroup(group.getName());
        List<String> inheritances=getValue(mpe.getData("inheritances"));
        boolean isdefault=getFirstValue(mpe.getData("default"),false);
        int rank=getFirstValue(mpe.getData("rank"), 1000);
        String ladder=getFirstValue(mpe.getData("ladder"), "default");
        String display=getFirstValue(mpe.getData("display"), "");
        String prefix=getFirstValue(mpe.getData("prefix"), "");
        String suffix=getFirstValue(mpe.getData("suffix"), "");

        //perms
        List<ValueEntry> permdata = mpe.getData("permissions");
        if(permdata==null)
        {
            permdata=new ArrayList<>();
        }
        List<String> globalperms=new ArrayList<>();
        List<String> foundservers=new ArrayList<>();

        //globalperms
        for(ValueEntry e:permdata)
        {
            //check for servers 
            if(e.getServer()!=null)
            {
                if(!foundservers.contains(e.getServer().toLowerCase()))
                {
                    foundservers.add(e.getServer().toLowerCase());
                }
            }

            //is global perm
            else
            {
                globalperms.add(e.getValue());
            }
        }

        //server perms
        Map<String,Server> servers=new HashMap<>();
        for(String server:foundservers)
        {
            List<String> serverperms=new ArrayList<>();
            List<String> foundworlds=new ArrayList<>();
            for(ValueEntry e:permdata)
            {
                if(e.getServer()!=null && e.getServer().equalsIgnoreCase(server))
                {
                    //check for worlds 
                    if(e.getWorld()!=null)
                    {
                        if(!foundworlds.contains(e.getWorld().toLowerCase()))
                        {
                            foundworlds.add(e.getWorld().toLowerCase());
                        }
                    }

                    //is server perm
                    else
                    {
                        serverperms.add(e.getValue());
                    }
                }
            }

            //world perms
            Map<String,World> worlds=new HashMap<>();
            for(String world:foundservers)
            {
                List<String> worldperms=new ArrayList<>();
                for(ValueEntry e:permdata)
                {
                    if(e.getServer()!=null && e.getServer().equalsIgnoreCase(server) && e.getWorld()!=null && e.getWorld().equalsIgnoreCase(world))
                    {
                        worldperms.add(e.getValue());
                    }
                }

                World w=new World(world.toLowerCase(),worldperms,null,null,null);
                worlds.put(world.toLowerCase(), w);
            }

            Server s=new Server(server,serverperms,worlds,null,null,null);
            servers.put(server.toLowerCase(),s);
        }

        // display props for servers and worlds
        for(Map.Entry<String, Server> server:servers.entrySet())
        {
            String sdisplay=getFirstValue(mpe.getData("display"), server.getKey(), "");
            String sprefix=getFirstValue(mpe.getData("prefix"), server.getKey(), "");
            String ssuffix=getFirstValue(mpe.getData("suffix"), server.getKey(), "");
            server.getValue().setDisplay(sdisplay);
            server.getValue().setPrefix(sprefix);
            server.getValue().setSuffix(ssuffix);

            for(Map.Entry<String, World> world:server.getValue().getWorlds().entrySet())
            {
                String wdisplay=getFirstValue(mpe.getData("display"), server.getKey(), world.getKey(), "");
                String wprefix=getFirstValue(mpe.getData("prefix"), server.getKey(), world.getKey(), "");
                String wsuffix=getFirstValue(mpe.getData("suffix"), server.getKey(), world.getKey(), "");
                world.getValue().setDisplay(wdisplay);
                world.getValue().setPrefix(wprefix);
                world.getValue().setSuffix(wsuffix);
            }
        }
        
        group.setInheritances(inheritances);
        group.setPerms(globalperms);
        group.setIsdefault(isdefault);
        group.setRank(rank);
        group.setLadder(ladder);
        group.setDisplay(display);
        group.setPrefix(prefix);
        group.setSuffix(suffix);
        group.setServers(servers);
    }
    @Override
    public void reloadUser(User user) 
    {
        MysqlPermEntity mpe = adapter.getUser(user.getName());
        List<String> sgroups=getValue(mpe.getData("groups"));
        List<Group> lgroups=new ArrayList<>();
        for(String s:sgroups)
        {
            Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
            if(g!=null)
            {
                lgroups.add(g);
            }
        }


        //perms
        List<ValueEntry> permdata = mpe.getData("permissions");
        if(permdata==null)
        {
            permdata=new ArrayList<>();
        }
        List<String> globalperms=new ArrayList<>();
        Map<String,List<String>> serverperms=new HashMap<>();
        Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
        for(ValueEntry e:permdata)
        {
            if(e.getServer()==null)
            {
                globalperms.add(e.getServer());
            }
            else if(e.getWorld()==null)
            {
                List<String> server = serverperms.get(e.getServer().toLowerCase());
                if(server==null)
                {
                    server=new ArrayList<>();
                    serverperms.put(e.getServer().toLowerCase(), server);
                }
                server.add(e.getValue());
            }
            else
            {
                Map<String, List<String>> server = serverworldperms.get(e.getServer().toLowerCase());
                if(server==null)
                {
                    server=new HashMap<>();
                    serverworldperms.put(e.getServer().toLowerCase(), server);
                }

                List<String> world = server.get(e.getWorld().toLowerCase());
                if(world==null)
                {
                    world=new ArrayList<>();
                    server.put(e.getWorld().toLowerCase(), world);
                }
                world.add(e.getValue());
            }
        }
        
        user.setGroups(lgroups);
        user.setExtraperms(globalperms);
        user.setServerPerms(serverperms);
        user.setServerWorldPerms(serverworldperms);
    }
}
