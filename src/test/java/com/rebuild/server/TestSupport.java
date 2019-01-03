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
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * 
 * @author devezhao
 * @since 01/03/2019
 */
public class TestSupport {
	
	protected static final Log LOG = LogFactory.getLog(TestSupport.class);

	@BeforeClass
	public static void startup() {
		LOG.warn("TESTING Startup ...");
		Application.debug();
	}
	
	@AfterClass
	public static void shutdown() {
		LOG.warn("TESTING Shutdown ...");
	}
}
