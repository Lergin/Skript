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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.SectionValidator;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.BukkitLoggerFilter;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.mirre.FilterPrintStream;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Callback;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

//TODO option to disable replacement of <color>s in command arguments?

/**
 * @author Peter Güttinger
 */
@SuppressWarnings("deprecation")
public abstract class Commands {
	
	public final static ArgsMessage m_too_many_arguments = new ArgsMessage("commands.too many arguments");
	public final static Message m_correct_usage = new Message("commands.correct usage");
	public final static Message m_internal_error = new Message("commands.internal error");
	
	private final static Map<String, ScriptCommand> commands = new HashMap<String, ScriptCommand>();
	
//	@Nullable
//	private static SimpleCommandMap commandMap = null;
	@Nullable
	private static Set<String> cmAliases;
	
	static {
		init(); // separate method for the annotation
	}
	
	@SuppressWarnings("unchecked")
	private final static void init() {
		// i think in sponge we don't need this
		/*try {
			if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
				final Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
				commandMapField.setAccessible(true);
				commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getPluginManager());

				try {
					final Field aliasesField = SimpleCommandMap.class.getDeclaredField("aliases");
					aliasesField.setAccessible(true);
					cmAliases = (Set<String>) aliasesField.get(commandMap);
				} catch (final NoSuchFieldException e) {}
			}
		} catch (final SecurityException e) {
			Skript.error("Please disable the security manager");
			commandMap = null;
		} catch (final Exception e) {
			Skript.outdatedError(e);
			commandMap = null;
		}*/
	}
	
	private final static SectionValidator commandStructure = new SectionValidator()
			.addEntry("usage", true)
			.addEntry("description", true)
			.addEntry("permission", true)
			.addEntry("permission message", true)
			.addEntry("aliases", true)
			.addEntry("executable by", true)
			.addSection("trigger", false);
	
	@Nullable
	public static List<Argument<?>> currentArguments = null;
	
	@SuppressWarnings("null")
	private final static Pattern escape = Pattern.compile("[" + Pattern.quote("(|)<>%\\") + "]");
	@SuppressWarnings("null")
	private final static Pattern unescape = Pattern.compile("\\\\[" + Pattern.quote("(|)<>%\\") + "]");
	
	private final static String escape(final String s) {
		return "" + escape.matcher(s).replaceAll("\\\\$0");
	}
	
	private final static String unescape(final String s) {
		return "" + unescape.matcher(s).replaceAll("$0");
	}

