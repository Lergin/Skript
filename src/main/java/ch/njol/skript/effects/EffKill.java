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

package ch.njol.skript.effects;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Event;

/**
 * @author Peter Güttinger
 */
@Name("Kill")
@Description({"Kills an entity.",
		"Note: This effect does not set the entitie's health to 0 (which causes issues), but damages the entity by 100 times its maximum health."})
@Examples({"kill the player",
		"kill all creepers in the player's world",
		"kill all endermen, witches and bats"})
@Since("1.0")
public class EffKill extends Effect {
	static {
		Skript.registerEffect(EffKill.class, "kill %entities%");
	}
	
	@SuppressWarnings("null")
	private Expression<Entity> entities;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] vars, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		entities = (Expression<Entity>) vars[0];
		return true;
	}
	
	@Override
	protected void execute(final Event e) {
		for (final Entity entity : entities.getArray(e)) {
			if (entity instanceof Living) {
				// I don't think this is needed if we remove the entity
				//final boolean creative = entity instanceof Player &&
				//		((Player) entity).get(Keys.GAME_MODE).orElse(GameModes.NOT_SET) == GameModes.CREATIVE;
				//if (creative)
				//	((Player) entity).offer(Keys.GAME_MODE, GameModes.SURVIVAL);

				entity.remove();//todo: does this kill an entity

				//if (creative)
				//	((Player) entity).offer(Keys.GAME_MODE, GameModes.CREATIVE);
			}
		}
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "kill " + entities.toString(e, debug);
	}
	
}
