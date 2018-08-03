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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.metadata.ExtRecordCreator;

/**
 * 列表查询解析
 * 
 * @author Zhao Fangfang
 * @version $Id: QueryParser.java 1430 2015-04-16 11:29:21Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-20
 */
public class JsonQueryParser {

	protected JSONObject queryElement;
	
	private DataListControl dataListControl;
	
	private Entity entity;
	private String sql;
	private String countSql;
	private int[] limit;
	private boolean reload;
	
	/**
	 * @param queryElement
	 * @param dataListControl
	 */
	public JsonQueryParser(JSONObject queryElement, DataListControl dataListControl) {
		this.queryElement = queryElement;
		this.dataListControl = dataListControl;
		this.entity = EntityHelper.getEntity(queryElement.getString("entity"));
	}
	
	/**
	 * @return
	 */
	public Entity getEntity() {
		return entity;
	}
	
	/**
	 * @return
	 */
	public String toSql() {
		doParseIfNeed();
		return sql;
	}
	
	/**
	 * @return
	 */
	public String toSqlCount() {
		doParseIfNeed();
		return countSql;
	}
	
	/**
	 * @return
	 */
	public int[] getSqlLimit() {
		doParseIfNeed();
		return limit;
	}
	
	/**
	 * @return
	 */
	public boolean isNeedReload() {
		doParseIfNeed();
		return reload;
	}
	
	/**
	 * 解析 SQL
	 */
	protected void doParseIfNeed() {
		if (sql != null) {
			return;
		}
		
		StringBuffer sqlBase = new StringBuffer("select ");
		JSONArray fieldsNode = queryElement.getJSONArray("fields");
		for (Object o : fieldsNode) {
			String field = o.toString();
			sqlBase.append(StringEscapeUtils.escapeSql(field)).append(',');
		}
		String pkName = entity.getPrimaryField().getName();
		sqlBase.append(pkName)
				.append(" from ")
				.append(entity.getName());
		
		StringBuffer sqlWhere = new StringBuffer(" where (1=1)");
		if (dataListControl.getDefaultFilter() != null) {
			sqlWhere.append('(').append(dataListControl.getDefaultFilter()).append(')');
		}
		
		JSONArray filterNode = queryElement.getJSONArray("filter");
		if (filterNode != null && !filterNode.isEmpty()) {
			sqlWhere.append(parseFilter(filterNode));
		}
		sqlBase.append(sqlWhere);
		
		StringBuffer sqlSort = new StringBuffer(" order by ");
		if (dataListControl.getDefaultFilter() != null) {
			sqlWhere.append('(').append(dataListControl.getDefaultFilter()).append(')');
		}
		
		JSONArray sortNode = queryElement.getJSONArray("sort");
		if (sortNode != null && !sortNode.isEmpty()) {
			sqlSort.append(parseSort(sortNode));
		} else if (entity.containsField(ExtRecordCreator.modifiedOn)) {
			sqlSort.append(ExtRecordCreator.modifiedOn + " desc");
		} else if (entity.containsField(ExtRecordCreator.createdOn)) {
			sqlSort.append(ExtRecordCreator.createdOn + " desc");
		}
		sqlBase.append(sqlSort);
		
		this.sql = sqlBase.toString();
		this.countSql = new StringBuffer("select ")
				.append("count(").append(pkName).append(')')
				.append(" from ")
				.append(entity.getName())
				.append(sqlWhere)
				.toString();
		
		int pageNo = NumberUtils.toInt(queryElement.getString("pageNo"), 1);
		int pageSize = NumberUtils.toInt(queryElement.getString("pageSize"), 20);
		this.limit = new int[] { pageSize, pageNo * pageSize - pageSize };
		this.reload = limit[1] == 0;
		if (!reload) {
			reload = BooleanUtils.toBoolean(queryElement.getString("reload"));
		}
	}
	
	/**
	 * @param filterNode
	 * @return
	 */
	protected String parseFilter(JSONArray filterNode) {
		StringBuffer sb = new StringBuffer();
		for (Object o : filterNode) {
			JSONObject el = (JSONObject) o;
			String field = el.getString("field");
			String value = el.getString("value");
			String op = el.getString("op");
			op = convOp(op);
			
			if ("in".equals(op) || "exists".equals(op)) {
				throw new UnsupportedOperationException("Unsupported 'in' or 'exists'");
			} else {
				sb.append(" and ")
						.append(field)
						.append(' ')
						.append(op)
						.append(' ');
				if (NumberUtils.isDigits(value)) {
					sb.append(value);
				} else if (StringUtils.isNotBlank(value)) {
					sb.append('\'').append(StringEscapeUtils.escapeSql(value)).append('\'');
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * @param sortNode
	 * @return
	 */
	protected String parseSort(JSONArray sortNode) {
		StringBuffer sb = new StringBuffer(); 
		int index = 0;
		for (Object o : sortNode) {
			JSONObject el = (JSONObject) o;
			String field = el.getString("field");
			String type = BooleanUtils.toBoolean(el.getString("ascending")) ? " asc" : " desc";
			
			sb.append(index++ == 0 ? ' ' : ',')
					.append(field)
					.append(type);
		}
		return sb.toString();
	}
	
	/**
	 * @param op
	 * @return
	 */
	protected String convOp(String op) {
		if ("eq".equals(op)) return "=";
		if ("neq".equals(op)) return "<>";
		if ("gt".equals(op)) return ">";
		if ("lt".equals(op)) return "<";
		if ("ge".equals(op)) return ">=";
		if ("le".equals(op)) return "<=";
		if ("nl".equals(op)) return "is null";
		if ("nt".equals(op)) return "is not null";
		if ("lk".equals(op)) return "like";
		if ("nlk".equals(op)) return "not like";
		if ("gb".equals(op)) return "group by";
		return op;
	}
}
