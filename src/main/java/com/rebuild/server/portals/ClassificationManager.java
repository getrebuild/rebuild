/*
rebuild - Building your system freely.
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

package com.rebuild.server.portals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.rebuild.server.Application;

import cn.devezhao.persist4j.engine.ID;

/**
 * TODO
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/28
 */
public class ClassificationManager implements PortalsManager {

	/**
	 * @param itemId
	 * @return
	 */
	public static String getFullName(ID itemId) {
		List<String> names = new ArrayList<>();
		while (itemId != null) {
			Object[] o = Application.createQueryNoFilter(
					"select name, parent from ClassificationData where itemId = ?")
					.setParameter(1, itemId)
					.unique();
			names.add((String) o[0]);
			itemId = (ID) o[1];
		}
		
		String namesArr[] = names.toArray(new String[names.size()]);
		ArrayUtils.reverse(namesArr);
		return StringUtils.join(namesArr, ".");
	}
}
