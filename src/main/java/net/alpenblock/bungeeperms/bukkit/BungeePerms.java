package net.alpenblock.bungeeperms.bukkit;

import lombok.Getter;
import net.alpenblock.bungeeperms.bukkit.bridge.BridgeManager;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class BungeePerms extends JavaPlugin
{

    private static BungeePerms instance;

    public static BungeePerms getInstance()
    {
        return instance;
    }

    //todo: better integration
    @Getter
    private BridgeManager bridge;

    private Server server;
    private Config config;
    private Debug debug;

    private PermissionsManager pm;

    private String servername;
    private boolean allowops;

    //todo: better integration
    public BungeePerms()
    {
        bridge = new BridgeManager();
    }

    @Override
    public void onLoad()
    {
        //static
        instance = this;

        server = getServer();

        config = new Config(this, "/config.yml");
        config.load();
        loadConfig();
        debug = new Debug(this, config, "BP");

        pm = new PermissionsManager(this, config, debug);

        //todo: better integration
        bridge.onLoad();
    }

    public void loadConfig()
    {
        servername = config.getString("servername", "servername");
        allowops = config.getBoolean("allowops", false);
    }

    @Override
    public void onEnable()
    {
        server.getLogger().info("Activating BungeePerms ...");
        pm.enable();

        //todo: better integration
        bridge.onEnable();
    }

    @Override
    public void onDisable()
    {
        server.getLogger().info("Deactivating BungeePerms ...");
        pm.disable();

        //todo: better integration
        bridge.onDisable();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        //impl cmds some time
        
        return bridge.onCommand(sender, cmd, label, args);
    }

    public PermissionsManager getPermissionsManager()
    {
        return pm;
    }

    public String getServerName()
    {
        return servername;
    }

    public boolean isAllowOps()
    {
        return allowops;
    }
}
