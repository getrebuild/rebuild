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

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.support.QueryHelper;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.query.AdvFilterParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * 批量操作
 *
 * @author devezhao
 * @since 10/16/2018
 */
public abstract class BulkOperator extends HeavyTask<Integer> {

	protected static final Log LOG = LogFactory.getLog(BulkOperator.class);
	
	final protected BulkContext context;
	final protected GeneralEntityService ges;
	
	private ID[] records;
	
	/**
	 * @param context
	 * @param ges 可避免多次经由拦截器检查
	 */
	protected BulkOperator(BulkContext context, GeneralEntityService ges) {
		super();
		this.context = context;
		this.ges = ges;
	}
	
	/**
	 * 获取待操作记录
	 * 
	 * @return
	 */
	protected ID[] prepareRecords() {
		if (this.records != null) {
			return this.records;
		}
		
		if (context.getRecords() != null) {
			this.records = context.getRecords();
			setTotal(this.records.length);
			return this.records;
		}

		JSONObject asFilterExp = context.getCustomData();
		AdvFilterParser filterParser = new AdvFilterParser(asFilterExp);
		String sqlWhere = filterParser.toSqlWhere();

		Entity entity = MetadataHelper.getEntity(asFilterExp.getString("entity"));
		String sql = "select %s from %s where (1=1) and " + sqlWhere;
		sql = String.format(sql, entity.getPrimaryField().getName(), entity.getName());
        return readIDArray(sql, context.getOpUser());
	}

    /**
     * 读取 ID[]
     *
     * @param sql
     * @param user
     * @return
     */
	public static ID[] readIDArray(String sql, ID user) {
        Query query = Application.getQueryFactory().createQuery(sql, user);
        Object[][] array = QueryHelper.readArray(query);

        Set<ID> ids = new HashSet<>();
        for (Object[] o : array) {
            ids.add((ID) o[0]);
        }
        return ids.toArray(new ID[0]);
    }
}
