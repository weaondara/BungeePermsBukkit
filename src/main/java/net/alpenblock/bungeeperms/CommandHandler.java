package net.alpenblock.bungeeperms;

import lombok.AllArgsConstructor;
import net.alpenblock.bungeeperms.platform.PlatformPlugin;
import net.alpenblock.bungeeperms.platform.Sender;

@AllArgsConstructor
public class CommandHandler
{

    protected PlatformPlugin plugin;
    protected PermissionsChecker checker;
    protected BPConfig config;

    public boolean onCommand(Sender sender, String cmd, String label, String[] args)
    {
        return false;
    }

}