	//		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private final static EventListener<SendCommandEvent> commandListener = new EventListener<SendCommandEvent>() {
		@Override
		public void handle(SendCommandEvent e) throws Exception {
			if (e.getCommand() == null || e.getCommand().isEmpty() || !e.getCause().containsType(CommandSource.class))
				return;

			CommandSource source = (CommandSource) e.getCause().root();

			if (SkriptConfig.enableEffectCommands.value() && e.getCommand().startsWith(SkriptConfig.effectCommandToken.value())) {
				if (handleEffectCommand(source, e.getCommand())) {
					e.setCommand("");
					e.setResult(CommandResult.success());
					suppressUnknownCommandMessage = true;
				}
				return;
			}
			if (handleCommand(source, e.getCommand())) {
				e.setCommand("");
				e.setResult(CommandResult.success());
				suppressUnknownCommandMessage = true;
			}
		}
	};
	
	public static boolean suppressUnknownCommandMessage = false;
	static {
		BukkitLoggerFilter.addFilter(new Filter() {
			@Override
			public boolean isLoggable(final @Nullable LogRecord record) {
				if (record == null)
					return false;
				if (suppressUnknownCommandMessage && record.getMessage() != null && record.getMessage().startsWith("Unknown command. Type")) {
					suppressUnknownCommandMessage = false;
					return false;
				}
				return true;
			}
		});
	}

	//Skript.classExists("org.bukkit.event.player.AsyncPlayerChatEvent") ? null :
	@Nullable
	private final static EventListener<MessageChannelEvent.Chat> pre1_3chatListener =  new EventListener<MessageChannelEvent.Chat>() {
		@SuppressWarnings("null")
		//@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void handle(final MessageChannelEvent.Chat e) {
			if (!SkriptConfig.enableEffectCommands.value() || !e.getMessage().toPlain().startsWith(SkriptConfig.effectCommandToken.value()))
				return;
			if (e.getCause().containsType(CommandSource.class) &&
					handleEffectCommand(e.getCause().first(CommandSource.class).get(), e.getMessage().toPlain()))
				e.setMessageCancelled(true);
		}
	};
	/*@Nullable
	private final static Listener post1_3chatListener = !Skript.classExists("org.bukkit.event.player.AsyncPlayerChatEvent") ? null : new Listener() {
		@SuppressWarnings("null")
		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void onPlayerChat(final AsyncPlayerChatEvent e) {
			if (!SkriptConfig.enableEffectCommands.value() || !e.getMessage().startsWith(SkriptConfig.effectCommandToken.value()))
				return;
			if (!e.isAsynchronous()) {
				if (handleEffectCommand(e.getPlayer(), e.getMessage()))
					e.setCancelled(true);
			} else {
				final Future<Boolean> f = Bukkit.getScheduler().callSyncMethod(Skript.getInstance(), new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return handleEffectCommand(e.getPlayer(), e.getMessage());
					}
				});
				try {
					while (true) {
						try {
							if (f.get())
								e.setCancelled(true);
							break;
						} catch (final InterruptedException e1) {}
					}
				} catch (final ExecutionException e1) {
					Skript.exception(e1);
				}
			}
		}
	};*/
	
	/**
	 * @param sender
	 * @param command full command string without the slash
	 * @return whether to cancel the event
	 */
	final static boolean handleCommand(final CommandSource sender, final String command) {
		final String[] cmd = command.split("\\s+", 2);
		cmd[0] = cmd[0].toLowerCase();
		if (cmd[0].endsWith("?")) {
			final ScriptCommand c = commands.get(cmd[0].substring(0, cmd[0].length() - 1));
			if (c != null) {
				c.sendHelp(sender);
				return true;
			}
		}
		final ScriptCommand c = commands.get(cmd[0]);
		if (c != null) {
//			if (cmd.length == 2 && cmd[1].equals("?")) {
//				c.sendHelp(sender);
//				return true;
//			}
			if (SkriptConfig.logPlayerCommands.value() && sender instanceof Player)
				SkriptLogger.LOGGER.info(sender.getName() + " [" + ((Player) sender).getUniqueId() + "]: /" + command);
			c.execute(sender, "" + cmd[0], cmd.length == 1 ? "" : "" + cmd[1]);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	final static boolean handleEffectCommand(final CommandSource sender, String command) {
		if (!(sender instanceof ConsoleSource || sender.hasPermission("skript.effectcommands")))
			return false;
		final boolean wasLocal = Language.setUseLocal(false);
		try {
			command = "" + command.substring(SkriptConfig.effectCommandToken.value().length()).trim();
			final RetainingLogHandler log = SkriptLogger.startRetainingLog();
			try {
				ScriptLoader.setCurrentEvent("effect command", EffectCommandEvent.class);
				final Effect e = Effect.parse(command, null);
				ScriptLoader.deleteCurrentEvent();
				
				if (e != null) {
					log.clear(); // ignore warnings and stuff
					log.printLog();
					
					sender.sendMessage(Text.of(TextColors.GRAY, "executing '", Text.of(command), "'"));
					if (SkriptConfig.logPlayerCommands.value() && !(sender instanceof ConsoleSource))
						Skript.info(sender.getName() + " issued effect command: " + command);
					e.run(new EffectCommandEvent(sender, command));
				} else {
					if (sender == Sponge.getServer().getConsole()) // log as SEVERE instead of INFO like printErrors below
						SkriptLogger.LOGGER.severe("Error in: " + command);
					else
						sender.sendMessage(Text.of(TextColors.RED, "Error in: ", TextColors.GRAY, Text.of(command)));
					log.printErrors(sender, "(No specific information is available)");
				}
			} finally {
				log.stop();
			}
			return true;
		} catch (final Exception e) {
			Skript.exception(e, "Unexpected error while executing effect command '" + command + "' by '" + sender.getName() + "'");
			sender.sendMessage(Text.of(TextColors.RED, "An internal error occurred while executing this effect. Please refer to the server log for details."));
			return true;
		} finally {
			Language.setUseLocal(wasLocal);
		}
	}
	
	@SuppressWarnings("null")
	private final static Pattern commandPattern = Pattern.compile("(?i)^command /?(\\S+)(\\s+(.+))?$"),
			argumentPattern = Pattern.compile("<\\s*(?:(.+?)\\s*:\\s*)?(.+?)\\s*(?:=\\s*(" + SkriptParser.wildcard + "))?\\s*>");
	
	@Nullable
	public final static ScriptCommand loadCommand(final SectionNode node) {
		final String key = node.getKey();
		if (key == null)
			return null;
		final String s = ScriptLoader.replaceOptions(key);
		
		int level = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '[') {
				level++;
			} else if (s.charAt(i) == ']') {
				if (level == 0) {
					Skript.error("Invalid placement of [optional brackets]");
					return null;
				}
				level--;
			}
		}
		if (level > 0) {
			Skript.error("Invalid amount of [optional brackets]");
			return null;
		}
		
		Matcher m = commandPattern.matcher(s);
		final boolean a = m.matches();
		assert a;
		
		final String command = "" + m.group(1).toLowerCase();
		final ScriptCommand existingCommand = commands.get(command);
		if (existingCommand != null && existingCommand.getLabel().equals(command)) {
			final File f = existingCommand.getScript();
			Skript.error("A command with the name /" + command + " is already defined" + (f == null ? "" : " in " + f.getName()));
			return null;
		}
		
		final String arguments = m.group(3) == null ? "" : m.group(3);
		final StringBuilder pattern = new StringBuilder();
		
		List<Argument<?>> currentArguments = Commands.currentArguments = new ArrayList<Argument<?>>(); //Mirre
		m = argumentPattern.matcher(arguments);
		int lastEnd = 0;
		int optionals = 0;
		for (int i = 0; m.find(); i++) {
			pattern.append(escape("" + arguments.substring(lastEnd, m.start())));
			optionals += StringUtils.count(arguments, '[', lastEnd, m.start());
			optionals -= StringUtils.count(arguments, ']', lastEnd, m.start());
			
			lastEnd = m.end();
			
			ClassInfo<?> c;
			c = Classes.getClassInfoFromUserInput("" + m.group(2));
			final NonNullPair<String, Boolean> p = Utils.getEnglishPlural("" + m.group(2));
			if (c == null)
				c = Classes.getClassInfoFromUserInput(p.getFirst());
			if (c == null) {
				Skript.error("Unknown type '" + m.group(2) + "'");
				return null;
			}
			final Parser<?> parser = c.getParser();
			if (parser == null || !parser.canParse(ParseContext.COMMAND)) {
				Skript.error("Can't use " + c + " as argument of a command");
				return null;
			}
			
			final Argument<?> arg = Argument.newInstance(m.group(1), c, m.group(3), i, !p.getSecond(), optionals > 0);
			if (arg == null)
				return null;
			currentArguments.add(arg);
			
			if (arg.isOptional() && optionals == 0) {
				pattern.append('[');
				optionals++;
			}
			pattern.append("%" + (arg.isOptional() ? "-" : "") + Utils.toEnglishPlural(c.getCodeName(), p.getSecond()) + "%");
		}
		
		pattern.append(escape("" + arguments.substring(lastEnd)));
		optionals += StringUtils.count(arguments, '[', lastEnd);
		optionals -= StringUtils.count(arguments, ']', lastEnd);
		for (int i = 0; i < optionals; i++)
			pattern.append(']');
		
		String desc = "/" + command + " ";
		final boolean wasLocal = Language.setUseLocal(true); // use localised class names in description
		try {
			desc += StringUtils.replaceAll(pattern, "(?<!\\\\)%-?(.+?)%", new Callback<String, Matcher>() {
				@Override
				public String run(final @Nullable Matcher m) {
					assert m != null;
					final NonNullPair<String, Boolean> p = Utils.getEnglishPlural("" + m.group(1));
					final String s = p.getFirst();
					return "<" + Classes.getClassInfo(s).getName().toString(p.getSecond()) + ">";
				}
			});
		} finally {
			Language.setUseLocal(wasLocal);
		}
		desc = unescape(desc);
		desc = "" + desc.trim();
		
		node.convertToEntries(0);
		commandStructure.validate(node);
		if (!(node.get("trigger") instanceof SectionNode))
			return null;
		
		final String usage = ScriptLoader.replaceOptions(node.get("usage", desc));
		final String description = ScriptLoader.replaceOptions(node.get("description", ""));
		ArrayList<String> aliases = new ArrayList<String>(Arrays.asList(ScriptLoader.replaceOptions(node.get("aliases", "")).split("\\s*,\\s*/?")));
		if (aliases.get(0).startsWith("/"))
			aliases.set(0, aliases.get(0).substring(1));
		else if (aliases.get(0).isEmpty())
			aliases = new ArrayList<String>(0);
		final String permission = ScriptLoader.replaceOptions(node.get("permission", ""));
		final String permissionMessage = ScriptLoader.replaceOptions(node.get("permission message", ""));
		final SectionNode trigger = (SectionNode) node.get("trigger");
		if (trigger == null)
			return null;
		final String[] by = ScriptLoader.replaceOptions(node.get("executable by", "console,players")).split("\\s*,\\s*|\\s+(and|or)\\s+");
		int executableBy = 0;
		for (final String b : by) {
			if (b.equalsIgnoreCase("console") || b.equalsIgnoreCase("the console")) {
				executableBy |= ScriptCommand.CONSOLE;
			} else if (b.equalsIgnoreCase("players") || b.equalsIgnoreCase("player")) {
				executableBy |= ScriptCommand.PLAYERS;
			} else {
				Skript.warning("'executable by' should be either be 'players', 'console', or both, but found '" + b + "'");
			}
		}
		
		if (!permissionMessage.isEmpty() && permission.isEmpty()) {
			Skript.warning("command /" + command + " has a permission message set, but not a permission");
		}
		
		if (Skript.debug() || node.debug())
			Skript.debug("command " + desc + ":");
		
		final File config = node.getConfig().getFile();
		if (config == null) {
			assert false;
			return null;
		}
		
		Commands.currentArguments = currentArguments;
		final ScriptCommand c;
		try {
			c = new ScriptCommand(config, command, "" + pattern.toString(), currentArguments, description, usage, aliases, permission, permissionMessage, executableBy, ScriptLoader.loadItems(trigger));
		} finally {
			Commands.currentArguments = null;
		}	
		registerCommand(c);
		
		if (Skript.logVeryHigh() && !Skript.debug())
			Skript.info("registered command " + desc);
		return c;
	}
	
