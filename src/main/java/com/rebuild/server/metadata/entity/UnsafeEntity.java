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

package com.rebuild.server.metadata.entity;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.impl.EntityImpl;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/04/2018
 */
public class UnsafeEntity extends EntityImpl {
	private static final long serialVersionUID = 2107073554299141281L;

	protected UnsafeEntity(String entityName, String physicalName, String entityLabel, int typeCode, String nameField) {
		super(entityName, physicalName, entityLabel, typeCode, nameField);
	}
	
	@Override
	protected void addField(Field field) {
		super.addField(field);
	}
}
