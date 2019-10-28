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

package com.rebuild.server.service.notification;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.BaseService;

/**
 * 消息通知服务
 * 
 * @author devezhao
 * @since 10/17/2018
 */
public class NotificationService extends BaseService {

	public NotificationService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int getEntityCode() {
		return EntityHelper.Notification;
	}
	
	@Override
	public Record create(Record record) {
		record.setBoolean("unread", true);
		record = super.create(record);
		cleanCache(record.getPrimary());
		return record;
	}
	
	@Override
	public Record update(Record record) {
		cleanCache(record.getPrimary());
		return super.update(record);
	}
	
	@Override
	public int delete(ID recordId) {
		cleanCache(recordId);
		return super.delete(recordId);
	}
	
	// 清理缓存
	private void cleanCache(ID messageId) {
		Object m[] = Application.createQueryNoFilter(
				"select toUser from Notification where messageId = ?")
				.setParameter(1, messageId)
				.unique();
		if (m != null) {
			final String ckey = "UnreadNotification-" + m[0];
			Application.getCommonCache().evict(ckey);	
		}
	}
	
	/**
	 * 发送消息
	 * 
	 * @param message
	 */
	public void send(Message message) {
		Record record = EntityHelper.forNew(EntityHelper.Notification, message.getFromUser());
		record.setID("fromUser", message.getFromUser());
		record.setID("toUser", message.getToUser());
		record.setString("message", message.getMessage());
		if (message.getType() > 0) {
			record.setInt("type", message.getType());
		}
		if (message.getRelatedRecord() != null) {
			record.setID("relatedRecord", message.getRelatedRecord());
		}
		this.create(record);
	}

	/**
	 * @param user
	 * @return
	 */
	public int getUnreadMessage(ID user) {
		final String ckey = "UnreadNotification-" + user;
		Object cval = Application.getCommonCache().getx(ckey);
		if (cval != null) {
			return (Integer) cval;
		}
		
		Object[] unread = Application.createQueryNoFilter(
				"select count(messageId) from Notification where toUser = ? and unread = 'T'")
				.setParameter(1, user)
				.unique();
		int count = unread == null ? 0 : ObjectUtils.toInt(unread[0]);
		Application.getCommonCache().putx(ckey, count);
		return count;
	}
}
