/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package cn.devezhao.rebuild.web.entity.datalist;

import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.query.compiler.SelectItem;
import cn.devezhao.rebuild.server.Application;

/**
 * 数据列表控制器
 * 
 * @author Zhao Fangfang
 * @version $Id: DefaultGridDataControl.java 1236 2015-03-20 07:58:33Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-20
 */
public class DefaultDataListControl implements DataListControl {

	protected JsonQueryParser queryParser;

	/**
	 */
	protected DefaultDataListControl() {
	}
	
	/**
	 * @param queryElement
	 */
	public DefaultDataListControl(JSONObject queryElement) {
		this.queryParser = new JsonQueryParser(queryElement, this);
	}

	/**
	 * @return
	 */
	public JsonQueryParser getQueryParser() {
		return queryParser;
	}

	@Override
	public String getDefaultFilter() {
		return null;
	}
	
	@Override
	public String getResult() {
		int timeout = 10 * 1000;
		int total = 0;
		if (queryParser.isNeedReload()) {
			String countSql = queryParser.toSqlCount();
			total = ((Long) Application.createQuery(countSql).unique()[0]).intValue();
		}
		
		Query query = Application.createQuery(queryParser.toSql()).setTimeout(timeout);
		int[] limit = queryParser.getSqlLimit();
		Object[][] array = query.setLimit(limit[0], limit[1]).array();
		
		DataWrapper wrapper = getDataWrapper(total, array, query.getSelectItems());
		return wrapper.toJson();
	}

	/**
	 * @param total
	 * @param data
	 * @param fields
	 * @return
	 */
	protected DataWrapper getDataWrapper(int total, Object[][] data, SelectItem[] fields) {
		return new DataWrapper(total, data, fields);
	}
}