//	public static boolean skriptCommandExists(final String command) {
//		final ScriptCommand c = commands.get(command);
//		return c != null && c.getName().equals(command);
//	}
	
	public static void registerCommand(final ScriptCommand command) {
		command.register();
		commands.put(command.getLabel(), command);

		for (final String alias : command.getActiveAliases()) {
			commands.put(alias.toLowerCase(), command);
		}
	}
	
	public static int unregisterCommands(final File script) {
		int numCommands = 0;
		final Iterator<ScriptCommand> commandsIter = commands.values().iterator();
		while (commandsIter.hasNext()) {
			final ScriptCommand c = commandsIter.next();
			if (script.equals(c.getScript())) {
				numCommands++;
				c.unregister();
				commandsIter.remove();
			}
		}
		return numCommands;
	}
	
	private static boolean registeredListeners = false;
	
	public final static void registerListeners() {
		if (!registeredListeners) {
			Skript skript = Skript.getInstance();
			Sponge.getEventManager().registerListeners(skript, pre1_3chatListener);
			Sponge.getEventManager().registerListeners(skript, commandListener);
			registeredListeners = true;
		}
	}
	
	public final static void clearCommands() {
		for (final ScriptCommand c : commands.values())
			c.unregister();

		commands.clear();
	}
	
	/**
	 * copied from CraftBukkit (org.bukkit.craftbukkit.help.CommandAliasHelpTopic)
	 */
	/*public final static class CommandAliasHelpTopic extends HelpTopic {
		
		private final String aliasFor;
		private final HelpMap helpMap;
		
		public CommandAliasHelpTopic(final String alias, final String aliasFor, final HelpMap helpMap) {
			this.aliasFor = aliasFor.startsWith("/") ? aliasFor : "/" + aliasFor;
			this.helpMap = helpMap;
			name = alias.startsWith("/") ? alias : "/" + alias;
			Validate.isTrue(!name.equals(this.aliasFor), "Command " + name + " cannot be alias for itself");
			shortText = ChatColor.YELLOW + "Alias for " + ChatColor.WHITE + this.aliasFor;
		}
		
		@Override
		public String getFullText(final @Nullable CommandSender forWho) {
			final StringBuilder sb = new StringBuilder(shortText);
			final HelpTopic aliasForTopic = helpMap.getHelpTopic(aliasFor);
			if (aliasForTopic != null) {
				sb.append("\n");
				sb.append(aliasForTopic.getFullText(forWho));
			}
			return "" + sb.toString();
		}
		
		@Override
		public boolean canSee(final @Nullable CommandSender commandSender) {
			if (amendedPermission == null) {
				final HelpTopic aliasForTopic = helpMap.getHelpTopic(aliasFor);
				if (aliasForTopic != null) {
					return aliasForTopic.canSee(commandSender);
				} else {
					return false;
				}
			} else {
				return commandSender != null && commandSender.hasPermission(amendedPermission);
			}
		}
	}*/
	
}
