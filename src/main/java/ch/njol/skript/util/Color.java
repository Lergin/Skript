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

package ch.njol.skript.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.LanguageChangeListener;
import ch.njol.yggdrasil.YggdrasilSerializable;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

/**
 * @author Peter Güttinger
 */
@SuppressWarnings({"deprecation", "null"})
public enum Color implements YggdrasilSerializable {

	//TODO: to from wool data via datamanipulator
	BLACK(org.spongepowered.api.util.Color.BLACK, TextColors.BLACK, org.spongepowered.api.util.Color.ofRgb(0x191919)),
	DARK_GREY(org.spongepowered.api.util.Color.GRAY, TextColors.DARK_GRAY, org.spongepowered.api.util.Color.ofRgb(0x4C4C4C)),
	LIGHT_GREY(org.spongepowered.api.util.Color.DARK_MAGENTA, TextColors.GRAY, org.spongepowered.api.util.Color.ofRgb(0x999999)),//color maybe wrong
	WHITE(org.spongepowered.api.util.Color.WHITE, TextColors.WHITE, org.spongepowered.api.util.Color.ofRgb(0xFFFFFF)),

	DARK_BLUE(org.spongepowered.api.util.Color.BLUE, TextColors.DARK_BLUE, org.spongepowered.api.util.Color.ofRgb(0x334CB2)),
	BROWN(org.spongepowered.api.util.Color.DARK_GREEN, TextColors.BLUE, org.spongepowered.api.util.Color.ofRgb(0x664C33)),//color maybe wrong
	DARK_CYAN(org.spongepowered.api.util.Color.DARK_CYAN, TextColors.DARK_AQUA, org.spongepowered.api.util.Color.ofRgb(0x4C7F99)),//color maybe wrong
	LIGHT_CYAN(org.spongepowered.api.util.Color.CYAN, TextColors.AQUA, org.spongepowered.api.util.Color.ofRgb(0x6699D8)),//color maybe wrong

	DARK_GREEN(org.spongepowered.api.util.Color.GREEN, TextColors.DARK_GREEN, org.spongepowered.api.util.Color.ofRgb(0x667F33)),
	LIGHT_GREEN(org.spongepowered.api.util.Color.LIME, TextColors.GREEN, org.spongepowered.api.util.Color.ofRgb(0x7FCC19)),

	YELLOW(org.spongepowered.api.util.Color.YELLOW, TextColors.YELLOW, org.spongepowered.api.util.Color.ofRgb(0xE5E533)),
	ORANGE(org.spongepowered.api.util.Color.NAVY, TextColors.GOLD, org.spongepowered.api.util.Color.ofRgb(0xD87F33)),//Color maybe wrong

	DARK_RED(org.spongepowered.api.util.Color.RED, TextColors.DARK_RED, org.spongepowered.api.util.Color.ofRgb(0x993333)),
	LIGHT_RED(org.spongepowered.api.util.Color.PINK, TextColors.RED, org.spongepowered.api.util.Color.ofRgb(0xF27FA5)),

	DARK_PURPLE(org.spongepowered.api.util.Color.PURPLE, TextColors.DARK_PURPLE, org.spongepowered.api.util.Color.ofRgb(0x7F3FB2)),
	LIGHT_PURPLE(org.spongepowered.api.util.Color.MAGENTA, TextColors.LIGHT_PURPLE, org.spongepowered.api.util.Color.ofRgb(0xB24CD8));

	private final static String LANGUAGE_NODE = "colors";

	private final org.spongepowered.api.util.Color wool;
	private final TextColor chat;
	private final org.spongepowered.api.util.Color bukkit;

	@Nullable
	Adjective adjective;

	private Color(final org.spongepowered.api.util.Color wool, final TextColor chat, final org.spongepowered.api.util.Color bukkit) {
		this.wool = wool;
		this.chat = chat;
		this.bukkit = bukkit;
	}

	private final static Color[] byWool = new Color[16];
	static {
		for (final Color c : values()) {
			byWool[c.wool.getData()] = c;
		}
	}

	final static Map<String, Color> byName = new HashMap<String, Color>();
	final static Map<String, Color> byEnglishName = new HashMap<String, Color>();
	static {
		Language.addListener(new LanguageChangeListener() {
			@Override
			public void onLanguageChange() {
				final boolean english = byEnglishName.isEmpty();
				byName.clear();
				for (final Color c : values()) {
					final String[] names = Language.getList(LANGUAGE_NODE + "." + c.name() + ".names");
					for (final String name : names) {
						byName.put(name.toLowerCase(), c);
						if (english)
							byEnglishName.put(name.toLowerCase(), c);
					}
					c.adjective = new Adjective(LANGUAGE_NODE + "." + c.name() + ".adjective");
				}
			}
		});
	}

	public byte getDye() {
		return (byte) (15 - wool.getData());
	}

	public org.spongepowered.api.util.Color getWoolColor() {
		return wool;
	}

	public byte getWool() {
		return wool.getData();
	}

	public String getChat() {
		return "" + chat.toString();
	}

	public TextColor asChatColor() {
		return chat;
	}

	// currently only used by SheepData
	public Adjective getAdjective() {
		return adjective;
	}

	@Override
	public String toString() {
		final Adjective a = adjective;
		return a == null ? "" + name() : a.toString(-1, 0);
	}

	@Nullable
	public final static Color byName(final String name) {
		return byName.get(name.toLowerCase());
	}

	@Nullable
	public final static Color byEnglishName(final String name) {
		return byEnglishName.get(name.toLowerCase());
	}

	@Nullable
	public final static Color byWool(final short data) {
		if (data < 0 || data >= 16)
			return null;
		return byWool[data];
	}

	@Nullable
	public final static Color byDye(final short data) {
		if (data < 0 || data >= 16)
			return null;
		return byWool[15 - data];
	}

	public final static Color byWoolColor(final org.spongepowered.api.util.Color color) {
		return byWool(color.getData());
	}

	public final org.spongepowered.api.util.Color getBukkitColor() {
		return bukkit;
	}

}
