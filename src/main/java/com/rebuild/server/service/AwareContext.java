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

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao
 * @since 10/31/2018
 */
public class AwareContext {

	// 操作人
	private ID editor;
	// 动作
	private Permission action;

	// 记录 ID
	private ID recordId;
	// 记录（与记录 ID 两者二选一）
	private Record record;

	private AwareContext(ID editor, Permission action, ID recordId) {
		super();
		this.editor = editor;
		this.action = action;
		this.recordId = recordId;
	}

	private AwareContext(ID editor, Permission action, Record record) {
		super();
		this.editor = editor;
		this.action = action;
		this.record = record;
	}

	public ID getEditor() {
		return editor;
	}

	public Permission getAction() {
		return action;
	}

	public ID getRecordId() {
		return recordId;
	}

	public Record getRecord() {
		return record;
	}
	
	@Override
	public String toString() {
		String astr = "{ Editor: %s, Action: %s, Record: %s }";
		return String.format(astr, getEditor(), getAction().getName(), getRecordId() != null ? getRecordId() : getRecord().getPrimary());
	}
	
	// --
	
	public static AwareContext valueOf(ID editor, Permission action, ID recordId) {
		return new AwareContext(editor, action, recordId);
	}

	public static AwareContext valueOf(ID editor, Permission action, Record record) {
		return new AwareContext(editor, action, record);
	}
}
