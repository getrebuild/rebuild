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

package com.rebuild.server.business.series;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import com.rebuild.server.Application;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;

/**
 * 
 * @author devezhao
 * @since 12/24/2018
 */
public class IncrementVar extends SeriesVar {
	
	private static final Map<String, Object> LOCKs = new HashMap<>();
	
	private Field field;
	private String zeroFlag;

	/**
	 * @param symbols
	 * @param field
	 * @param zeroFlag
	 */
	protected IncrementVar(String symbols, Field field, String zeroFlag) {
		super(symbols);
		this.field = field;
		this.zeroFlag = zeroFlag;
	}
	
	/**
	 * @param field
	 */
	protected IncrementVar(Field field) {
		super(null);
		this.field = field;
	}

	@Override
	public String generate() {
		int iAuto = 1;
		if (field != null) {
			String key = ckey();
			Object keyLock = null;
			synchronized (LOCKs) {
				keyLock = LOCKs.get(key);
				if (keyLock == null) {
					keyLock = new Object();
					LOCKs.put(key, keyLock);
				}
			}
			
			synchronized (keyLock) {
				Object val = Application.getCommonCache().get(key);
				if (val != null) {
					iAuto = ObjectUtils.toInt(val);
				} else {
					iAuto = count();
				}
				iAuto += 1;
				Application.getCommonCache().put(key, iAuto);
			}
		}
		return StringUtils.leftPad(iAuto + "", getSymbols().length(), '0');
	}
	
	/**
	 * 清空序号缓存
	 */
	protected void clean() {
		String key = ckey();
		Object keyLock = null;
		synchronized (LOCKs) {
			keyLock = LOCKs.get(key);
			if (keyLock == null) {
				keyLock = new Object();
				LOCKs.put(key, keyLock);
			}
		}
		
		synchronized (keyLock) {
			Application.getCommonCache().evict(key);
		}
	}
	
	/**
	 * @return
	 */
	private int count() {
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
	
	/**
	 * @return
	 */
	private String ckey() {
		Assert.notNull(field, "'field' not be null");
		return String.format("SERIES-%s-%s", field.getOwnEntity().getName(), field.getName());
	}
}
