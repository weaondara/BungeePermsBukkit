/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.alpenblock.bungeeperms.bukkit;

import java.lang.reflect.Field;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

/**
 *
 * @author alex
 */
public class Injector 
{
    public static org.bukkit.permissions.Permissible inject(Player player, org.bukkit.permissions.Permissible newpermissible) 
    {
        try
        {
            Field perm = Class.forName(getVersionedClassName("entity.CraftHumanEntity")).getDeclaredField("perm");
            perm.setAccessible(true);
            org.bukkit.permissions.Permissible oldpermissible = (org.bukkit.permissions.Permissible) perm.get(player);
            if (newpermissible instanceof PermissibleBase) 
            {
                //copy attachments
                Field attachments = PermissibleBase.class.getDeclaredField("attachments");
                attachments.setAccessible(true);
                ((List) attachments.get(newpermissible)).addAll((List)attachments.get(oldpermissible));
            }

            // inject permissible
            perm.set(player, newpermissible);
            return oldpermissible;
        }
        catch (Exception e) {e.printStackTrace();}
		return null;
	}
    public static org.bukkit.permissions.Permissible uninject(Player player) 
    {
        try
        {
            Field perm = Class.forName(getVersionedClassName("entity.CraftHumanEntity")).getDeclaredField("perm");
            perm.setAccessible(true);
            org.bukkit.permissions.Permissible permissible = (org.bukkit.permissions.Permissible) perm.get(player);
            if (permissible instanceof Permissible) 
            {
                perm.set(player, ((Permissible)permissible).getOldPermissible());
                return (Permissible)permissible;
            }

            return null;
        }
        catch (Exception e) {e.printStackTrace();}
		return null;
	}
    
	private static String getVersionedClassName(String classname) 
    {
        String version;
        
        Class serverClass = Bukkit.getServer().getClass();
		if (!serverClass.getSimpleName().equals("CraftServer")) 
        {
			return null;
		} 
        else if (serverClass.getName().equals("org.bukkit.craftbukkit.CraftServer")) 
        {
			version = ".";
		} 
        else 
        {
			version = serverClass.getName().substring("org.bukkit.craftbukkit".length());
			version = version.substring(0, version.length() - "CraftServer".length());
		}

		return "org.bukkit.craftbukkit" + version + classname;
	}
}
