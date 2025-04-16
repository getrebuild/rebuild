/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.RecentlyUsedHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 最近搜索（针对引用字段）。
 * 非自动，需要调用 `recently-add` 方法手动添加方可用，后期考虑自动化
 *
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/25
 */
@RestController
@RequestMapping("/commons/search/")
public class RecentlyUsedSearchController extends BaseController {

    @GetMapping("recently")
    public JSON fetchRecently(HttpServletRequest request) {
        String entity = getParameterNotNull(request, "entity");
        String type = getParameter(request, "type");

        ID[] recently = RecentlyUsedHelper.gets(getRequestUser(request), entity, type);
        return formatSelect2(recently, Language.L("最近使用"));
    }

    @PostMapping("recently-add")
    public RespBody addRecently(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String ids = getParameterNotNull(request, "id");
        String type = getParameter(request, "type");

        for (String id : ids.split(",")) {
            if (ID.isId(id)) {
                RecentlyUsedHelper.add(user, ID.valueOf(id), type);
            }
        }

        return RespBody.ok();
    }

    @PostMapping("recently-clean")
    public RespBody cleanRecently(HttpServletRequest request) {
        String entity = getParameterNotNull(request, "entity");
        String type = getParameter(request, "type");

        RecentlyUsedHelper.clean(getRequestUser(request), entity, type);
        return RespBody.ok();
    }

    /**
     * 格式化成前端 `select2` 组件数据格式
     *
     * @param idLabels
     * @param useGroupName select2 分组 null 表示无分组
     * @return
     */
    static JSONArray formatSelect2(ID[] idLabels, String useGroupName) {
        JSONArray data = new JSONArray();
        boolean isClazz = idLabels.length > 0 && idLabels[0].getEntityCode() == EntityHelper.ClassificationData;
        for (ID id : idLabels) {
            String label = id.getLabel();
            if (StringUtils.isBlank(label)) {
                label = FieldValueHelper.NO_LABEL_PREFIX + id.toLiteral().toUpperCase();
            }

            ClassificationManager.Item item = isClazz ? ClassificationManager.instance.getItem(id) : null;
            data.add(JSONUtils.toJSONObject(
                    new String[] { "id", "text", "code" },
                    new Object[] { id, label, item == null ? null : item.getCode() }));
        }

        if (useGroupName != null) {
            JSONObject group = new JSONObject();
            group.put("text", useGroupName);
            group.put("children", data);

            JSONArray array = new JSONArray();
            array.add(group);
            data = array;
        }
        return data;
    }
}
