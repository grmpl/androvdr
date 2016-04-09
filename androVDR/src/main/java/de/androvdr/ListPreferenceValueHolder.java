/*
 * Copyright (c) 2010-2011 by androvdr <androvdr@googlemail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * For more information on the GPL, please go to:
 * http://www.gnu.org/copyleft/gpl.html
 */

package de.androvdr;

import java.util.LinkedList;
import java.util.List;

public abstract class ListPreferenceValueHolder {
	protected boolean initialized = false;

	protected CharSequence[] ids;
	protected CharSequence[] names;

	public CharSequence[] getNames() {
		if (names == null)
			initialize();

		return names;
	}

	public CharSequence[] getIds() {
		if (ids == null)
			initialize();

		return ids;
	}

	private synchronized void initialize() {
		if (initialized)
			return;

		List<CharSequence> deviceIdsList = new LinkedList<CharSequence>();
		List<CharSequence> deviceNamesList = new LinkedList<CharSequence>();
		setValues(deviceIdsList, deviceNamesList);
		ids = deviceIdsList.toArray(new CharSequence[deviceIdsList.size()]);
		names = deviceNamesList.toArray(new CharSequence[deviceNamesList.size()]);

		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	protected abstract void setValues(List<CharSequence> ids, List<CharSequence> names);
}
