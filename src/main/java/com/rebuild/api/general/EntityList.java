/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.server.helper.datalist.DataListControl;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 查询记录列表
 *
 * @author devezhao
 * @since 2020/5/21
 *
 * @see com.rebuild.server.service.query.AdvFilterParser
 * @see com.rebuild.server.helper.datalist.DataListWrapper
 */
public class EntityList extends EntityGet {

    @Override
    protected String getApiName() {
        return "entity/list";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        String entity = context.getParameterNotBlank("entity");
        if (!MetadataHelper.containsEntity(entity)) {
            throw new ApiInvokeException("Unknow entity : " + entity);
        }

        Entity useEntity = MetadataHelper.getEntity(entity);

        String[] fields = context.getParameterNotBlank("fields").split(",");
        fields = getValidFields(useEntity, fields);

        int pageNo = context.getParameterAsInt("page_no", 1);
        int pageSize = context.getParameterAsInt("page_size", 40);
        String sortBy = context.getParameter("sort_by");
        if (sortBy == null) {
            sortBy = EntityHelper.ModifiedOn + ":desc";
        }
        if (!useEntity.containsField(sortBy.split(":")[0])) {
            throw new ApiInvokeException("Unknow field in `sort_by` : " + sortBy.split(":")[0]);
        }

        JSON useFilter = context.getPostData();

        // 优先级高
        String quickName = context.getParameter("q");
        if (StringUtils.isNotBlank(quickName)) {
            JSONObject quickFilter = JSONUtils.toJSONObject(
                    new String[] { "entity", "type" },
                    new String[] { useEntity.getName(), "QUICK" });
            quickFilter.put("values", JSONUtils.toJSONObject("1", quickName));

            useFilter = quickFilter;
        }

        JSONObject queryEntry = new JSONObject();
        queryEntry.put("entity", useEntity.getName());
        queryEntry.put("fields", fields);
        queryEntry.put("pageNo", pageNo);
        queryEntry.put("pageSize", pageSize);
        queryEntry.put("filter", useFilter);
        queryEntry.put("sort", sortBy);
        queryEntry.put("reload", "true");

        DataListControl control = new ApiDataListControl(queryEntry, context.getBindUser());
        JSONObject ret = (JSONObject) control.getJSONResult();
        return formatSuccess(ret);
    }
}
