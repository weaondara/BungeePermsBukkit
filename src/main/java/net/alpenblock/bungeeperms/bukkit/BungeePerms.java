package net.alpenblock.bungeeperms.bukkit;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

public class BungeePerms extends JavaPlugin
{
    private static BungeePerms instance;
	public static BungeePerms getInstance()
	{
		return instance;
	}
	
	private Server server;
	private Config config;
	private Debug debug;
    
	private PermissionsManager pm;
    
    private String servername;
	
	@Override
	public void onLoad()
	{
		//static
		instance=this;
		
		server=getServer();
        
        config=new Config(this,"/config.yml");
        config.load();
        loadConfig();
        debug=new Debug(this,config,"BP");
        
		pm=new PermissionsManager(this,config,debug);
	}
	private void loadConfig()
    {
        servername=config.getString("servername", "servername");
    }
	@Override
	public void onEnable()
	{
		server.getLogger().info("Activating BungeePerms ...");
        pm.enable();
	}
	
	@Override
	public void onDisable() 
	{
		server.getLogger().info("Deactivating BungeePerms ...");
        pm.disable();
	}
    public PermissionsManager getPermissionsManager()
	{
		return pm;
	}

    public String getServerName()
    {
        return servername;
    }
}
