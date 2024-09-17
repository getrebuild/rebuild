/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.audit;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author devezhao
 * @since 11/01/2018
 */
@RestController
@RequestMapping("/admin/audit/")
public class RevisionHistoryController extends EntityController {

    @GetMapping("revision-history")
    public ModelAndView page() {
        return createModelAndView("/admin/audit/revision-history");
    }

    @GetMapping("revision-history/details")
    public JSON details(@IdParam ID revisionId) {
        Object[] rev = Application.createQueryNoFilter(
                "select revisionContent,belongEntity from RevisionHistory where revisionId = ?")
                .setParameter(1, revisionId)
                .unique();

        JSONArray contents = JSON.parseArray((String) rev[0]);
        if (MetadataHelper.containsEntity((String) rev[1])) {
            paddingFieldsName(contents, MetadataHelper.getEntity((String) rev[1]));
        }

        return contents;
    }

    @GetMapping("revision-history/details-list")
    public JSON detailsList(@IdParam ID revisionId) {
        Object[] o = Application.getQueryFactory().uniqueNoFilter(revisionId, "recordId,belongEntity");
        if (o == null) return JSONUtils.EMPTY_ARRAY;

        Object[][] array = Application.createQueryNoFilter(
                "select revisionContent,revisionType,revisionOn,revisionBy.fullName from RevisionHistory where recordId = ? order by autoId desc")
                .setParameter(1, o[0])
                .setLimit(500)
                .array();

        List<Object> list = new ArrayList<>();
        if (MetadataHelper.containsEntity((String) o[1])) {
            Entity entity = MetadataHelper.getEntity((String) o[1]);

            for (Object[] item : array) {
                JSONArray contents = JSON.parseArray((String) item[0]);
                paddingFieldsName(contents, entity);

                item[0] = contents;
                item[2] = I18nUtils.formatDate((Date) item[2]);
                list.add(item);
            }
        }
        return (JSON) JSON.toJSON(list);
    }

    // 补充字段名称
    private void paddingFieldsName(JSONArray contents, Entity entity) {
        final int entityCode = entity.getEntityCode();
        for (Iterator<Object> iter = contents.iterator(); iter.hasNext(); ) {
            JSONObject item = (JSONObject) iter.next();
            String fieldName = item.getString("field");

            if (entity.containsField(fieldName)) {
                EasyField easyField = EasyMetaFactory.valueOf(entity.getField(fieldName));
                // 排除不可查询字段
                if (!easyField.isQueryable()) {
                    if (fieldName.equalsIgnoreCase("contentMore") && entityCode == EntityHelper.Feeds) {
                        // 保留
                    } else {
                        iter.remove();
                        continue;
                    }
                }

                fieldName = easyField.getLabel();
            } else {
                if ("SHARETO".equalsIgnoreCase(fieldName)) {
                    fieldName = Language.L("共享用户");
                } else {
                    fieldName = "[" + fieldName.toUpperCase() + "]";
                }
            }
            item.put("field", fieldName);
        }
    }
}
