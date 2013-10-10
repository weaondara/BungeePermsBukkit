package net.alpenblock.bungeeperms.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * The Class Config.
 */
public class Config 
{
	
	/** The fconfig. */
	private FileConfiguration fconfig;
	
	/** The path. */
	private String path;
	
	/**
	 * Instantiates a new config.
	 *
	 * @param p the p
	 * @param path the path
	 */
	public Config (Plugin p,String path) 
	{
		this.path=p.getDataFolder()+path;
        createFile();
        fconfig = YamlConfiguration.loadConfiguration(new File(this.path));
	}
	
	/**
	 * Load.
	 */
	public void load()
    {
		createFile();
		try 
        {
            fconfig.load(path);
		} 
        catch (Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Save.
	 */
	public void save() 
    {
		createFile();
		try 
        {
            fconfig.save(path);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	/**
	 * Creates the file.
	 */
	public void createFile()
	{
		File file=new File(path);
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
			try 
			{
				file.createNewFile();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Gets the string.
	 *
	 * @param key the key
	 * @param def the def
	 * @return the string
	 */
	public String getString(String key, String def) {
		
		if(fconfig.contains(key)) {
			return fconfig.getString(key);
		}
		else {		
			fconfig.set(key, def);
			try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
			return def;
		}
	}
	
	/**
	 * Gets the int.
	 *
	 * @param key the key
	 * @param def the def
	 * @return the int
	 */
	public int getInt(String key, int def) {
		
		if(fconfig.contains(key)) {
			return fconfig.getInt(key);
		}
		else {
			fconfig.set(key, def);
			try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
			return def;
		}
			
	}
	
	/**
	 * Gets the boolean.
	 *
	 * @param key the key
	 * @param def the def
	 * @return the boolean
	 */
	public boolean getBoolean(String key, boolean def) {
		
		if(fconfig.contains(key)) {
			return fconfig.getBoolean(key);
		}
		else {
			fconfig.set(key, def);
			try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
			return def;
		}
			
	}
	public <T extends Enum> T getEnumValue(String key, T def)
	{
		if(fconfig.contains(key)) 
		{
            String s=getString(key,def.name());
            T[] constants=(T[]) def.getDeclaringClass().getEnumConstants();
            for(T constant:constants)
            {
                if(constant.name().equals(s))
                {
                    return constant;
                }
            }
			return def;
		}
		else 
		{
			load();
			fconfig.set(key, def.name());
			save();
			return def;
		}
	}
	/**
	 * Gets the list string.
	 *
	 * @param key the key
	 * @param def the def
	 * @return the list string
	 */
	public List<String> getListString(String key, List<String> def) {
		
		if(fconfig.contains(key)) {
			return fconfig.getStringList(key);
		}
		else {
			fconfig.set(key, def);
			try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
			return def;
		}
			
	}
	
	/**
	 * Gets the double.
	 *
	 * @param key the key
	 * @param def the def
	 * @return the double
	 */
	public double getDouble(String key, double def)
	{
		if(fconfig.contains(key)) {
			return fconfig.getDouble(key);
		}
		else {
			fconfig.set(key, def);
			try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
			return def;
		}
	}
	
	/**
	 * Sets the string.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setString(String key, String val) {
		fconfig.set(key, val);
	}
	
	/**
	 * Sets the string.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setStringAndSave(String key, String val) {
		fconfig.set(key, val);
		try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Sets the int.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setInt(String key, int val)
    {
		fconfig.set(key, val);
	}
	
	/**
	 * Sets the int.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setIntAndSave(String key, int val) {
		fconfig.set(key, val);
		try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Sets the bool.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setBool(String key, boolean val) {
		fconfig.set(key, val);
	}
	
	/**
	 * Sets the bool.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setBoolAndSave(String key, boolean val) {
		fconfig.set(key, val);
		try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
	}
    
	public <T extends Enum> void setEnumValue(String key, T val) 
	{
		fconfig.set(key, val.name());
	}
    public <T extends Enum> void setEnumAndSave(String key, T val) 
	{
		fconfig.set(key, val.name());
		try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
	}
	/**
	 * Sets the list string.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setListString(String key, List<String> val) {
		fconfig.set(key, val);
	}
	
	/**
	 * Sets the list string.
	 *
	 * @param key the key
	 * @param val the val
	 */
	public void setListStringAndSave(String key, List<String> val) {
		fconfig.set(key, val);
		try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
	}
	
	/**
	 * Gets the sub nodes.
	 *
	 * @param node the node
	 * @return the sub nodes
	 */
	public List<String> getSubNodes(String node)
	{
		List<String> ret=new ArrayList<String>();
		try
		{
			for(Object o:fconfig.getConfigurationSection(node).getKeys(false).toArray())
			{
				ret.add((String) o);
			}
		}
		catch(Exception e){}
		return ret;
	}
	
	/**
	 * Delete node.
	 *
	 * @param node the node
	 */
	public void deleteNode(String node)
	{
		fconfig.set(node, null);
		try { fconfig.save(path); } catch (IOException e) { e.printStackTrace(); }
	}
    
    public boolean keyExists(String node)
    {
        return fconfig.contains(node);
    }
}
