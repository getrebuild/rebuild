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

package com.rebuild.web.base;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.rebuild.server.helper.state.StateHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 元数据获取
 * 
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/commons/metadata/")
public class MetadataGetting extends BaseControll {

	@RequestMapping("entities")
	public void entities(HttpServletRequest request, HttpServletResponse response) {
		ID user = getRequestUser(request);
		boolean includeSlave = getBoolParameter(request, "slave", false);
		List<Map<String, String>> list = new ArrayList<>();
		for (Entity e : MetadataSorter.sortEntities(user)) {
			// 是否返回明细实体
			if (e.getMasterEntity() != null && !includeSlave) {
				continue;
			}
			
			Map<String, String> map = new HashMap<>();
			EasyMeta easy = new EasyMeta(e);
			map.put("name", e.getName());
			map.put("label", easy.getLabel());
			map.put("icon", easy.getIcon());
			list.add(map);
		}
		writeSuccess(response, list);
	}
	
	@RequestMapping("fields")
	public void fields(HttpServletRequest request, HttpServletResponse response) {
		String entity = getParameterNotNull(request, "entity");
		Entity entityMeta = MetadataHelper.getEntity(entity);
		boolean appendRefFields = "2".equals(getParameter(request, "deep"));
		String filterKey = getParameter(request, "filter");
		
		List<Map<String, Object>> fsList = new ArrayList<>();
		putFields(fsList, entityMeta, appendRefFields, filterKey);

		// 追加二级引用字段
		if (appendRefFields) {
			for (Field field : entityMeta.getFields()) {
				if (EasyMeta.getDisplayType(field) != DisplayType.REFERENCE) {
					continue;
				}

				int entityCode = field.getReferenceEntity().getEntityCode();
                if (MetadataHelper.isBizzEntity(entityCode) || entityCode == EntityHelper.RobotApprovalConfig) {
                    continue;
                }

                fsList.add(buildField(field));
                putFields(fsList, field, false, filterKey);
			}
		}

		writeSuccess(response, fsList);
	}

	/**
	 * @param dest
	 * @param entityOrField
	 * @param filterRefField
	 * @param filterKey
	 */
	private void putFields(List<Map<String, Object>> dest, BaseMeta entityOrField, boolean filterRefField, String filterKey) {
	    Field parentField = null;
	    Entity useEntity;
	    if (entityOrField instanceof Field) {
            parentField = (Field) entityOrField;
            useEntity = parentField.getReferenceEntity();
        } else {
	        useEntity = (Entity) entityOrField;
        }

		for (Field field : MetadataSorter.sortFields(useEntity)) {
			if ("SEARCH".equalsIgnoreCase(filterKey)) {
				if (!field.isQueryable() || EasyMeta.getDisplayType(field) == DisplayType.BARCODE) {
					continue;
				}
			}

			Map<String, Object> map = buildField(field);

			// 引用字段处理
			if (EasyMeta.getDisplayType(field) == DisplayType.REFERENCE && filterRefField) {
                boolean isApprovalId = field.getName().equalsIgnoreCase(EntityHelper.ApprovalId);
				boolean isBizz = MetadataHelper.isBizzEntity(field.getReferenceEntity().getEntityCode());
                if (!(isApprovalId || isBizz)) {
				    continue;
                }
			}

			if (parentField != null) {
				map.put("name", parentField.getName() + "." + map.get("name"));
				map.put("label", EasyMeta.getLabel(parentField) + "." + map.get("label"));
			}

			dest.add(map);
		}
	}

    /**
     * @param field
     * @return
     */
	public static Map<String, Object> buildField(Field field) {
        Map<String, Object> map = new HashMap<>();
        EasyMeta easyField = EasyMeta.valueOf(field);
        map.put("name", field.getName());
        map.put("label", easyField.getLabel());
        map.put("type", easyField.getDisplayType().name());
        map.put("nullable", field.isNullable());
        map.put("creatable", field.isCreatable());
        map.put("updatable", field.isUpdatable());

        DisplayType dt = EasyMeta.getDisplayType(field);
        if (dt == DisplayType.REFERENCE) {
            Entity refEntity = field.getReferenceEntity();
            Field nameField  = MetadataHelper.getNameField(refEntity);
            map.put("ref", new String[] { refEntity.getName(), EasyMeta.getDisplayType(nameField).name() });
        } else if (dt == DisplayType.STATE) {
            map.put("stateClass", StateHelper.getSatetClass(field).getName());
        }
        return map;
    }
	
	// 哪些实体引用了指定实体
	@RequestMapping("references")
	public void references(HttpServletRequest request, HttpServletResponse response) {
		String entity = getParameterNotNull(request, "entity");
		Entity entityMeta = MetadataHelper.getEntity(entity);
		
		Set<Entity> references = new HashSet<>();
		for (Field field : entityMeta.getReferenceToFields()) {
			Entity own = field.getOwnEntity();
			if (!(MetadataHelper.isSlaveEntity(own.getEntityCode()) || field.getType() == FieldType.ANY_REFERENCE)) {
				references.add(own);
			}
		}

		List<String[]> list = new ArrayList<>();
		for (Entity e : references) {
			EasyMeta easy = new EasyMeta(e);
			list.add(new String[] { easy.getName(), easy.getLabel() });
		}
		writeSuccess(response, list);
	}
}
