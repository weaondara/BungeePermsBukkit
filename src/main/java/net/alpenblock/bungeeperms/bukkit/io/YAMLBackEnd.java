package net.alpenblock.bungeeperms.bukkit.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alpenblock.bungeeperms.bukkit.BungeePerms;
import net.alpenblock.bungeeperms.bukkit.Config;
import net.alpenblock.bungeeperms.bukkit.Group;
import net.alpenblock.bungeeperms.bukkit.Server;
import net.alpenblock.bungeeperms.bukkit.User;
import net.alpenblock.bungeeperms.bukkit.World;
import org.bukkit.plugin.Plugin;

public class YAMLBackEnd implements BackEnd
{
    private org.bukkit.Server server;
    private Plugin plugin;
    
    private String permspath;
    private Config permsconf;
    
    public YAMLBackEnd(org.bukkit.Server server, Plugin p)
    {
        this.server=server;
        plugin=p;
        
        permspath="/permissions.yml";
        
        checkPermFile();
        
        permsconf=new Config(plugin,permspath);
    }
    
    @Override
    public BackEndType getType()
    {
        return BackEndType.YAML;
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
			List<String> inheritances=permsconf.getListString("groups."+g+".inheritances", new ArrayList<String>());
			List<String> permissions=permsconf.getListString("groups."+g+".permissions", new ArrayList<String>());
			boolean isdefault=permsconf.getBoolean("groups."+g+".default",false);
			int rank=permsconf.getInt("groups."+g+".rank", 1000);
			String ladder=permsconf.getString("groups."+g+".ladder", "default");
			String display=permsconf.getString("groups."+g+".display", "");
			String prefix=permsconf.getString("groups."+g+".prefix", "");
			String suffix=permsconf.getString("groups."+g+".suffix", "");
			
			//per server perms
			Map<String,Server> servers=new HashMap<>();
			for(String server:permsconf.getSubNodes("groups."+g+".servers"))
			{
				List<String> serverperms=permsconf.getListString("groups."+g+".servers."+server+".permissions", new ArrayList<String>());
                String sdisplay=permsconf.getString("groups."+g+".servers."+server+".display", "");
                String sprefix=permsconf.getString("groups."+g+".servers."+server+".prefix", "");
                String ssuffix=permsconf.getString("groups."+g+".servers."+server+".suffix", "");
                
                //per server world perms
                Map<String,World> worlds=new HashMap<>();
                for(String world:permsconf.getSubNodes("groups."+g+".servers."+server+".worlds"))
                {
                    List<String> worldperms=permsconf.getListString("groups."+g+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>());
                    String wdisplay=permsconf.getString("groups."+g+".servers."+server+".worlds."+world+".display", "");
                    String wprefix=permsconf.getString("groups."+g+".servers."+server+".worlds."+world+".prefix", "");
                    String wsuffix=permsconf.getString("groups."+g+".servers."+server+".worlds."+world+".suffix", "");
                    
                    World w=new World(world,worldperms,wdisplay,wprefix,wsuffix);
                    worlds.put(world, w);
                }
                
                servers.put(server, new Server(server,serverperms,worlds,sdisplay,sprefix,ssuffix));
			}
			
			Group group=new Group(g, inheritances, permissions, servers, rank, ladder, isdefault, display, prefix, suffix);
			ret.add(group);
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
			List<String> sgroups=permsconf.getListString("users."+u+".groups", new ArrayList<String>());
			List<Group> lgroups=new ArrayList<>();
			for(String s:sgroups)
			{
				Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
				if(g!=null)
				{
					lgroups.add(g);
				}
			}
			List<String> extrapermissions=permsconf.getListString("users."+u+".permissions", new ArrayList<String>());
			
			Map<String,List<String>> serverperms=new HashMap<>();
			Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
			for(String server:permsconf.getSubNodes("users."+u+".servers"))
			{
                //per server perms
				serverperms.put(server, permsconf.getListString("users."+u+".servers."+server+".permissions", new ArrayList<String>()));
                
                //per server world perms
                Map<String,List<String>> worldperms=new HashMap<>();
                for(String world:permsconf.getSubNodes("users."+u+".servers."+server+".worlds"))
                {
                    worldperms.put(world, permsconf.getListString("users."+u+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>()));
                }
                serverworldperms.put(server, worldperms);
			}
			
			User user=new User(u, lgroups, extrapermissions, serverperms,serverworldperms);
			ret.add(user);
		}
        
        return ret;
    }
    @Override
    public Group loadGroup(String group)
    {
        List<String> inheritances=permsconf.getListString("groups."+group+".inheritances", new ArrayList<String>());
        List<String> permissions=permsconf.getListString("groups."+group+".permissions", new ArrayList<String>());
        boolean isdefault=permsconf.getBoolean("groups."+group+".default",false);
        int rank=permsconf.getInt("groups."+group+".rank", 1000);
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

        Group g=new Group(group, inheritances, permissions, servers, rank, ladder, isdefault, display, prefix, suffix);
        return g;
    }
    @Override
    public User loadUser(String user) 
    {
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

        User u=new User(user, lgroups, extrapermissions, serverperms,serverworldperms);
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
    

    private void checkPermFile()
    {
        File f=new File(plugin.getDataFolder(),permspath);
        if(!f.exists()|!f.isFile())
        {
            plugin.getLogger().info("[BungeePerms] no permissions file found !!!");
        }
    }

    

    @Override
    public void reloadGroup(Group group) 
    {
        permsconf.load();
        
        //load group from database
        List<String> inheritances=permsconf.getListString("groups."+group.getName()+".inheritances", new ArrayList<String>());
        List<String> permissions=permsconf.getListString("groups."+group.getName()+".permissions", new ArrayList<String>());
        boolean isdefault=permsconf.getBoolean("groups."+group.getName()+".default",false);
        int rank=permsconf.getInt("groups."+group.getName()+".rank", 1000);
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
        group.setLadder(ladder);
        group.setDisplay(display);
        group.setPrefix(prefix);
        group.setSuffix(suffix);
        group.setServers(servers);
    }

    @Override
    public void reloadUser(User user)
    {
        permsconf.load();
        
        //load user from database
        List<String> sgroups=permsconf.getListString("users."+user.getName()+".groups", new ArrayList<String>());
        List<Group> lgroups=new ArrayList<>();
        for(String s:sgroups)
        {
            Group g=BungeePerms.getInstance().getPermissionsManager().getGroup(s);
            if(g!=null)
            {
                lgroups.add(g);
            }
        }
        List<String> extrapermissions=permsconf.getListString("users."+user.getName()+".permissions", new ArrayList<String>());

        Map<String,List<String>> serverperms=new HashMap<>();
        Map<String,Map<String,List<String>>> serverworldperms=new HashMap<>();
        for(String server:permsconf.getSubNodes("users."+user.getName()+".servers"))
        {
            //per server perms
            serverperms.put(server, permsconf.getListString("users."+user.getName()+".servers."+server+".permissions", new ArrayList<String>()));

            //per server world perms
            Map<String,List<String>> worldperms=new HashMap<>();
            for(String world:permsconf.getSubNodes("users."+user.getName()+".servers."+server+".worlds"))
            {
                worldperms.put(world, permsconf.getListString("users."+user.getName()+".servers."+server+".worlds."+world+".permissions", new ArrayList<String>()));
            }
            serverworldperms.put(server, worldperms);
        }

        user.setGroups(lgroups);
        user.setExtraperms(extrapermissions);
        user.setServerPerms(serverperms);
        user.setServerWorldPerms(serverworldperms);
    }
}