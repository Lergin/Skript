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
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.LanguageChangeListener;
import ch.njol.skript.localization.Noun;
import ch.njol.util.coll.CollectionUtils;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.gen.PopulatorObject;
import org.spongepowered.api.world.gen.PopulatorObjects;

/**
 * @author Peter Güttinger
 */
public enum StructureType {
	TREE(PopulatorObjects.OAK, PopulatorObjects.MEGA_OAK, PopulatorObjects.TALL_TAIGA,
			PopulatorObjects.MEGA_POINTY_TAIGA, PopulatorObjects.MEGA_TALL_TAIGA, PopulatorObjects.POINTY_TAIGA,
			PopulatorObjects.JUNGLE, PopulatorObjects.JUNGLE_BUSH, PopulatorObjects.MEGA_JUNGLE,
			PopulatorObjects.BIRCH, PopulatorObjects.MEGA_BIRCH, PopulatorObjects.SWAMP, PopulatorObjects.SAVANNA),
	
	REGULAR(PopulatorObjects.OAK, PopulatorObjects.MEGA_OAK), SMALL_REGULAR(PopulatorObjects.OAK),
	BIG_REGULAR(PopulatorObjects.MEGA_OAK),
	REDWOOD(PopulatorObjects.TALL_TAIGA, PopulatorObjects.POINTY_TAIGA), SMALL_REDWOOD(PopulatorObjects.POINTY_TAIGA),
	BIG_REDWOOD(PopulatorObjects.TALL_TAIGA),
	JUNGLE(PopulatorObjects.JUNGLE, PopulatorObjects.MEGA_JUNGLE), SMALL_JUNGLE(PopulatorObjects.JUNGLE),
	BIG_JUNGLE(PopulatorObjects.MEGA_JUNGLE), JUNGLE_BUSH(PopulatorObjects.JUNGLE_BUSH),
	SWAMP(PopulatorObjects.SWAMP),

	//todo: savanna, megaTaiga, non trees
	
	MUSHROOM(PopulatorObjects.RED, PopulatorObjects.BROWN),
	RED_MUSHROOM(PopulatorObjects.RED), BROWN_MUSHROOM(PopulatorObjects.BROWN),
	
	;
	
	private Noun name;
	private final PopulatorObject[] types;
	
	private StructureType(final PopulatorObject... types) {
		this.types = types;
		name = new Noun("tree types." + name() + ".name");
	}
	
	public void grow(final Location<World> loc) {
		PopulatorObject pop = CollectionUtils.getRandom(types);

		if(pop.canPlaceAt(loc.getExtent(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
			pop.placeObject(loc.getExtent(), new Random(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}
	
	public void grow(final BlockSnapshot b) {
		grow(b.getLocation().get());
	}
	
	public PopulatorObject[] getTypes() {
		return types;
	}
	
	@Override
	public String toString() {
		return name.toString();
	}
	
	public String toString(final int flags) {
		return name.toString(flags);
	}
	
	public Noun getName() {
		return name;
	}
	
	public boolean is(final PopulatorObject type) {
		return CollectionUtils.contains(types, type);
	}
	
	/**
	 * lazy
	 */
	final static Map<Pattern, StructureType> parseMap = new HashMap<Pattern, StructureType>();
	
	static {
		Language.addListener(new LanguageChangeListener() {
			@Override
			public void onLanguageChange() {
				parseMap.clear();
			}
		});
	}
	
	@Nullable
	public static StructureType fromName(String s) {
		if (parseMap.isEmpty()) {
			for (final StructureType t : values()) {
				final String pattern = Language.get("tree types." + t.name() + ".pattern");
				parseMap.put(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), t);
			}
		}
		s = "" + s.toLowerCase();
		for (final Entry<Pattern, StructureType> e : parseMap.entrySet()) {
			if (e.getKey().matcher(s).matches())
				return e.getValue();
		}
		return null;
	}
	
}
