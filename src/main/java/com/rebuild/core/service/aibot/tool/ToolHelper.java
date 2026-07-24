/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 工具通用帮助类
 *
 * @author devezhao
 * @since 2026/7/24
 */
public class ToolHelper {

    private ToolHelper() {}

    /**
     * 解析文件 key 参数（支持单个字符串或数组）
     *
     * @param value 字符串或 JSONArray
     * @return JSON 数组字符串，如 ["rb/xxx.jpg"]；无效时返回 null
     */
    public static String resolveFileKeys(Object value) {
        if (value == null) return null;

        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            return arr.isEmpty() ? null : arr.toJSONString();
        }

        String str = value.toString().trim();
        if (str.isEmpty()) return null;

        // 单个 fileKey 字符串，包装为数组
        return JSON.toJSONString(new String[]{str});
    }

    /**
     * 解析实体（支持名称、code、标签匹配）
     * 精确匹配优先，多个模糊匹配时抛出异常供用户选择
     *
     * @param name 实体名称、code 或标签
     * @return 匹配的实体，未找到返回 null
     */
    public static Entity resolveEntity(String name) {
        if (StringUtils.isBlank(name)) return null;

        // 1. 精确匹配实体名称
        if (MetadataHelper.containsEntity(name)) {
            return MetadataHelper.getEntity(name);
        }

        // 2. CODE
        if (StringUtils.isNumeric(name)) {
            int code = Integer.parseInt(name);
            if (MetadataHelper.containsEntity(code)) {
                return MetadataHelper.getEntity(code);
            }
        }

        // 3. 标签匹配
        String nameLower = name.toLowerCase();
        List<Entity> fuzzyMatches = new ArrayList<>();

        for (Entity e : MetadataHelper.getEntities()) {
            String label = EasyMetaFactory.getLabel(e);
            if (StringUtils.isBlank(label)) continue;

            // 精确标签匹配，直接返回
            if (label.equalsIgnoreCase(name)) {
                return e;
            }
            // 模糊匹配
            if (label.toLowerCase().contains(nameLower)
                    || nameLower.contains(label.toLowerCase())) {
                fuzzyMatches.add(e);
            }
        }

        if (fuzzyMatches.isEmpty()) return null;
        if (fuzzyMatches.size() == 1) return fuzzyMatches.get(0);

        // 多个匹配，抛出异常让用户选择
        JSONArray list = new JSONArray();
        for (Entity e : fuzzyMatches) {
            list.add(JSONUtils.toJSONObject(
                    new String[]{"name", "label"},
                    new Object[]{e.getName(), EasyMetaFactory.getLabel(e)}));
        }
        throw new ToolException("匹配到多个实体，请指定更精确的名称: " + list.toJSONString());
    }

    /**
     * 解析用户（支持 ID、全名、用户名）
     *
     * @param userIdent 用户 ID、全名或用户名
     * @return 用户 ID，未找到返回 null
     */
    public static ID resolveUser(String userIdent) {
        if (StringUtils.isBlank(userIdent)) return null;

        if (ID.isId(userIdent)) {
            return ID.valueOf(userIdent);
        }

        // 按全名查找
        ID user = UserHelper.findUserByFullName(userIdent);
        if (user == null && Application.getUserStore().existsName(userIdent)) {
            user = Application.getUserStore().getUser(userIdent).getId();
        }
        return user;
    }
}
