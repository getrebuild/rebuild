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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.task.BulkTask;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.notification.NotificationObserver;
import com.rebuild.server.service.query.AdvFilterParser;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 批量操作
 * 
 * @author devezhao
 * @since 10/16/2018
 */
public abstract class BulkOperator extends BulkTask {

	protected static final Log LOG = LogFactory.getLog(BulkOperator.class);
	
	final protected BulkContext context;
	final protected GeneralEntityService ges;
	
	private ID[] records;
	private int affected = 0;
	
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
	protected ID[] getWillRecords() {
		if (this.records != null) {
			return this.records;
		}
		
		if (context.getRecords() != null) {
			this.records = context.getRecords();
			setTotal(this.records.length);
			return this.records;
		}
		
		JSONObject filterExp = context.getFilterExp();
		
		AdvFilterParser filterParser = new AdvFilterParser(filterExp);
		String sqlWhere = filterParser.toSqlWhere();

		Entity entity = MetadataHelper.getEntity(filterExp.getString("entity"));
		String sql = "select %s from %s where (1=1) and " + sqlWhere;
		sql = String.format(sql, entity.getPrimaryField().getName(), entity.getName());
		
		// TODO 解析过滤并查询结果
		throw new UnsupportedOperationException();
	}
	
	/**
	 * 影响的记录数
	 * 
	 * @return
	 */
	public int getAffected() {
		return affected;
	}
	
	@Override
	public void run() {
		// 分派/共享消息合并发送
		if (context.getAction() == BizzPermission.ASSIGN || context.getAction() == BizzPermission.SHARE) {
			BulkOperatorTx.begin();
			try {
				affected = this.operate();
			} finally {
				BulkOperatorTx.end();
				
				OperatingContext operatingContext = OperatingContext.create(
						context.getOpUser(), context.getAction(), null, null, context.getRecords());
				new NotificationObserver().update(null, operatingContext);
			}
		} else {
			affected = this.operate();
		}
	}
	
	/**
	 * 实际执行
	 * 
	 * @return 执行数量
	 */
	abstract public Integer operate();
}
