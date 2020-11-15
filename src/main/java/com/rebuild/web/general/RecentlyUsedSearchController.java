/*
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
import com.rebuild.core.service.general.RecentlyUsedHelper;
import com.rebuild.core.support.general.FieldValueWrapper;
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
 * 非自动，需要调用 <tt>recently-add</tt> 方法手动添加方可用，后期考虑自动化
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
        return formatSelect2(recently, getLang(request, "RecentlyUsed"));
    }

    @PostMapping("recently-add")
    public RespBody addRecently(HttpServletRequest request) {
        ID id = getIdParameterNotNull(request, "id");
        String type = getParameter(request, "type");

        RecentlyUsedHelper.add(getRequestUser(request), id, type);
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
     * @param groupName select2 分组 null 表示无分组
     * @return
     */
    static JSONArray formatSelect2(ID[] idLabels, String groupName) {
        JSONArray data = new JSONArray();
        for (ID id : idLabels) {
            String label = id.getLabel();
            if (StringUtils.isBlank(label)) {
                label = FieldValueWrapper.NO_LABEL_PREFIX + id.toLiteral().toUpperCase();
            }

            data.add(JSONUtils.toJSONObject(
                    new String[] { "id", "text" },
                    new String[] { id.toLiteral(), label }));
        }

        if (groupName != null) {
            JSONObject group = new JSONObject();
            group.put("text", groupName);
            group.put("children", data);

            JSONArray array = new JSONArray();
            array.add(group);
            data = array;
        }
        return data;
    }
}
