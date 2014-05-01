package net.alpenblock.bungeeperms.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.alpenblock.bungeeperms.bukkit.io.BackEnd;
import net.alpenblock.bungeeperms.bukkit.io.BackEndType;
import net.alpenblock.bungeeperms.bukkit.io.MySQL2BackEnd;
import net.alpenblock.bungeeperms.bukkit.io.MySQLBackEnd;
import net.alpenblock.bungeeperms.bukkit.io.MySQLUUIDPlayerDB;
import net.alpenblock.bungeeperms.bukkit.io.NoneUUIDPlayerDB;
import net.alpenblock.bungeeperms.bukkit.io.UUIDPlayerDB;
import net.alpenblock.bungeeperms.bukkit.io.UUIDPlayerDBType;
import net.alpenblock.bungeeperms.bukkit.io.YAMLBackEnd;
import net.alpenblock.bungeeperms.bukkit.io.YAMLUUIDPlayerDB;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PermissionsManager implements Listener,PluginMessageListener
{
	private org.bukkit.Server server;
	private Plugin plugin;
    private Config config;
    private Debug debug;
    private boolean enabled;
    
    private String channel;
    
    @Getter
    private boolean saveAllUsers;
    @Getter
    private boolean deleteUsersOnCleanup;
    
    @Getter @Setter
    private BackEnd backEnd;
    @Getter
    private UUIDPlayerDB UUIDPlayerDB;
    
    @Getter
    private boolean useUUIDs;
    
    @Getter
    private boolean useregexperms;
    
    @Getter
    private PermissionsResolver resolver;
    
    @Getter 
    private boolean superpermscompat;
    
    private List<Group> groups;
    private List<User> users;
    private int permsversion;
	
	public PermissionsManager(Plugin p,Config conf,Debug d)
	{
		server=p.getServer();
		plugin=p;
        config=conf;
        debug=d;
        
        channel="bungeeperms";
        
        resolver=new PermissionsResolver();
		
		//perms
		loadPerms();
        
        enabled=false;
	}
	
	public final void loadPerms()
	{
		server.getLogger().info("[BungeePerms] loading permissions ...");
        
        useUUIDs=config.getBoolean("useUUIDs", false);
		
        useregexperms=config.getBoolean("useregexperms", false);
        resolver.setUseRegex(useregexperms);
        
        superpermscompat=config.getBoolean("superpermscompat", false);
        
        BackEndType bet=config.getEnumValue("backendtype",BackEndType.MySQL);
        if(bet==BackEndType.YAML)
        {
            backEnd=new YAMLBackEnd(server,plugin);
        }
        else if(bet==BackEndType.MySQL)
        {
            backEnd=new MySQLBackEnd(server,config,debug);
        }
        else if(bet==BackEndType.MySQL2)
        {
            backEnd=new MySQL2BackEnd(server,config,debug);
        }
        backEnd.load();
        
        UUIDPlayerDBType updbt=config.getEnumValue("uuidplayerdb",UUIDPlayerDBType.None);
        if(updbt==UUIDPlayerDBType.None)
        {
            UUIDPlayerDB=new NoneUUIDPlayerDB();
        }
        else if(updbt==UUIDPlayerDBType.YAML)
        {
            UUIDPlayerDB=new YAMLUUIDPlayerDB();
        }
        else if(updbt==UUIDPlayerDBType.MySQL)
        {
            UUIDPlayerDB=new MySQLUUIDPlayerDB(config,debug);
        }
        
        //load all groups
        groups=backEnd.loadGroups();
        
        users=new ArrayList<>();
        
        //load permsversion
        permsversion=backEnd.loadVersion();
		
		server.getLogger().info("[BungeePerms] permissions loaded");
	}
    
    public void enable()
    {
        if(!enabled)
        {
            //inject into console
            Permissible permissible=new Permissible(server.getConsoleSender());
            org.bukkit.permissions.Permissible oldpermissible=Injector.inject(server.getConsoleSender(), permissible);
            permissible.setOldPermissible(oldpermissible);
            
            //load online players; allows reload
            for(Player p:Bukkit.getOnlinePlayers())
            {
                if(useUUIDs)
                {
                    getUser(p.getUniqueId());
                }
                else
                {
                    getUser(p.getName());
                }
                setBukkitPermissions(p);
            }
            
            server.getPluginManager().registerEvents(this,plugin);
            server.getMessenger().registerOutgoingPluginChannel(plugin, channel);
            server.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
            enabled=true;
        }
    }
    public void disable()
    {
        if(!enabled)
        {
            //uninject from console
            Injector.uninject(server.getConsoleSender());
            
            PlayerLoginEvent.getHandlerList().unregister(this);
            PlayerJoinEvent.getHandlerList().unregister(this);
            PlayerQuitEvent.getHandlerList().unregister(this);
            PlayerChangedWorldEvent.getHandlerList().unregister(this);
            server.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
            server.getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
            enabled=false;
        }
    }
	

    public synchronized Group getMainGroup(User player) 
	{
		if(player==null)
		{
			throw new NullPointerException("player is null");
		}
		if(player.getGroups().isEmpty())
		{
			return null;
		}
		Group ret=player.getGroups().get(0);
		for(int i=1;i<player.getGroups().size();i++)
		{
			if(player.getGroups().get(i).getWeight()<ret.getWeight())
			{
				ret=player.getGroups().get(i);
			}
		}
		return ret;
	}
	public synchronized Group getNextGroup(Group group)
	{
        List<Group> laddergroups=getLadderGroups(group.getLadder());
        
		for(int i=0;i<laddergroups.size();i++)
		{
			if(laddergroups.get(i).getRank()==group.getRank())
			{
				if(i+1<laddergroups.size())
				{
					return laddergroups.get(i+1);
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
        List<Group> laddergroups=getLadderGroups(group.getLadder());
        
		for(int i=0;i<laddergroups.size();i++)
		{
			if(laddergroups.get(i).getRank()==group.getRank())
			{
				if(i>0)
				{
					return laddergroups.get(i-1);
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
        List<Group> ret=new ArrayList<>();
        
        for(Group g:groups)
        {
            if(g.getLadder().equalsIgnoreCase(ladder))
            {
                ret.add(g);
            }
        }
        
        Collections.sort(ret);
        
        return ret;
    }
    public synchronized List<Group> getDefaultGroups()
	{
		List<Group> ret=new ArrayList<>();
		for(Group g:groups)
		{
			if(g.isDefault())
			{
				ret.add(g);
			}
		}
		return ret;
	}
    
	public synchronized Group getGroup(String groupname)
	{
		for(Group g:groups)
		{
			if(g.getName().equalsIgnoreCase(groupname))
			{
				return g;
			}
		}
        
        Group g=backEnd.loadGroup(groupname);
        
        Collections.sort(groups);
        for(Group gr:groups)
        {
            gr.recalcPerms();
        }
        
		return null;
	}
	public synchronized User getUser(String usernameoruuid)
	{
        if(useUUIDs)
        {
            UUID uuid=Statics.parseUUID(usernameoruuid);
            if(uuid!=null)
            {
                return getUser(uuid);
            }
            else
            {
                uuid=UUIDPlayerDB.getUUID(usernameoruuid);
                if(uuid!=null)
                {
                    return getUser(uuid);
                }
            }
        }
        
		for(User u:users)
		{
			if(u.getName().equalsIgnoreCase(usernameoruuid))
			{
				return u;
			}
		}
        
        //load user from database
        User u=backEnd.loadUser(usernameoruuid);
        if(u!=null)
        {
            users.add(u);
            return u;
        }
        
		return null;
	}
    public synchronized User getUser(UUID uuid)
	{
		for(User u:users)
		{
			if(u.getUUID().equals(uuid))
			{
				return u;
			}
		}
        
        //load user from database
        User u=backEnd.loadUser(uuid);
        if(u!=null)
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
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onLogin(PlayerLoginEvent e)
	{
        reloadUser(e.getPlayer().getUniqueId());
        
        //inject permissible
        Permissible permissible=new Permissible(e.getPlayer());
        org.bukkit.permissions.Permissible oldpermissible=Injector.inject(e.getPlayer(), permissible);
        permissible.setOldPermissible(oldpermissible);
        
        //add permissions
        setBukkitPermissions(e.getPlayer());
	}
    @EventHandler(priority=EventPriority.LOWEST)
	public void onJoin(PlayerJoinEvent e)
	{
        //add permissions
        setBukkitPermissions(e.getPlayer());
        
        sendWorldUpdate(e.getPlayer());
	}
    @EventHandler(priority=EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent e)
	{
        //remove permissions
        removeBukkitPermissions(e.getPlayer());
        
        //uninject permissible
        Injector.uninject(e.getPlayer());
        
        User u=getUser(e.getPlayer().getUniqueId());
        users.remove(u);
	}
    @EventHandler
	public void onChangedWorld(PlayerChangedWorldEvent e)
	{
        //set new permissions
        setBukkitPermissions(e.getPlayer());
        
        sendWorldUpdate(e.getPlayer());
	}
	
    //api
	public boolean hasPerm(CommandSender sender, String permission)
	{
		if(sender instanceof Player)
		{
			return (useUUIDs ? getUser(((Player)sender).getUniqueId()) : getUser(sender.getName())).hasPerm(permission);
		}
		return false;
	}
	public boolean hasPermOrConsole(CommandSender sender, String permission)
	{
		if(sender instanceof Player)
		{
			return (useUUIDs ? getUser(((Player)sender).getUniqueId()) : getUser(sender.getName())).hasPerm(permission);
		}
		else if(sender instanceof ConsoleCommandSender)
		{
			return true;
		}
		return false;
	}
	public boolean hasPerm(String sender, String permission)
	{
		if(!sender.equalsIgnoreCase("CONSOLE"))
		{
			User u=getUser(sender);
			if(u==null)
			{
				return false;
			}
			return u.hasPerm(permission);
		}
		return false;
	}
	public boolean hasPermOrConsole(String sender, String permission)
	{
		if(sender.equalsIgnoreCase("CONSOLE"))
		{
			return true;
		}
		else
		{
			User u=getUser(sender);
			if(u==null)
			{
				return false;
			}
			return u.hasPerm(permission);
		}
	}
	public boolean has(CommandSender sender, String perm, boolean msg)
	{
		if(sender instanceof Player)
		{
			boolean isperm=(hasPerm(sender, perm));
			if(!isperm & msg){sender.sendMessage(Color.Error+"You don't have permission to do that!"+ChatColor.RESET);}
			return isperm;
		}
		else
		{
			sender.sendMessage(Color.Error+"You don't have permission to do that!"+ChatColor.RESET);
			return false;
		}
	}
	public boolean hasOrConsole(CommandSender sender, String perm, boolean msg)
	{
		boolean isperm=(hasPerm(sender, perm)|(sender instanceof ConsoleCommandSender));
		if(!isperm & msg){sender.sendMessage(Color.Error+"You don't have permission to do that!"+ChatColor.RESET);}
		return isperm;
	}
	public boolean hasPermOnServer(String sender, String permission,String server)
	{
		if(!sender.equalsIgnoreCase("CONSOLE"))
		{
			User p=getUser(sender);
			if(p==null)
			{
				return false;
			}
			return p.hasPermOnServer(permission,server);
		}
		return false;
	}
	public boolean hasPermOrConsoleOnServer(String sender, String permission,String server)
	{
		if(sender.equalsIgnoreCase("CONSOLE"))
		{
			return true;
		}
		else
		{
			User u=getUser(sender);
			if(u==null)
			{
				return false;
			}
			return u.hasPermOnServer(permission,server);
		}
	}

	public boolean hasPermOnServerInWorld(String sender, String permission,String server,String world)
	{
		if(!sender.equalsIgnoreCase("CONSOLE"))
		{
			User u=getUser(sender);
			if(u==null)
			{
				return false;
			}
            
            if(world==null)
            {
                return hasPermOnServer(sender,permission,server);
            }
                
			return u.hasPermOnServerInWorld(permission,server,world);
		}
		return false;
	}
	public boolean hasPermOrConsoleOnServerInWorld(String sender, String permission,String server,String world)
	{
		if(sender.equalsIgnoreCase("CONSOLE"))
		{
			return true;
		}
		else
		{
			User u=getUser(sender);
			if(u==null)
			{
				return false;
			}
            
			if(world==null)
            {
                return hasPermOnServer(sender,permission,server);
            }
                
			return u.hasPermOnServerInWorld(permission,server,world);
		}
	}

    //permissions update
    private void setBukkitPermissions(Player player) 
    {
//        User user=getUser(player.getName());
//        
//        String permname = "bungeeperms.player." + player.getName();
//        Permission perm = Bukkit.getPluginManager().getPermission(permname);
//        
//        boolean hasPermissionAttachment = player.hasPermission(permname);
//        
//        //a non-existing user must not have any permission
//        if(user==null)
//        {
//            if(hasPermissionAttachment)
//            {
//                Bukkit.getPluginManager().removePermission(permname);
//            }
//            return;
//        }
//        
//        if (perm == null) 
//        {
//            perm = new Permission(permname, PermissionDefault.FALSE, listToMap(user.getEffectivePerms(BungeePerms.getInstance().getServerName(),player.getWorld().getName())));
//            Bukkit.getPluginManager().addPermission(perm);
//        }
//        else 
//        {
//            perm.getChildren().clear();
//            perm.getChildren().putAll(listToMap(user.getEffectivePerms(BungeePerms.getInstance().getServerName(),player.getWorld().getName())));
//        }
//
//        perm.recalculatePermissibles();
//
//        if (!hasPermissionAttachment) 
//        {
//            player.addAttachment(BungeePerms.getInstance(), perm.getName(), true);
//        }
    }
    private void removeBukkitPermissions(Player player) 
    {
//        String permname = "bungeeperms.player." + player.getName();
//        Bukkit.getPluginManager().removePermission(permname);
    }
    public void refreshBukkitPermissions(String player) 
    {
//        Player p = Bukkit.getPlayerExact(player);
//        if (p != null) 
//        {
//            setBukkitPermissions(p);
//        }
    }
    
    //for bungeecord per-server-and-world perms
    private void sendWorldUpdate(Player p)
    {
        p.sendPluginMessage(plugin, channel, ("playerworldupdate;"+p.getName()+";"+p.getWorld().getName()).getBytes());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) 
    {
        String msg=new String(bytes);
        List<String> data=Statics.toList(msg, ";");
        
        debug.log("msg="+msg);
        
        String cmd=data.get(0);
        String userorgroup=data.size()>1?data.get(1):null;
        
        if(cmd.equalsIgnoreCase("deleteuser"))
        {
            User u=getUser(userorgroup);
            users.remove(u);
            
            //refreshBukkitPermissions(userorgroup);
        }
        else if(cmd.equalsIgnoreCase("deletegroup"))
        {
            Group g=getGroup(userorgroup);
            groups.remove(g);
            for(Group gr:groups)
            {
                gr.recalcPerms();
            }
            for(User u:users)
            {
                u.recalcPerms();
                //setBukkitPermissions(p);
            }
        }
        else if(cmd.equalsIgnoreCase("reloaduser"))
        {
            reloadUser(userorgroup);
        }
        else if(cmd.equalsIgnoreCase("reloadgroup"))
        {
            reloadGroup(userorgroup);
        }
        else if(cmd.equalsIgnoreCase("reloadusers"))
        {
            reloadUsers();
        }
        else if(cmd.equalsIgnoreCase("reloadgroups"))
        {
            reloadGroups();
        }
        else if(cmd.equalsIgnoreCase("reloadall"))
        {
            config.load();
            BungeePerms.getInstance().loadConfig();
            loadPerms();
        }
    }
    
    private void reloadUser(String user)
    {
        User u=getUser(user);
        if(u==null)
        {
            debug.log("User "+user+" not found!!!");
            return;
        }
        backEnd.reloadUser(u);
        u.recalcPerms();
        //refreshBukkitPermissions(user);
    }
    private void reloadUser(UUID uuid)
    {
        User u=getUser(uuid);
        if(u==null)
        {
            debug.log("User "+uuid+" not found!!!");
            return;
        }
        backEnd.reloadUser(u);
        u.recalcPerms();
        //refreshBukkitPermissions(user);
    }
    private void reloadGroup(String group)
    {
        Group g=getGroup(group);
        if(g==null)
        {
            debug.log("Group "+group+" not found!!!");
            return;
        }
        backEnd.reloadGroup(g);
        Collections.sort(groups);
        for(Group gr:groups)
        {
            gr.recalcPerms();
        }
        for(User u:users)
        {
            u.recalcPerms();
            //setBukkitPermissions(p);
        }
    }
    private void reloadUsers()
    {
        for(User u:users)
        {
            backEnd.reloadUser(u);
            u.recalcPerms();
        }
    }
    private void reloadGroups()
    {
        for(Group g:groups)
        {
            backEnd.reloadGroup(g);
        }
        Collections.sort(groups);
        for(Group g:groups)
        {
            g.recalcPerms();
        }
        for(User u:users)
        {
            u.recalcPerms();
        }
    }
}