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

import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.engine.ID;

/**
 * 通知消息
 * 
 * @author devezhao
 * @since 10/17/2018
 */
public class Message {

	private ID fromUser;
	private ID toUser;
	private String message;
	
	private ID relatedRecord;

	public Message(ID toUser, String message, ID relatedRecord) {
		this(UserService.SYSTEM_USER, toUser, message, relatedRecord);
	}

	public Message(ID fromUser, ID toUser, String message, ID relatedRecord) {
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.message = message;
		this.relatedRecord = relatedRecord;
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
}
