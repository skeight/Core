package com.iConomy;

import java.io.File;
import java.util.Locale;

import com.nijikokun.bukkit.Permissions.Permissions;

import com.iConomy.command.Handler;
import com.iConomy.command.Parser;
import com.iConomy.command.exceptions.InvalidUsage;
import com.iConomy.handlers.*;
import com.iConomy.IO.Database;
import com.iConomy.IO.exceptions.MissingDriver;
import com.iConomy.system.Account;
import com.iConomy.system.Accounts;
import com.iConomy.system.Holdings;
import com.iConomy.util.Common;
import com.iConomy.util.Messaging;
import com.iConomy.util.Template;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class iConomy extends JavaPlugin {
    public PluginDescriptionFile info;
    public PluginManager manager;

    private Accounts Accounts = new Accounts();
    private Parser Commands = new Parser();
    private Permissions Permissions;

    public static boolean TerminalSupport = false;
    public static File directory;
    public static Database Database;
    public static Server Server;
    public static Template Template;

    public void onEnable() {
        final long startTime = System.nanoTime();
        final long endTime;

        try {
            // Localize locale to prevent issues.
            Locale.setDefault(Locale.US);

            // Server & Terminal Support
            Server = getServer();
            TerminalSupport = ((CraftServer)getServer()).getReader().getTerminal().isANSISupported();

            // Get general plugin information
            info = getDescription();

            // Plugin directory setup
            directory = getDataFolder();
            if(!directory.exists()) directory.mkdir();

            // Extract Files
            Common.extract("Config.yml", "Template.yml");

            // Setup Configuration
            Constants.load(new Configuration(new File(directory, "Config.yml")));

            // Setup Template
            Template = new Template(directory.getPath(), "Template.yml");

            // Setup Commands
            Commands.add("/money +name", new money(this));
            Commands.add("/money -create +name", new create(this));
            Commands.add("/money -remove +name", new remove(this));
            Commands.add("/money -give +name +amount:empty", new give(this));
            Commands.add("/money -take +name +amount:empty", new take(this));
            Commands.add("/money -set +name +amount:empty", new set(this));

            // Setup Database.
            try {
                Database = new Database(
                    Constants.Nodes.DatabaseType.toString(),
                    Constants.Nodes.DatabaseUrl.toString(),
                    Constants.Nodes.DatabaseUsername.toString(),
                    Constants.Nodes.DatabasePassword.toString()
                );

                // Check to see if it's a binary database, if so, check the database existance
                // If it doesn't exist, create one.
                if(Database.getDatabase() == null)
                    if(!Database.tableExists(Constants.Nodes.DatabaseTable.toString())) {
                        System.out.println("Testing...");
                    }

            } catch (MissingDriver ex) {
                System.out.println(ex.getMessage());
            }

            // Test account creation / existance.
            String name = "Nijikokun";

            System.out.println(name + " exists? " + Accounts.exists(name));

            Account Nijikokun = Accounts.get(name);
            Holdings holdings = Nijikokun.getHoldings();

            System.out.println("Balance: " + holdings.getBalance());
        } finally {
          endTime = System.nanoTime();
        }

        final long duration = endTime - startTime;

        // Finish
        System.out.println("[" + info.getName() + "] Enabled (" + Common.readableProfile(duration) + ")");
    }

    public void onDisable() {
        String name = info.getName();
        System.out.println("[" + name + "] Closing general data...");

        // Start Time Logging
        final long startTime = System.nanoTime();
        final long endTime;

        // Disable Startup information to prevent
        // duplicate information on /reload
        try {
            info = null;
            Server = null;
            manager = null;
            Accounts = null;
            Commands = null;
            Database = null;
            Template = null;
            TerminalSupport = false;
        } finally {
          endTime = System.nanoTime();
        }

        // Finish duration
        final long duration = endTime - startTime;

        // Output finished & time.
        System.out.println("[" + name + "] Disabled. (" + Common.readableProfile(duration) + ")");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Handler handler = Commands.getHandler(command.getName());
        String split = "/" + command.getName().toLowerCase();

        for (int i = 0; i < args.length; i++) {
            split = split + " " + args[i];
        }

        Messaging.save(sender);
        Commands.save(split);
        Commands.parse();

        if(Commands.getHandler() != null)
            handler = Commands.getHandler();

        if(handler == null) return false;

        try {
            return handler.perform(sender, Commands.getArguments());
        } catch (InvalidUsage ex) {
            Messaging.send(sender, ex.getMessage());
            return false;
        }
    }

    public boolean hasPermissions(CommandSender sender, String command) {
        if(sender instanceof Player) {
            Player player = (Player)sender;
            if(Commands.hasPermission(command)) {
                String node = Commands.getPermission(command);

                if(this.Permissions != null)
                    return Permissions.Security.permission(player, node);
                else {
                    return player.isOp();
                }
            }
        }

        return true;
    }
}
