package net.alpenblock.bungeeperms.bukkit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The Class Statics.
 */
public class Statics {
	
	/**
	 * Count sequences.
	 *
	 * @param s the s
	 * @param seq the seq
	 * @return the int
	 */
	public static int countSequences(String s, String seq)
	{
		int count=0;
		for(int i=0;i<s.length()-seq.length()+1;i++)
		{
			if(s.substring(i, i+seq.length()).equalsIgnoreCase(seq))
			{
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Gets the full player name.
	 *
	 * @param s the s
	 * @param player the player
	 * @return the full player name
	 */
	public static String getFullPlayerName(String player)
	{
		Player p = Bukkit.getPlayer(player);
		if(p!=null) 
		{
			for(Player pp:Bukkit.getOnlinePlayers())
			{
				if(pp.getName().startsWith(player))
				{
					return pp.getName();
				}
			}
			return p.getName();
		} 
		else 
		{
			return player;
		}
	}
	
	/**
	 * To list.
	 *
	 * @param s the s
	 * @param seperator the seperator
	 * @return the list
	 */
	public static List<String> toList(String s,String seperator)
	{
		List<String> l=new ArrayList<>();
		String ls="";
		for(int i=0;i<(s.length()-seperator.length())+1;i++)
		{
			if(s.substring(i, i+seperator.length()).equalsIgnoreCase(seperator))
			{
				l.add(ls);
				ls="";
				i=i+seperator.length()-1;
			}
			else
			{
				ls+=s.substring(i,i+1);
			}
		}
		if(ls.length()>0)
		{		
			l.add(ls);
		}
		return l;
	}
	
	/**
	 * Arg alias.
	 *
	 * @param arg the arg
	 * @param aliases the aliases
	 * @return true, if successful
	 */
	public static boolean ArgAlias(String arg,String[] aliases)
	{
		for(int i=0;i<aliases.length;i++)
		{
			if(aliases[i].equalsIgnoreCase(arg))
			{
				return true;
			}
		}
		return false;
	}
    
    public static <T> T replaceField(Object instance,T var,String varname) 
    {
        try 
        {
            Field f=instance.getClass().getDeclaredField(varname);
            f.setAccessible(true);
            T old=(T) f.get(instance);
            f.set(instance, var);
            return old;
        } 
        catch (Exception ex) 
        {
            return null;
        }
    }
    public static <T> T getField(Object instance,Class<T> type,String varname) 
    {
        try 
        {
            Field f=instance.getClass().getDeclaredField(varname);
            f.setAccessible(true);
            T old=(T) f.get(instance);
            return old;
        } 
        catch (Exception ex) 
        {
            return null;
        }
    }
    public static void setField(Object instance,Object var,String varname) 
    {
        try 
        {
            Field f=instance.getClass().getDeclaredField(varname);
            f.setAccessible(true);
            f.set(instance, var);
        } 
        catch (Exception ex) 
        { }
    }
    public static void setField(Class clazz, Object instance,Object var,String varname) 
    {
        try 
        {
            Field f=clazz.getDeclaredField(varname);
            f.setAccessible(true);
            f.set(instance, var);
        } 
        catch (Exception ex) 
        { }
    }
    
    public static UUID parseUUID(String s)
    {
        try
        {
            return UUID.fromString(s);
        }
        catch(Exception e) {}
        
        if(s.length()==32)
        {
            s=s.substring(0,8)+"-"+
                    s.substring(8,12)+"-"+
                    s.substring(12,16)+"-"+
                    s.substring(16,20)+"-"+
                    s.substring(20,32)+"-";
            try
            {
                return UUID.fromString(s);
            }
            catch(Exception e) {}
        }
        
        return null;
    }
}
