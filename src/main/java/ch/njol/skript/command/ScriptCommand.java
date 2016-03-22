/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011-2014 Peter Güttinger
 * 
 */

package ch.njol.skript.command;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.log.Verbosity;
import ch.njol.skript.util.EmptyStacktraceException;
import ch.njol.skript.util.Utils;
import ch.njol.util.StringUtils;
import ch.njol.util.Validate;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.*;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * This class is used for user-defined commands.
 * 
 * @author Peter Güttinger
 */
public class ScriptCommand implements CommandCallable {
	public final static Message m_executable_by_players = new Message("commands.executable by players");
	public final static Message m_executable_by_console = new Message("commands.executable by console");
	
	final String name;
	private final String label;
	private final List<String> aliases;
	private List<String> activeAliases;
	private final String permission, permissionMessage;
	private final String description;
	final String usage;
	
	final Trigger trigger;
	
	private final String pattern;
	private final List<Argument<?>> arguments;
	
	public final static int PLAYERS = 0x1, CONSOLE = 0x2, BOTH = PLAYERS | CONSOLE;
	final int executableBy;

	/**
	 * Creates a new SkriptCommand.
	 * 
	 * @param name /name
	 * @param pattern
	 * @param arguments the list of Arguments this command takes
	 * @param description description to display in /help
	 * @param usage message to display if the command was used incorrectly
	 * @param aliases /alias1, /alias2, ...
	 * @param permission permission or null if none
	 * @param permissionMessage message to display if the player doesn't have the given permission
	 * @param items trigger to execute
	 */
	public ScriptCommand(final File script, final String name, final String pattern, final List<Argument<?>> arguments, final String description, final String usage, final ArrayList<String> aliases, final String permission, final String permissionMessage, final int executableBy, final List<TriggerItem> items) {
		Validate.notNull(name, pattern, arguments, description, usage, aliases, items);
		this.name = name;
		label = "" + name.toLowerCase();
		this.permission = permission;
		this.permissionMessage = permissionMessage.isEmpty() ? Language.get("commands.no permission message") : Utils.replaceEnglishChatStyles(permissionMessage);

		// also the main command is a alias in sponge
		//final Iterator<String> as = aliases.iterator();
		//while (as.hasNext()) { // remove aliases that are the same as the command
		//	if (as.next().equalsIgnoreCase(label))
		//		as.remove();
		//}
		aliases.add(0, label);

		this.aliases = aliases;
		activeAliases = new ArrayList<String>();
		
		this.description = Utils.replaceEnglishChatStyles(description);
		this.usage = Utils.replaceEnglishChatStyles(usage);
		
		this.executableBy = executableBy;
		
		this.pattern = pattern;
		this.arguments = arguments;
		
		trigger = new Trigger(script, "command /" + name, new SimpleEvent(), items);

		this.commandMapping = Sponge.getCommandManager().register(Skript.getInstance(),this, aliases).get();

		activeAliases.addAll(commandMapping.getAllAliases());
	}


	@Override
	public CommandResult process(CommandSource sender, String s) throws CommandException {
		if (sender == null || label == null || s == null)
			return CommandResult.empty();
		execute(sender, label, s);
		return CommandResult.success();
	}

	@Override
	public List<String> getSuggestions(CommandSource commandSource, String s) throws CommandException {
		//todo add args as suggestions
		return new ArrayList<String>();
	}

	@Override
	public boolean testPermission(CommandSource commandSource) {
		if(commandSource.hasPermission(permission)){
			return true;
		}else{
			commandSource.sendMessage(Text.of(permissionMessage));
			return false;
		}
	}

	@Override
	public Optional<? extends Text> getShortDescription(CommandSource commandSource) {
		return Optional.of(Text.of(description));
	}

	@Override
	public Optional<? extends Text> getHelp(CommandSource commandSource) {
		return Optional.of(Text.of(description));
	}

	@Override
	public Text getUsage(CommandSource commandSource) {
		return Text.of(usage);
	}

	public boolean execute(final CommandSource sender, final String commandLabel, final String rest) {
		if (sender instanceof Player) {
			if ((executableBy & PLAYERS) == 0) {
				sender.sendMessage(Text.of("", m_executable_by_console));
				return false;
			}
		} else {
			if ((executableBy & CONSOLE) == 0) {
				sender.sendMessage(Text.of("", m_executable_by_players));
				return false;
			}
		}
		
		if (!permission.isEmpty() && !sender.hasPermission(permission)) {
			sender.sendMessage(Text.of(permissionMessage));
			return false;
		}

		//todo
//		if (Bukkit.isPrimaryThread()) {
//			execute2(sender, commandLabel, rest);
//		} else {
//			// must not wait for the command to complete as some plugins call commands in such a way that the server will deadlock
//			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(), new Runnable() {
//				@Override
//				public void run() {
//					execute2(sender, commandLabel, rest);
//				}
//			});
//		}

		execute2(sender, commandLabel, rest);

		return true; // Skript prints its own error message anyway
	}
	
