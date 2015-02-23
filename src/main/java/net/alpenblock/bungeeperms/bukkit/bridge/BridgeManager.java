package net.alpenblock.bungeeperms.bukkit.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.alpenblock.bungeeperms.bukkit.BungeePerms;
import net.alpenblock.bungeeperms.bukkit.bridge.bridges.essentials.EssentialsBridge;
import net.alpenblock.bungeeperms.bukkit.bridge.bridges.vault.VaultBridge;
import net.alpenblock.bungeeperms.bukkit.bridge.bridges.worldedit.WorldEditBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class BridgeManager implements Listener
{

    private static BridgeManager instance;

    public static BridgeManager getInstance()
    {
        return instance;
    }

    private Map<Class<? extends Bridge>, String> brigdesmap;
    private List<Bridge> bridges;

    public void onLoad()
    {
        instance = this;

        brigdesmap = new HashMap<>();
        bridges = new ArrayList<>();

        brigdesmap.put(WorldEditBridge.class, "com.sk89q.worldedit.bukkit.WorldEditPlugin");
        brigdesmap.put(VaultBridge.class, "net.milkbowl.vault.Vault");
        brigdesmap.put(EssentialsBridge.class, "com.earth2me.essentials.Essentials");

        for (Map.Entry<Class<? extends Bridge>, String> entry : brigdesmap.entrySet())
        {
            createBridge(entry.getKey(), entry.getValue());
        }
    }

    public void onEnable()
    {
        for (Bridge b : bridges)
        {
            b.enable();
        }
        Bukkit.getPluginManager().registerEvents(this, BungeePerms.getInstance());
    }

    public void onDisable()
    {
        PluginEnableEvent.getHandlerList().unregister((Listener) this);
        for (Bridge b : bridges)
        {
            b.disable();
        }
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if(cmd.getName().equalsIgnoreCase("bungeepermsbukkitbridge"))
        {
            if(!(sender instanceof ConsoleCommandSender))
            {
                sender.sendMessage(ChatColor.DARK_RED + "Only console can do that!");
                return true;
            }
            if(args.length == 1 && args[0].equalsIgnoreCase("reload"))
            {
                onDisable();
                onEnable();
            }
            return true;
        }
        return false;
    }

    public Bridge createBridge(Class<? extends Bridge> c, String classname)
    {
        try
        {
            Class.forName(classname);
            Bridge b = c.newInstance();
            bridges.add(b);
            return b;
        }
        catch (Exception ex)
        {
        }
        return null;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e)
    {
        for (Map.Entry<Class<? extends Bridge>, String> entry : brigdesmap.entrySet())
        {
            try
            {
                Class.forName(entry.getValue());
                for (Bridge b : bridges)
                {
                    if (b.getClass().getName().equals(entry.getKey().getName()))
                    {
                        throw new Exception();
                    }
                }
                createBridge(entry.getKey(), entry.getValue()).enable();
            }
            catch (Exception ex)
            {
            }
        }
    }
}
