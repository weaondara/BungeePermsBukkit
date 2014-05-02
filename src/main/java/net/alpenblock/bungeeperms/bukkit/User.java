package net.alpenblock.bungeeperms.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

@Getter
@Setter
@ToString
public class User
{
    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    private Map<String,List<String>> cachedPerms;
    
	private String name;
    private UUID UUID;
	private List<Group> groups;
	private List<String> extraPerms;
	private Map<String, List<String>> serverPerms;
    private Map<String, Map<String, List<String>>> serverWorldPerms;
	
	public User(String name, UUID UUID, List<Group> groups, List<String> extraPerms, Map<String, List<String>> serverPerms, Map<String, Map<String, List<String>>> serverWorldPerms) 
	{
		this.name = name;
        this.UUID = UUID;
		this.groups = groups;
		this.extraPerms = extraPerms;
		this.serverPerms = serverPerms;
		this.serverWorldPerms = serverWorldPerms;
        
        cachedPerms=new HashMap<>();
	}
	
    public boolean hasPerm(String perm)
	{
		List<String> perms=getEffectivePerms();
        
        Boolean has=BungeePerms.getInstance().getPermissionsManager().getResolver().has(perms, perm);
		
        has=checkSuperPerms(has, perm);
		
        return has;
	}
	public boolean hasPermOnServer(String perm, String server) 
	{
		List<String> perms=getEffectivePerms(server);
        
        Boolean has=BungeePerms.getInstance().getPermissionsManager().getResolver().has(perms, perm);
		
        has=checkSuperPerms(has, perm);
		
        return has;
	}
    public boolean hasPermOnServerInWorld(String perm, String server, String world) 
	{
		List<String> perms=getEffectivePerms(server,world);
		        
        Boolean has=BungeePerms.getInstance().getPermissionsManager().getResolver().has(perms, perm);
        
        has=checkSuperPerms(has, perm);
		
        return has;
	}
	
	public List<String> getEffectivePerms()
	{
        List<String> effperms=cachedPerms.get("global");
        if(effperms==null)
        {
            effperms=calcEffectivePerms();
            cachedPerms.put("global", effperms);
        }
        
        return effperms;
    }
    public List<String> getEffectivePerms(String server) 
	{
        List<String> effperms=cachedPerms.get(server.toLowerCase());
        if(effperms==null)
        {
            effperms=calcEffectivePerms(server);
            cachedPerms.put(server.toLowerCase(), effperms);
        }
        
        return effperms;
    }
    public List<String> getEffectivePerms(String server, String world) 
	{
        List<String> effperms=cachedPerms.get(server.toLowerCase()+";"+world.toLowerCase());
        if(effperms==null)
        {
            effperms=calcEffectivePerms(server,world);
            cachedPerms.put(server.toLowerCase()+";"+world.toLowerCase(), effperms);
        }
        
        return effperms;
    }
    
	public List<String> calcEffectivePerms()
	{
		List<String> ret=new ArrayList<>();
		for(Group g:groups)
		{
			List<String> gperms=g.getEffectivePerms();
            ret.addAll(gperms);
		}
        ret.addAll(extraPerms);
        
        ret=BungeePerms.getInstance().getPermissionsManager().getResolver().simplify(ret);
        
		return ret;
	}
	public List<String> calcEffectivePerms(String server)
	{
		List<String> ret=new ArrayList<>();
		for(Group g:groups)
		{
			List<String> gperms=g.getEffectivePerms(server);
			ret.addAll(gperms);
		}
		ret.addAll(extraPerms);
		
		//per server perms
		List<String> perserverPerms=serverPerms.get(server.toLowerCase());
		if(perserverPerms!=null)
		{
			ret.addAll(perserverPerms);
		}
        
        ret=BungeePerms.getInstance().getPermissionsManager().getResolver().simplify(ret);
		
		return ret;
	}
	public List<String> calcEffectivePerms(String server, String world)
	{
		List<String> ret=new ArrayList<>();
		for(Group g:groups)
		{
			List<String> gperms=g.getEffectivePerms(server,world);
			ret.addAll(gperms);
		}
		
		ret.addAll(extraPerms);
		
		//per server perms
		List<String> perserverPerms=serverPerms.get(server.toLowerCase());
		if(perserverPerms!=null)
		{
			ret.addAll(perserverPerms);
		}
        
        //per server world perms
        Map<String,List<String>> serverPerms=serverWorldPerms.get(server.toLowerCase());
        if(serverPerms!=null)
        {
            List<String> serverWorldPerms=serverPerms.get(world.toLowerCase());
            if(serverWorldPerms!=null)
            {
                ret.addAll(serverWorldPerms);
            }
        }
        
        ret=BungeePerms.getInstance().getPermissionsManager().getResolver().simplify(ret);
		
		return ret;
	}

    public void recalcPerms() 
    {
        for(Map.Entry<String, List<String>> e:cachedPerms.entrySet())
        {
            String where=e.getKey();
            List<String> l=Statics.toList(where, ";");
            String server=l.get(0);
            
            if(l.size()==1)
            {
                if(server.equalsIgnoreCase("global"))
                {
                    cachedPerms.put("global", calcEffectivePerms());
                }
                else
                {
                    List<String> effperms=calcEffectivePerms(server);
                    cachedPerms.put(server.toLowerCase(), effperms);
                }
            }
            else if(l.size()==2)
            {
                String world=l.get(1);
                
                recalcPerms(server,world);
            }
        }
    }
    public void recalcPerms(String server)
    {
        for(Map.Entry<String, List<String>> e:cachedPerms.entrySet())
        {
            String where=e.getKey();
            List<String> l=Statics.toList(where, ";");
            String lserver=l.get(0);
            
            if(lserver.equalsIgnoreCase(server))
            {
                if(l.size()==1)
                {
                    List<String> effperms=calcEffectivePerms(server);
                    cachedPerms.put(server.toLowerCase(), effperms);
                }
                else if(l.size()==2)
                {
                    String world=l.get(1);
                    recalcPerms(server,world);
                }
            }
        }
    }
    public void recalcPerms(String server,String world)
    {
        List<String> effperms=calcEffectivePerms(server,world);
        cachedPerms.put(server.toLowerCase()+";"+world.toLowerCase(), effperms);
    }
    
    public boolean isNothingSpecial() 
    {
        for(Group g:groups)
        {
            if(!g.isDefault())
            {
                return false;
            }
        }
        return serverWorldPerms.isEmpty()&serverPerms.isEmpty()&extraPerms.isEmpty();
    }

    public Group getGroupByLadder(String ladder) 
    {
        for(Group g:groups)
        {
            if(g.getLadder().equalsIgnoreCase(ladder))
            {
                return g;
            }
        }
        return null;
    }
    
    private boolean checkSuperPerms(Boolean has, String perm)
    {
        if(has!=null)
        {
            return has;
        }
        
        has=false;
        if(BungeePerms.getInstance().getPermissionsManager().isSuperpermscompat())
        {
            Player p=BungeePerms.getInstance().getPermissionsManager().isUseUUIDs() ? Bukkit.getServer().getPlayer(UUID) : Bukkit.getServer().getPlayer(name);
            if(p!=null)
            {
                PermissibleBase base=Injector.getPermissible(p);
                if(base instanceof Permissible)
                {
                    Permissible permissible=(Permissible) base;
                    has=permissible.hasSuperPerm(perm);
                }
            }
        }
        
        return has;
    }
}
