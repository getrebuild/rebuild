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

package com.rebuild.web.admin.robot;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.business.trigger.impl.FieldAggregation;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.helper.state.StateHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/admin/robot/trigger/")
public class FieldAggregationControll extends BaseControll {
	
	@RequestMapping("field-aggregation-entities")
	public void getTargetEntity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity sourceEntity = MetadataHelper.getEntity(getParameterNotNull(request, "source"));
		
		List<String[]> entities = new ArrayList<>();
		for (Field refField : MetadataSorter.sortFields(sourceEntity, DisplayType.REFERENCE)) {
			if (MetadataHelper.isApprovalField(refField.getName())) {
				continue;
			}

			Entity refEntity = refField.getReferenceEntity();
			String entityLabel = EasyMeta.getLabel(refEntity) + " (" + EasyMeta.getLabel(refField) + ")";
			entities.add(new String[] { refEntity.getName(), entityLabel, refField.getName() });
		}

		// 可归集到自己（通过主键字段）
		entities.add(new String[] { sourceEntity.getName(), EasyMeta.getLabel(sourceEntity), FieldAggregation.SOURCE_SELF});

		writeSuccess(response, entities);
	}
	
	@RequestMapping("field-aggregation-fields")
	public void getTargetField(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity sourceEntity = MetadataHelper.getEntity(getParameterNotNull(request, "source"));
		String target = getParameter(request, "target");
		Entity targetEntity = StringUtils.isBlank(target) ? null : MetadataHelper.getEntity(target);

		final DisplayType[] allowTypes = new DisplayType[] { DisplayType.NUMBER, DisplayType.DECIMAL };

		List<String[]> sourceFields = new ArrayList<>();
		List<String[]> targetFields = new ArrayList<>();

		// 源字段

		for (Field field : MetadataSorter.sortFields(sourceEntity.getFields(), allowTypes)) {
			sourceFields.add(buildField(field, false));
		}
		// 关联实体
		for (Field fieldRef : MetadataSorter.sortFields(sourceEntity.getFields(), DisplayType.REFERENCE)) {
			String fieldRefName = fieldRef.getName() + ".";
			String fieldRefLabel = EasyMeta.getLabel(fieldRef) + ".";
			for (Field field : MetadataSorter.sortFields(fieldRef.getReferenceEntity(), allowTypes)) {
				String[] build = buildField(field, false);
				build[0] = fieldRefName + build[0];
				build[1] = fieldRefLabel + build[1];
				sourceFields.add(build);
			}
		}

		// 目标字段

		if (targetEntity != null) {
			for (Field field : MetadataSorter.sortFields(targetEntity.getFields(), allowTypes)) {
				if (EasyMeta.valueOf(field).isBuiltin()) {
					continue;
				}
				targetFields.add(buildField(field, false));
			}
		}

		// 审批流程启用
		boolean hadApproval = targetEntity != null && RobotApprovalManager.instance.hadApproval(targetEntity, null) != null;

		JSON data = JSONUtils.toJSONObject(
				new String[] { "source", "target", "hadApproval" },
				new Object[] {
						sourceFields.toArray(new String[sourceFields.size()][]),
						targetFields.toArray(new String[targetFields.size()][]),
						hadApproval});
		writeSuccess(response, data);
	}

	/**
	 * @param field
	 * @return
	 * @see com.rebuild.web.base.MetadataGetting#buildField(Field)
	 */
	protected static String[] buildField(Field field, boolean includesType) {
		EasyMeta easyField = EasyMeta.valueOf(field);
		if (!includesType) {
			return new String[] { field.getName(), easyField.getLabel() };
		}

		DisplayType dt = easyField.getDisplayType();
		String typeExt = null;
		if (dt == DisplayType.REFERENCE) {
			typeExt = field.getReferenceEntity().getName();
		} else if (dt == DisplayType.STATE) {
			typeExt = StateHelper.getSatetClass(field).getName();
		} else if (dt == DisplayType.ID) {
			dt = DisplayType.REFERENCE;
			typeExt = field.getOwnEntity().getName();
		}
		return new String[] { field.getName(), easyField.getLabel(), dt.name(), typeExt };
	}
}
