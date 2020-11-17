/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.api.general;

import cn.devezhao.persist4j.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.ApiContext;
import com.rebuild.api.ApiInvokeException;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.DataListBuilder;
import com.rebuild.core.support.general.DataListWrapper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 查询记录列表
 *
 * @author devezhao
 * @see com.rebuild.core.service.query.AdvFilterParser
 * @see DataListWrapper
 * @since 2020/5/21
 */
public class EntityList extends EntityGet {

    @Override
    protected String getApiName() {
        return "entity/list";
    }

    @Override
    public JSON execute(ApiContext context) throws ApiInvokeException {
        final String entity = context.getParameterNotBlank("entity");
        final Entity useEntity = MetadataHelper.getEntity(entity);
        if (!useEntity.isQueryable()) {
            throw new ApiInvokeException("Unsupportted operation for entity : " + entity);
        }

        if (!Application.getPrivilegesManager().allowRead(context.getBindUser(), useEntity.getEntityCode())) {
            return formatFailure("No permission to read records of " + EasyMetaFactory.getLabel(useEntity));
        }

        String[] fields = context.getParameterNotBlank("fields").split(",");
        fields = getValidFields(useEntity, fields);

        int pageNo = context.getParameterAsInt("page_no", 1);
        int pageSize = context.getParameterAsInt("page_size", 40);
        String sortBy = context.getParameter("sort_by");

        if (StringUtils.isBlank(sortBy)) {
            sortBy = EntityHelper.ModifiedOn + ":desc";

        } else if (!useEntity.containsField(sortBy.split(":")[0])) {
            return formatFailure("Invalid sort field : " + sortBy.split(":")[0]);
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

        DataListBuilder builder = new ApiDataListBuilder(queryEntry, context.getBindUser());
        JSONObject ret = (JSONObject) builder.getJSONResult();
        return formatSuccess(ret);
    }
}
