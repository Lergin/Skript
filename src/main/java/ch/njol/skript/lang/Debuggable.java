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

package ch.njol.skript.lang;

import org.eclipse.jdt.annotation.Nullable;
import org.spongepowered.api.event.Event;

/**
 * @author Peter Güttinger
 */
public interface Debuggable {
	
	/**
	 * @param e The event to get information to. This is always null if debug == false.
	 * @param debug If true this should print more information, if false this should print what is shown to the end user
	 * @return String representation of this object
	 */
	public String toString(@Nullable Event e, boolean debug);
	
	/**
	 * Should return <tt>{@link #toString(Event, boolean) toString}(null, false)</tt>
	 */
	@Override
	public String toString();
	
}
