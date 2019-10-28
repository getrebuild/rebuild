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

import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.metadata.entity.EasyMeta;

/**
 * 自动编号工厂类
 * 
 * @author devezhao
 * @since 12/24/2018
 */
public class SeriesGeneratorFactory {

	/**
	 * @param field
	 * @return
	 */
	public static SeriesGenerator create(Field field) {
		return new SeriesGenerator(EasyMeta.valueOf(field));
	}
	
	/**
	 * 生成
	 * 
	 * @param field
	 * @return
	 */
	public static String generate(Field field) {
		return create(field).generate();
	}
	
	/**
	 * 预览
	 * 
	 * @param config
	 * @return
	 */
	public static String preview(JSONObject config) {
		return new SeriesGenerator(null, config).generate();
	}
	
	/**
	 * 重置序号
	 * 
	 * @param field
	 */
	protected static void zero(Field field) {
		new IncreasingVar(field).clean();
	}
}
