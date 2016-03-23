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

import java.util.Date;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;

/**
 * @author Peter Güttinger
 */
@Name("Ban")
@Description({"Bans/unbans a player or IP.",
		"Starting with Skript 2.1.1 and Bukkit 1.7.2 R0.4, one can also ban players with a reason."})
@Examples({"unban player",
		"ban \"127.0.0.1\"",
		"IP-ban the player because \"he is an idiot\""})
@Since("1.4, 2.1.1 (ban reason)")
public class EffBan extends Effect {
	static {
		Skript.registerEffect(EffBan.class,
				"ban %strings/offlineplayers% [(by reason of|because [of]|on account of|due to) %-string%]", "unban %strings/offlineplayers%",
				"ban %players% by IP [(by reason of|because [of]|on account of|due to) %-string%]", "unban %players% by IP",
				"IP(-| )ban %players% [(by reason of|because [of]|on account of|due to) %-string%]", "(IP(-| )unban|un[-]IP[-]ban) %players%");
	}
	
	@SuppressWarnings("null")
	private Expression<?> players;
	@Nullable
	private Expression<String> reason;
	
	private boolean ban;
	private boolean ipBan;
	
	@SuppressWarnings({"null", "unchecked"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		players = exprs[0];
		reason = exprs.length > 1 ? (Expression<String>) exprs[1] : null;
		ban = matchedPattern % 2 == 0;
		ipBan = matchedPattern >= 2;
		return true;
	}
	
	@Override
	protected void execute(final Event e) {
		final String reason = this.reason != null ? this.reason.getSingle(e) : ""; // don't check for null, just ignore an invalid reason
		final Date expires = null;
		final String source = "Skript ban effect";
		for (final Object o : players.getArray(e)) {
			BanService banService = Sponge.getServiceManager().getRegistration(BanService.class).get().getProvider();

			if (ban) {
				Ban.Builder banBuilder = Ban.builder();

				if (expires != null) {
					banBuilder.expirationDate(expires.toInstant());
				}

				if (reason != null) {
					banBuilder.reason(Text.of(reason));
				}

				banBuilder.source(Text.of(source));

				if (o instanceof Player) {
					if (ipBan) {
						banBuilder.type(BanTypes.IP).address(((Player) o).getConnection().getAddress().getAddress());
					} else {
						banBuilder.type(BanTypes.PROFILE).profile(((Player) o).getProfile());
					}
				} else if (o instanceof GameProfile) {
					banBuilder.type(BanTypes.PROFILE).profile((GameProfile) o);
				} else if (o instanceof UUID) {
					banBuilder.type(BanTypes.PROFILE).profile(GameProfile.of((UUID) o, null));
				} else if (o instanceof String) {
					System.out.println("String as ban no longer supported use UUID"); //todo only as a reminder
					assert false;
				} else {
					assert false;
				}

				banService.addBan(banBuilder.build());
			} else {
				if (o instanceof Player) {
					if (ipBan) {
						banService.pardon(((Player) o).getConnection().getAddress().getAddress());
					} else {
						banService.pardon(((Player) o).getProfile());
					}
				} else if (o instanceof GameProfile) {
					banService.pardon((GameProfile) o);
				} else if (o instanceof UUID) {
					banService.pardon(GameProfile.of((UUID) o, null));
				} else if (o instanceof String) {
					System.out.println("String as ban no longer supported use UUID"); //todo only as a reminder
					assert false;
				} else {
					assert false;
				}
			}
		}
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return (ipBan ? "IP-" : "") + (ban ? "" : "un") + "ban " + players.toString(e, debug) + (reason != null ? " on account of " + reason.toString(e, debug) : "");
	}
	
}
