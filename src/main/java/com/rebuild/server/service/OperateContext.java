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

package com.rebuild.server.service;

import org.springframework.util.Assert;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 记录操作上下文
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public class OperateContext {

	private ID operator;
	private Permission action;
	
	private Record beforeRecord;
	private Record afterRecord;

	private OperateContext(ID operator, Permission action, Record beforeRecord, Record afterRecord) {
		this.operator = operator;
		this.action = action;
		
		Assert.isTrue(beforeRecord != null || afterRecord != null, "'beforeRecord' 或 'afterRecord' 至少有一个不为空");
		this.beforeRecord = beforeRecord;
		this.afterRecord = afterRecord;
	}

	/**
	 * 操作人
	 * 
	 * @return
	 */
	public ID getOperator() {
		return operator;
	}
	
	/**
	 * 操作类型
	 * 
	 * @return
	 */
	public Permission getAction() {
		return action;
	}

	/**
	 * 操作执行*前*的记录
	 * 
	 * @return
	 */
	public Record getBeforeRecord() {
		return beforeRecord;
	}
	
	/**
	 * 操作执行*后*的记录
	 * 
	 * @return
	 */
	public Record getAfterRecord() {
		return afterRecord;
	}
	
	/**
	 * 记录ID
	 * 
	 * @return
	 */
	public ID getRecordId() {
		return (getBeforeRecord() != null ? getBeforeRecord() : getAfterRecord()).getPrimary();
	}
	
	@Override
	public String toString() {
		String astr = "{ Operator: %s, Action: %s, Record: %s }";
		return String.format(astr, getOperator(), getAction().getName(), getRecordId());
	}
	
	/**
	 * @param operator
	 * @param action
	 * @param before
	 * @param after
	 * @return
	 */
	public static OperateContext valueOf(ID operator, Permission action, Record before, Record after) {
		return new OperateContext(operator, action, before, after);
	}
}
