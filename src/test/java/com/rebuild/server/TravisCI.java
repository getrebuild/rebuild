/*
rebuild - Building your system freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author devezhao
 * @since 01/26/2019
 */
public class TravisCI {
	
	private static final Log LOG = LogFactory.getLog(TravisCI.class);
	
	/**
	 * travis-ci install
	 */
	public static void install() throws Exception {
		LOG.warn("Preparing travis-ci TODO ...");
		
//		Class.forName(Driver.class.getName());
//		Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:4653/test", "root", "");
//		Statement stmt = conn.createStatement();
	}
}
