package net.alpenblock.bungeeperms.bukkit;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 *
 * @author alex
 */
public class Permissible extends PermissibleBase
{
	private Player player;
    private Map<String, PermissionAttachmentInfo> permissions;
	private org.bukkit.permissions.Permissible oldpermissible = null;
    
    public Permissible(Player player) 
    {
		super(player);
		this.player = player;
		permissions = new LinkedHashMap<String, PermissionAttachmentInfo>() {
			@Override
			public PermissionAttachmentInfo put(String k, PermissionAttachmentInfo v) {
				PermissionAttachmentInfo existing = this.get(k);
				if (existing != null) 
                {
					return existing;
				}
				return super.put(k, v);
			}
		};
        
        Statics.setField(this, permissions, "permissions");
	}

	public org.bukkit.permissions.Permissible getOldPermissible()
    {
		return oldpermissible;
	}
	public void setOldPermissible(org.bukkit.permissions.Permissible oldPermissible)
    {
		this.oldpermissible = oldPermissible;
	}

	@Override
	public boolean hasPermission(String permission) 
    {
        boolean res=BungeePerms.getInstance().getPermissionsManager().hasPermOnServerInWorld(player.getName(), permission, BungeePerms.getInstance().getServerName(), player.getWorld().getName());
        //System.out.println(player+" has perm "+permission+"="+res);
		return res;
	}
	@Override
	public boolean hasPermission(Permission permission)
    {
		return hasPermission(permission.getName());
	}
	@Override
	public void recalculatePermissions() 
    {
        clearPermissions();
    }

	@Override
	public boolean isPermissionSet(String permission)
    {
		return true;
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
    {
		return new LinkedHashSet<>(permissions.values());
	}
}