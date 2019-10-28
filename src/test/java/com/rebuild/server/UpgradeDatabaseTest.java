/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

import com.rebuild.server.helper.upgrade.DbScriptsReader;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Map;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/22
 */
public class UpgradeDatabaseTest {
	
	@Test
	public void testUpgrade() throws Exception {
		Application.debug();
		
		// It's okay
		UpgradeDatabase.getInstance().upgrade();
		// It's okay too
		UpgradeDatabase.getInstance().upgrade();
	}

	@Test
	public void testRead() throws Exception {
		Map<Integer, String[]> sqls = new DbScriptsReader().read();
		
		int verIdx = 1;
		while (true) {
			String sql[] = sqls.get(verIdx);
			if (sql == null) {
				break;
			}
			System.out.println("-- #" + verIdx);
			System.out.println(StringUtils.join(sql, "\n-- NewLine\n"));
			verIdx++;
		}
	}
}
