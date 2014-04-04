package net.alpenblock.bungeeperms.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class Group.
 */
public class Group implements Comparable<Group>
{
    private Map<String,List<String>> cachedPerms;
    
	private String name;
	private List<String> inheritances;
	private List<String> perms;
	private Map<String,Server> servers;
	private int rank;
	private int weight;
    private String ladder;
	private boolean isdefault;
	private String display;
	private String prefix;
	private String suffix;

    public Group(String name, List<String> inheritances, List<String> perms, Map<String, Server> servers, int rank, int weight, String ladder, boolean isdefault, String display, String prefix, String suffix)
    {
        this.name = name;
        this.inheritances = inheritances;
        this.perms = perms;
        this.servers = servers;
        this.rank = rank;
        this.weight = weight;
        this.ladder = ladder;
        this.isdefault = isdefault;
        this.display = display;
        this.prefix = prefix;
        this.suffix = suffix;
        
        cachedPerms=new HashMap<>();
    }
	
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<String> getInheritances()
    {
        return inheritances;
    }

    public void setInheritances(List<String> inheritances)
    {
        this.inheritances = inheritances;
    }

    public List<String> getPerms()
    {
        return perms;
    }

    public void setPerms(List<String> perms)
    {
        this.perms = perms;
    }

    public Map<String, Server> getServers()
    {
        return servers;
    }

    public void setServers(Map<String, Server> servers)
    {
        this.servers = servers;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int rank)
    {
        this.rank = rank;
    }

    public int getWeight()
    {
        return weight;
    }

    public void setWeight(int weight)
    {
        this.weight = weight;
    }

    public String getLadder()
    {
        return ladder;
    }

    public void setLadder(String ladder)
    {
        this.ladder = ladder;
    }

    public boolean isDefault()
    {
        return isdefault;
    }

    public void setIsdefault(boolean isdefault)
    {
        this.isdefault = isdefault;
    }

    public String getDisplay()
    {
        return display;
    }

