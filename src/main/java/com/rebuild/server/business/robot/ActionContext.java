/*
rebuild - Building your business-systems freely.
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

package com.rebuild.server.business.robot;

import com.alibaba.fastjson.JSON;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 触发动作执行上下文
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/27
 */
public class ActionContext {
	
	final private ID sourceRecord;
	final private Entity sourceEntity;
	final private JSON actionContent;
	
	/**
	 * @param sourceEntity
	 * @param actionContent
	 */
	public ActionContext(ID sourceRecord, Entity sourceEntity, JSON actionContent) {
		this.sourceRecord = sourceRecord;
		this.sourceEntity = sourceRecord != null ? MetadataHelper.getEntity(sourceRecord.getEntityCode()) : sourceEntity;
		this.actionContent = actionContent;
	}
	
	/**
	 * 触发源实体
	 * 
	 * @return
	 */
	public Entity getSourceEntity() {
		return sourceEntity;
	}
	
	/**
	 * 触发源记录
	 * 
	 * @return
	 */
	public ID getSourceRecord() {
		return sourceRecord;
	}
	
	/**
	 * 触发内容
	 * 
	 * @return
	 */
	public JSON getActionContent() {
		return actionContent;
	}
}
