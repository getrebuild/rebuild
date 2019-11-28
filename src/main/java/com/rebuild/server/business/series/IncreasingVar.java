/*
rebuild - Building your business-systems freely.
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

package com.rebuild.server.business.series;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import com.rebuild.server.Application;
import com.rebuild.server.helper.KVStorage;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 数字自增系列
 * 
 * @author devezhao
 * @since 12/24/2018
 */
public class IncreasingVar extends SeriesVar {
	
	private static final Map<String, Object> LOCKs = new HashMap<>();
	
	private Field field;
	private String zeroFlag;

	/**
	 * @param symbols
	 * @param field
	 * @param zeroFlag
	 */
	protected IncreasingVar(String symbols, Field field, String zeroFlag) {
		super(symbols);
		this.field = field;
		this.zeroFlag = zeroFlag;
	}
	
	/**
	 * @param field
	 */
	protected IncreasingVar(Field field) {
		super(null);
		this.field = field;
	}

	@Override
	public String generate() {
		// Preview mode
		if (field == null) {
			return StringUtils.leftPad("1", getSymbols().length(), '0');
		}
		
		final String nameKey = String.format("Series-%s.%s", field.getOwnEntity().getName(), field.getName());
		Object keyLock = null;
		synchronized (LOCKs) {
			keyLock = LOCKs.computeIfAbsent(nameKey, k -> new Object());
		}

		int nextValue = 1;
		synchronized (keyLock) {
			String val = KVStorage.getCustomValue(nameKey);
			if (val != null) {
				nextValue = ObjectUtils.toInt(val);
			} else {
				nextValue = countFromDb();
			}
			nextValue += 1;

			// TODO 使用缓存，避免频繁更新数据库
			KVStorage.setCustomValue(nameKey, nextValue);
		}
		return StringUtils.leftPad(nextValue + "", getSymbols().length(), '0');
	}
	
	/**
	 * 清空序号缓存
	 */
	protected void clean() {
		if (this.field == null) {
            return;
        }

		final String nameKey = String.format("Series-%s.%s", field.getOwnEntity().getName(), field.getName());
		Object keyLock = null;
		synchronized (LOCKs) {
			keyLock = LOCKs.computeIfAbsent(nameKey, k -> new Object());
		}
		synchronized (keyLock) {
			KVStorage.setCustomValue(nameKey, 0);
		}
	}
	
	/**
	 * 例如有100条记录，序号也为100。
	 * 但是删除了10条后，调用此方法所生产的序号只有 90（直接采用 count 记录数）
	 * 
	 * @return
	 */
	private int countFromDb() {
		String dateLimit = null;
		if ("Y".equals(zeroFlag)) {
			dateLimit = CalendarUtils.format("yyyy", CalendarUtils.now()) + "-01-01";
		} else if ("M".equals(zeroFlag)) {
			dateLimit = CalendarUtils.format("yyyy-MM", CalendarUtils.now()) + "-01";
		} else if ("D".equals(zeroFlag)) {
			dateLimit = CalendarUtils.format("yyyy-MM-dd", CalendarUtils.now());
		}
		
		if (dateLimit != null) {
			dateLimit = "createdOn >= '" + dateLimit + " 00:00:00'";
		} else {
			dateLimit = "(1=1)";
		}
		
		String sql = String.format("select count(%s) from %s where %s", field.getName(), field.getOwnEntity().getName(), dateLimit);
		Object[] count = Application.createQueryNoFilter(sql).unique();
		return ObjectUtils.toInt(count[0]);
	}
}
