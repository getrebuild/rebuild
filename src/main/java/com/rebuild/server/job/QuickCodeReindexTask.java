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

package com.rebuild.server.job;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.rebuild.server.Application;
import com.rebuild.server.bizz.UserService;
import com.rebuild.server.metadata.EntityHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * TODO QuickCode 重建
 * 
 * @author devezhao
 * @since 12/28/2018
 */
public class QuickCodeReindexTask extends BulkTask {
	
	private static final Log LOG = LogFactory.getLog(QuickCodeReindexTask.class);
	
	private Entity entity;

	/**
	 * @param entity
	 */
	public QuickCodeReindexTask(Entity entity) {
		super();
		this.entity = entity;
	}
	
	@Override
	public void run() {
		if (!entity.containsField(EntityHelper.QuickCode)) {
			throw new IllegalArgumentException("No QuickCode field found : " + entity);
		}
		
		Field nameFiled = entity.getNameField();
		String sql = String.format("select %s,%s,quickCode from %s",
				entity.getPrimaryField().getName(), nameFiled.getName(), entity.getName());
		int page = 1;
		while (true) {
			Object[][] array =  Application.createQueryNoFilter(sql)
					.setLimit(1000, page * 1000 - 1000)
					.array();
			for (Object[] o : array) {
				String nameVal = (String) o[1];
				String quickCodeNew = generateQuickCode(nameVal);
				if (quickCodeNew.equals(o[2])) {
					break;
				}
				
				Record record = EntityHelper.forUpdate((ID) o[0], UserService.SYSTEM_USER);
				record.setString("quickCode", quickCodeNew);
				record.removeValue(EntityHelper.ModifiedBy);
				record.removeValue(EntityHelper.ModifiedOn);
				Application.getCommonService().update(record);
			}
			
			if (array.length < 1000) {
				break;
			}
		}
		
		completeAfter();
	}
	
	/**
	 * @param nameVal
	 * @return
	 */
	public static String generateQuickCode(String nameVal) {
		if (StringUtils.isBlank(nameVal)) {
			return StringUtils.EMPTY;
		}
		
		String quickCode = StringUtils.EMPTY;
		try {
			quickCode = PinyinHelper.getShortPinyin(nameVal).toUpperCase();
		} catch (Exception e) {
			LOG.error("QuickCode shorting error : " + nameVal, e);
		}
		return quickCode;
	}
}
