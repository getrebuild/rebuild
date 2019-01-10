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

package com.rebuild.server.business.datas;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.SystemConfiguration;
import com.rebuild.server.metadata.MetadataHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author devezhao
 * @since 01/10/2019
 */
public class ImportsEnter {

	private File sourceFile;
	private Entity toEntity;

	private int repeatOpt;
	private Field[] repeatFields;

	private ID defaultOwningUser;

	private Map<Field, Integer> filedsMapping;

	/**
	 * @param sourceFile
	 * @param toEntity
	 * @param repeatOpt
	 * @param repeatFields
	 * @param defaultOwningUser
	 * @param filedsMapping
	 */
	protected ImportsEnter(File sourceFile, Entity toEntity, int repeatOpt, Field[] repeatFields, ID defaultOwningUser,
			Map<Field, Integer> filedsMapping) {
		this.sourceFile = sourceFile;
		this.toEntity = toEntity;
		this.repeatOpt = repeatOpt;
		this.repeatFields = repeatFields;
		this.defaultOwningUser = defaultOwningUser;
		this.filedsMapping = filedsMapping;
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public Entity getToEntity() {
		return toEntity;
	}

	public int getRepeatOpt() {
		return repeatOpt;
	}

	public Field[] getRepeatFields() {
		return repeatFields;
	}

	public ID getDefaultOwningUser() {
		return defaultOwningUser;
	}

	public Map<Field, Integer> getFiledsMapping() {
		return filedsMapping;
	}

	// --

	/**
	 * @param json
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static ImportsEnter parse(JSONObject json) throws IllegalArgumentException {
		Assert.notNull(json.getString("file"), "Node `file`");
		Assert.notNull(json.getString("entity"), "Node `entity`");
		Assert.notNull(json.getInteger("repeat_opt"), "Node `repeat_opt`");
		Assert.notNull(json.getJSONObject("fields_mapping"), "Node `fields_mapping`");

		File file = SystemConfiguration.getFileOfTemp(json.getString("file"));
		if (!file.exists()) {
			throw new IllegalArgumentException("File unexists : " + file);
		}
		Entity entity = MetadataHelper.getEntity(json.getString("entity"));

		int repeatOpt = json.getIntValue("repeat_opt");
		Field[] repeatFields = null;
		if (repeatOpt != 3) {
			Assert.notNull(json.getJSONArray("repeat_fields"), "Node `repeat_fields`");
			Set<Field> rfs = new HashSet<>();
			for (Object field : json.getJSONArray("repeat_fields")) {
				rfs.add(entity.getField((String) field));
			}
			Assert.isTrue(!rfs.isEmpty(), "Node `repeat_fields`");
			repeatFields = rfs.toArray(new Field[rfs.size()]);
		}
		
		String user = json.getString("owning_user");
		ID ownUser = ID.isId(user) ? ID.valueOf(user) : null;

		JSONObject fm = json.getJSONObject("fields_mapping");
		Map<Field, Integer> filedsMapping = new HashMap<>();
		for (Map.Entry<String, Object> e : fm.entrySet()) {
			filedsMapping.put(entity.getField(e.getKey()), (Integer) e.getValue());
		}
		
		return new ImportsEnter(file, entity, repeatOpt, repeatFields, ownUser, filedsMapping);
	}
}
