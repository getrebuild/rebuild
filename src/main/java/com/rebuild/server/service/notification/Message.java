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

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.service.bizz.UserService;

/**
 * 通知消息。
 * 注意：若指定了 relatedRecord 相关记录删除时消息也将被删除
 * 
 * @author devezhao
 * @since 10/17/2018
 */
public class Message {
	
	// 一般消息
	public static final int TYPE_DEFAULT = 0;
	
	// 分配消息
	public static final int TYPE_ASSIGN = 10;
	// 共享消息
	public static final int TYPE_SAHRE = 11;
	
	// 审批消息
	public static final int TYPE_APPROVAL = 20;

	private ID fromUser;
	private ID toUser;
	private String message;
	private ID relatedRecord;
	private int type;
	
	/**
	 * @param fromUser
	 * @param toUser
	 * @param message
	 * @param relatedRecord
	 * @param type
	 */
	public Message(ID fromUser, ID toUser, String message, ID relatedRecord, int type) {
		this.fromUser = fromUser == null ? UserService.SYSTEM_USER : fromUser;
		this.toUser = toUser;
		this.message = message;
		this.relatedRecord = relatedRecord;
		this.type = type;
	}

	public ID getFromUser() {
		return fromUser;
	}

	public ID getToUser() {
		return toUser;
	}
	
	public ID getRelatedRecord() {
		return relatedRecord;
	}

	public String getMessage() {
		return message;
	}
	
	public int getType() {
		return type;
	}
}