	boolean execute2(final CommandSource sender, final String commandLabel, final String rest) {
		final ScriptCommandEvent event = new ScriptCommandEvent(ScriptCommand.this, sender);
		
		final ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			final boolean ok = SkriptParser.parseArguments(rest, ScriptCommand.this, event);
			if (!ok) {
				final LogEntry e = log.getError();
				if (e != null)
					sender.sendMessage(Text.of(TextColors.DARK_RED, e.getMessage()));
				sender.sendMessage(Text.of(Commands.m_correct_usage, " ", usage));
				log.clear();
				log.printLog();
				return false;
			}
			log.clear();
			log.printLog();
		} finally {
			log.stop();
		}
		
		if (Skript.log(Verbosity.VERY_HIGH))
			Skript.info("# /" + name + " " + rest);
		final long startTrigger = System.nanoTime();
		
		if (!trigger.execute(event))
			sender.sendMessage(Text.of(Commands.m_internal_error.toString()));
		
		if (Skript.log(Verbosity.VERY_HIGH))
			Skript.info("# " + name + " took " + 1. * (System.nanoTime() - startTrigger) / 1000000. + " milliseconds");
		return true;
	}
	
	public void sendHelp(final CommandSource sender) {
		if (!description.isEmpty())
			sender.sendMessage(Text.of(description));
		sender.sendMessage(Text.of(TextColors.GOLD, "Usage", TextColors.RESET, ": ", usage));
	}
	
	/**
	 * Gets the arguments this command takes.
	 * 
	 * @return The internal list of arguments. Do not modify it!
	 */
	public List<Argument<?>> getArguments() {
		return arguments;
	}
	
	public String getPattern() {
		return pattern;
	}

	private CommandMapping commandMapping;

	//todo remove
	//public void register(final SimpleCommandMap commandMap, final @Nullable Set<String> aliases) {
//
//	}

	//todo test if it wants to register multiple at ones
	public void register() {
		final CommandManager commandManager = Sponge.getCommandManager();
		final Skript skript = Skript.getInstance();

		commandMapping = commandManager.register(skript, this, aliases).get();

		activeAliases.clear();
		activeAliases.addAll(commandMapping.getAllAliases());
	}
	
	public void unregister() {
		final CommandManager commandManager = Sponge.getCommandManager();

		commandManager.removeMapping(commandMapping);
	}

//	no need to do this in sponge

//	private transient Collection<HelpTopic> helps = new ArrayList<HelpTopic>();
//
//	public void registerHelp() {
//		helps.clear();
//		final HelpMap help = Bukkit.getHelpMap();
//		final HelpTopic t = new GenericCommandHelpTopic(bukkitCommand);
//		help.addTopic(t);
//		helps.add(t);
//		final HelpTopic aliases = help.getHelpTopic("Aliases");
//		if (aliases != null && aliases instanceof IndexHelpTopic) {
//			aliases.getFullText(Bukkit.getConsoleSender()); // CraftBukkit has a lazy IndexHelpTopic class (org.bukkit.craftbukkit.help.CustomIndexHelpTopic) - maybe its used for aliases as well
//			try {
//				final Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
//				topics.setAccessible(true);
//				@SuppressWarnings("unchecked")
//				final ArrayList<HelpTopic> as = new ArrayList<HelpTopic>((Collection<HelpTopic>) topics.get(aliases));
//				for (final String alias : activeAliases) {
//					final HelpTopic at = new CommandAliasHelpTopic("/" + alias, "/" + getLabel(), help);
//					as.add(at);
//					helps.add(at);
//				}
//				Collections.sort(as, HelpTopicComparator.helpTopicComparatorInstance());
//				topics.set(aliases, as);
//			} catch (final Exception e) {
//				Skript.outdatedError(e);//, "error registering aliases for /" + getName());
//			}
//		}
//	}
//
//	public void unregisterHelp() {
//		Bukkit.getHelpMap().getHelpTopics().removeAll(helps);
//		final HelpTopic aliases = Bukkit.getHelpMap().getHelpTopic("Aliases");
//		if (aliases != null && aliases instanceof IndexHelpTopic) {
//			try {
//				final Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
//				topics.setAccessible(true);
//				@SuppressWarnings("unchecked")
//				final ArrayList<HelpTopic> as = new ArrayList<HelpTopic>((Collection<HelpTopic>) topics.get(aliases));
//				as.removeAll(helps);
//				topics.set(aliases, as);
//			} catch (final Exception e) {
//				Skript.outdatedError(e);//, "error unregistering aliases for /" + getName());
//			}
//		}
//		helps.clear();
//	}
	
	public String getName() {
		return name;
	}
	
	public String getLabel() {
		return label;
	}
	
	public List<String> getAliases() {
		return aliases;
	}
	
	public List<String> getActiveAliases() {
		return activeAliases;
	}


	@Nullable
	public File getScript() {
		return trigger.getScript();
	}
}