    public void setDisplay(String display)
    {
        this.display = display;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public String getSuffix()
    {
        return suffix;
    }

    public void setSuffix(String suffix)
    {
        this.suffix = suffix;
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
    public List<String> getEffectivePerms(String server,String world) 
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
		for(Group g:BungeePerms.getInstance().getPermissionsManager().getGroups())
		{
			if(inheritances.contains(g.getName()))
			{
				List<String> gperms=g.getEffectivePerms();
				for(String perm:gperms)
				{
					boolean added=false;
					for(int i=0;i<ret.size();i++)
					{
						if(ret.get(i).equalsIgnoreCase(perm))
						{
							added=true;
							break;
						}
						else if(ret.get(i).equalsIgnoreCase("-"+perm))
						{
							ret.set(i,perm);
							added=true;
							break;
						}
						else if(perm.equalsIgnoreCase("-"+ret.get(i)))
						{
							ret.remove(i);
							added=true;
							break;
						}
					}
					if(!added)
					{
						ret.add(perm);
					}
				}
			}
		}
		for(String s:perms)
		{
			boolean added=false;
			for(int i=0;i<ret.size();i++)
			{
				if(ret.get(i).equalsIgnoreCase(s))
				{
					added=true;
					break;
				}
				else if(ret.get(i).equalsIgnoreCase("-"+s))
				{
					ret.set(i,s);
					added=true;
					break;
				}
				else if(s.equalsIgnoreCase("-"+ret.get(i)))
				{
					ret.remove(i);
					added=true;
					break;
				}
			}
			if(!added)
			{
				ret.add(s);
			}
		}
		return ret;
	}
	public List<String> calcEffectivePerms(String server) 
	{
		List<String> ret=new ArrayList<>();
		for(Group g:BungeePerms.getInstance().getPermissionsManager().getGroups())
		{
			if(inheritances.contains(g.getName()))
			{
				List<String> gperms=g.getEffectivePerms(server);
				for(String perm:gperms)
				{
					boolean added=false;
					for(int i=0;i<ret.size();i++)
					{
						if(ret.get(i).equalsIgnoreCase(perm))
						{
							added=true;
							break;
						}
						else if(ret.get(i).equalsIgnoreCase("-"+perm))
						{
							ret.set(i,perm);
							added=true;
							break;
						}
						else if(perm.equalsIgnoreCase("-"+ret.get(i)))
						{
							ret.remove(i);
							added=true;
							break;
						}
					}
					if(!added)
					{
						ret.add(perm);
					}
				}
			}
			
			//per server perms
//            Server srv=g.getServers().get(server);
//			if(srv==null)
//			{
//				srv=new Server(server,new ArrayList<String>(),new HashMap<String,World>(),"","","");
//			}
//			List<String> serverperms=srv.getPerms();
//			for(String perm:serverperms)
//			{
//				boolean added=false;
//				for(int i=0;i<ret.size();i++)
//				{
//					if(ret.get(i).equalsIgnoreCase(perm))
//					{
//						added=true;
//						break;
//					}
//					else if(ret.get(i).equalsIgnoreCase("-"+perm))
//					{
//						ret.set(i,perm);
//						added=true;
//						break;
//					}
//					else if(perm.equalsIgnoreCase("-"+ret.get(i)))
//					{
//						ret.remove(i);
//						added=true;
//						break;
//					}
//				}
//				if(!added)
//				{
//					ret.add(perm);
//				}
//			}
		}
		
		for(String s:perms)
		{
			boolean added=false;
			for(int i=0;i<ret.size();i++)
			{
				if(ret.get(i).equalsIgnoreCase(s))
				{
					added=true;
					break;
				}
				else if(ret.get(i).equalsIgnoreCase("-"+s))
				{
					ret.set(i,s);
					added=true;
					break;
				}
				else if(s.equalsIgnoreCase("-"+ret.get(i)))
				{
					ret.remove(i);
					added=true;
					break;
				}
			}
			if(!added)
			{
				ret.add(s);
			}
		}
		
		//per server perms
        Server srv=servers.get(server.toLowerCase());
        if(srv==null)
        {
            srv=new Server(server.toLowerCase(),new ArrayList<String>(),new HashMap<String,World>(),"","","");
        }
        List<String> perserverperms=srv.getPerms();
		for(String perm:perserverperms)
		{
			boolean added=false;
			for(int i=0;i<ret.size();i++)
			{
				if(ret.get(i).equalsIgnoreCase(perm))
				{
					added=true;
					break;
				}
				else if(ret.get(i).equalsIgnoreCase("-"+perm))
				{
					ret.set(i,perm);
					added=true;
					break;
				}
				else if(perm.equalsIgnoreCase("-"+ret.get(i)))
				{
					ret.remove(i);
					added=true;
					break;
				}
			}
			if(!added)
			{
				ret.add(perm);
			}
		}
		
		return ret;
	}
	public List<String> calcEffectivePerms(String server,String world) 
	{
		List<String> ret=new ArrayList<>();
		for(Group g:BungeePerms.getInstance().getPermissionsManager().getGroups())
		{
			if(inheritances.contains(g.getName()))
			{
				for(String perm:g.getEffectivePerms(server,world))
				{
					boolean added=false;
					for(int i=0;i<ret.size();i++)
					{
						if(ret.get(i).equalsIgnoreCase(perm))
						{
							added=true;
							break;
						}
						else if(ret.get(i).equalsIgnoreCase("-"+perm))
						{
							ret.set(i,perm);
							added=true;
							break;
						}
						else if(perm.equalsIgnoreCase("-"+ret.get(i)))
						{
							ret.remove(i);
							added=true;
							break;
						}
					}
					if(!added)
					{
						ret.add(perm);
					}
				}
			
                //per server perms
//                Server srv=g.getServers().get(server);
//                if(srv==null)
//                {
//                    srv=new Server(server,new ArrayList<String>(),new HashMap<String,World>(),"","","");
//                }
//                List<String> serverperms=srv.getPerms();
//                for(String perm:serverperms)
//                {
//                    boolean added=false;
//                    for(int i=0;i<ret.size();i++)
//                    {
//                        if(ret.get(i).equalsIgnoreCase(perm))
//                        {
//                            added=true;
//                            break;
//                        }
//                        else if(ret.get(i).equalsIgnoreCase("-"+perm))
//                        {
//                            ret.set(i,perm);
//                            added=true;
//                            break;
//                        }
//                        else if(perm.equalsIgnoreCase("-"+ret.get(i)))
//                        {
//                            ret.remove(i);
//                            added=true;
//                            break;
//                        }
//                    }
//                    if(!added)
//                    {
//                        ret.add(perm);
//                    }
//                }
//                //per server world perms
//                World w=srv.getWorlds().get(world);
//                if(w==null)
//                {
//                    w=new World(server,new ArrayList<String>(),"","","");
//                }
//                List<String> serverworldperms=w.getPerms();
//                for(String perm:serverworldperms)
//                {
//                    boolean added=false;
//                    for(int i=0;i<ret.size();i++)
//                    {
//                        if(ret.get(i).equalsIgnoreCase(perm))
//                        {
//                            added=true;
//                            break;
//                        }
//                        else if(ret.get(i).equalsIgnoreCase("-"+perm))
//                        {
//                            ret.set(i,perm);
//                            added=true;
//                            break;
//                        }
//                        else if(perm.equalsIgnoreCase("-"+ret.get(i)))
//                        {
//                            ret.remove(i);
//                            added=true;
//                            break;
//                        }
//                    }
//                    if(!added)
//                    {
//                        ret.add(perm);
//                    }
//                }
            }
		}
		
		for(String s:perms)
		{
			boolean added=false;
			for(int i=0;i<ret.size();i++)
			{
				if(ret.get(i).equalsIgnoreCase(s))
				{
					added=true;
					break;
				}
				else if(ret.get(i).equalsIgnoreCase("-"+s))
				{
					ret.set(i,s);
					added=true;
					break;
				}
				else if(s.equalsIgnoreCase("-"+ret.get(i)))
				{
					ret.remove(i);
					added=true;
					break;
				}
			}
			if(!added)
			{
				ret.add(s);
			}
		}
		
		//per server perms
        Server srv=servers.get(server.toLowerCase());
        if(srv==null)
        {
            srv=new Server(server.toLowerCase(),new ArrayList<String>(),new HashMap<String,World>(),"","","");
        }
        List<String> perserverperms=srv.getPerms();
		for(String perm:perserverperms)
		{
			boolean added=false;
			for(int i=0;i<ret.size();i++)
			{
				if(ret.get(i).equalsIgnoreCase(perm))
				{
					added=true;
					break;
				}
				else if(ret.get(i).equalsIgnoreCase("-"+perm))
				{
					ret.set(i,perm);
					added=true;
					break;
				}
				else if(perm.equalsIgnoreCase("-"+ret.get(i)))
				{
					ret.remove(i);
					added=true;
					break;
				}
			}
			if(!added)
			{
				ret.add(perm);
			}
		}
        
        //per server world perms
        World w=srv.getWorlds().get(world.toLowerCase());
        if(w==null)
        {
            w=new World(world.toLowerCase(),new ArrayList<String>(),"","","");
        }
        List<String> serverworldperms=w.getPerms();
        for(String perm:serverworldperms)
        {
            boolean added=false;
            for(int i=0;i<ret.size();i++)
            {
                if(ret.get(i).equalsIgnoreCase(perm))
                {
                    added=true;
                    break;
                }
                else if(ret.get(i).equalsIgnoreCase("-"+perm))
                {
                    ret.set(i,perm);
                    added=true;
                    break;
                }
                else if(perm.equalsIgnoreCase("-"+ret.get(i)))
                {
                    ret.remove(i);
                    added=true;
                    break;
                }
            }
            if(!added)
            {
                ret.add(perm);
            }
        }
		
		return ret;
	}
	
	public boolean has(String perm) 
	{
		List<String> perms=getEffectivePerms();
		boolean has=false;
		for(String p:perms)
		{
			if(p.equalsIgnoreCase(perm))
			{
				has=true;
			}
			else if(p.equalsIgnoreCase("-"+perm))
			{
				has=false;
			}
			else if(p.endsWith("*"))
			{
				List<String> lp=Statics.toList(p, ".");
				List<String> lperm=Statics.toList(perm, ".");
				int index=0;
				try
				{
					while(true)
					{
						if(index==0)
						{
							if( lperm.get(0).equalsIgnoreCase(lp.get(0))|
								lp.get(0).equalsIgnoreCase("-"+lperm.get(0)))
							{
								index++;
							}
							else
							{
								break;
							}
						}
						else
						{
							if(lperm.get(index).equalsIgnoreCase(lp.get(index)))
							{
								index++;
							}
							else
							{
								break;
							}
						}
					}
					if(lp.get(index).equalsIgnoreCase("*"))
					{
						has=!lp.get(0).startsWith("-");
					}
				}
				catch(Exception e){}
			}
		}
		return has;
	}
	public boolean hasOnServer(String perm,String server) 
	{
		List<String> perms=getEffectivePerms(server);
		boolean has=false;
		for(String p:perms)
		{
			if(p.equalsIgnoreCase(perm))
			{
				has=true;
			}
			else if(p.equalsIgnoreCase("-"+perm))
			{
				has=false;
			}
			else if(p.endsWith("*"))
			{
				List<String> lp=Statics.toList(p, ".");
				List<String> lperm=Statics.toList(perm, ".");
				int index=0;
				try
				{
					while(true)
					{
						if(index==0)
						{
							if( lperm.get(0).equalsIgnoreCase(lp.get(0))|
								lp.get(0).equalsIgnoreCase("-"+lperm.get(0)))
							{
								index++;
							}
							else
							{
								break;
							}
						}
						else
						{
							if(lperm.get(index).equalsIgnoreCase(lp.get(index)))
							{
								index++;
							}
							else
							{
								break;
							}
						}
					}
					if(lp.get(index).equalsIgnoreCase("*"))
					{
						has=!lp.get(0).startsWith("-");
					}
				}
				catch(Exception e){}
			}
		}
		return has;
	}
	public boolean hasOnServerInWorld(String perm,String server,String world) 
	{
		List<String> perms=getEffectivePerms(server,world);
		boolean has=false;
		for(String p:perms)
		{
			if(p.equalsIgnoreCase(perm))
			{
				has=true;
			}
			else if(p.equalsIgnoreCase("-"+perm))
			{
				has=false;
			}
			else if(p.endsWith("*"))
			{
				List<String> lp=Statics.toList(p, ".");
				List<String> lperm=Statics.toList(perm, ".");
				int index=0;
				try
				{
					while(true)
					{
						if(index==0)
						{
							if( lperm.get(0).equalsIgnoreCase(lp.get(0))|
								lp.get(0).equalsIgnoreCase("-"+lperm.get(0)))
							{
								index++;
							}
							else
							{
								break;
							}
						}
						else
						{
							if(lperm.get(index).equalsIgnoreCase(lp.get(index)))
							{
								index++;
							}
							else
							{
								break;
							}
						}
					}
					if(lp.get(index).equalsIgnoreCase("*"))
					{
						has=!lp.get(0).startsWith("-");
					}
				}
				catch(Exception e){}
			}
		}
		return has;
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

    @Override
    public int compareTo(Group g)
    {
        return -Integer.compare(rank, g.getRank());
    }
}
