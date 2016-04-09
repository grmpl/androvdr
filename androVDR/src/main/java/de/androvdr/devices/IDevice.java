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

package de.androvdr.devices;


public interface IDevice {
	public String getDisplayClassName();
	
	public long getId();
	public void setId(long id);
	
	public String getName();
	public void setName(String name);

	public String getIP();
	public void setIP(String ip);

	public int getPort();
	public void setPort(int port);

	public String getUser();
	public void setUser(String user);

	public String getPassword();
	public void setPassword(String password);

	public String getLastError();

	public void disconnect();
}
