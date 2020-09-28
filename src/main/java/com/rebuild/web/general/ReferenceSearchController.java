/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.general.RecentlyUsedHelper;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.FieldValueWrapper;
import com.rebuild.core.support.general.ProtocolFilterParser;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * 引用字段搜索
 *
 * @author zhaofang123@gmail.com
 * @see RecentlyUsedSearchController
 * @since 08/24/2018
 */
@Controller
@RequestMapping("/commons/search/")
public class ReferenceSearchController extends EntityController {

    // 快速搜索引用字段
    @GetMapping({"reference", "quick"})
    public void referenceSearch(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final String entity = getParameterNotNull(request, "entity");
        final String field = getParameterNotNull(request, "field");

        Entity metaEntity = MetadataHelper.getEntity(entity);
        Field referenceField = metaEntity.getField(field);
        if (referenceField.getType() != FieldType.REFERENCE) {
            writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            return;
        }

        // 查询引用字段的实体
        Entity searchEntity = referenceField.getReferenceEntity();

        // 引用字段数据过滤仅在搜索时有效
        // 启用数据过滤后最近搜索将不可用
        String protocolFilter = new ProtocolFilterParser(null).parseRef(field + "." + entity);

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            ID[] recently = null;
            if (protocolFilter == null) {
                String type = getParameter(request, "type");
                recently = RecentlyUsedHelper.gets(user, searchEntity.getName(), type);
            }

            if (recently == null || recently.length == 0) {
                writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            } else {
                writeSuccess(response, RecentlyUsedSearchController.formatSelect2(recently, null));
            }
            return;
        }

        // 查询字段
        Set<String> searchFields = ParseHelper.buildQuickFields(searchEntity, getParameter(request, "quickFields"));
        if (searchFields.isEmpty()) {
            LOG.warn("No fields of search found : " + searchEntity);
            writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            return;
        }

        q = StringEscapeUtils.escapeSql(q);
        String like = " like '%" + q + "%'";
        String searchWhere = StringUtils.join(searchFields.iterator(), like + " or ") + like;
        if (protocolFilter != null) {
            searchWhere = "(" + searchWhere + ") and (" + protocolFilter + ')';
        }

        List<Object> result = resultSearch(searchWhere, searchEntity, true);
        writeSuccess(response, result);
    }

    // 搜索指定实体的指定字段
    @GetMapping("search")
    public void search(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final String entity = getParameterNotNull(request, "entity");

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = getParameter(request, "type");
            ID[] recently = RecentlyUsedHelper.gets(user, entity, type);
            if (recently.length == 0) {
                writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            } else {
                writeSuccess(response, RecentlyUsedSearchController.formatSelect2(recently, null));
            }
            return;
        }

        final Entity searchEntity = MetadataHelper.getEntity(entity);

        // 查询字段
        Set<String> searchFields = ParseHelper.buildQuickFields(searchEntity, getParameter(request, "quickFields"));
        if (searchFields.isEmpty()) {
            LOG.warn("No fields of search found : " + searchEntity);
            writeSuccess(response, ArrayUtils.EMPTY_STRING_ARRAY);
            return;
        }

        q = StringEscapeUtils.escapeSql(q);
        String like = " like '%" + q + "%'";
        String searchWhere = StringUtils.join(searchFields.iterator(), like + " or ") + like;

        List<Object> result = resultSearch(searchWhere, searchEntity, true);
        writeSuccess(response, result);
    }

    // 搜索分类字段
    @GetMapping("classification")
    public void searchClassification(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final String entity = getParameterNotNull(request, "entity");
        final String field = getParameterNotNull(request, "field");

        Field fieldMeta = MetadataHelper.getField(entity, field);
        ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, false);

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = "d" + useClassification;
            ID[] recently = RecentlyUsedHelper.gets(user, "ClassificationData", type);
            if (recently.length == 0) {
                writeSuccess(response, JSONUtils.EMPTY_ARRAY);
            } else {
                writeSuccess(response, RecentlyUsedSearchController.formatSelect2(recently, null));
            }
            return;
        }
        q = StringEscapeUtils.escapeSql(q);

        int openLevel = ClassificationManager.instance.getOpenLevel(fieldMeta);
        String sqlWhere = String.format(
                "dataId = '%s' and level = %d and (fullName like '%%%s%%' or quickCode like '%%%s%%') order by fullName",
                useClassification.toLiteral(), openLevel, q, q);

        List<Object> result = resultSearch(sqlWhere, MetadataHelper.getEntity(EntityHelper.ClassificationData), false);
        writeSuccess(response, result);
    }

    /**
     * 封装查询结果
     *
     * @param sqlWhere
     * @param entity
     * @return
     */
    private List<Object> resultSearch(String sqlWhere, Entity entity, boolean usePrivileges) {
        Field nameField = MetadataHelper.getNameField(entity);

        String sql = MessageFormat.format("select {0},{1} from {2} where ({3})",
                entity.getPrimaryField().getName(), nameField.getName(), entity.getName(), sqlWhere);

        DisplayType dt = EasyMeta.getDisplayType(nameField);
        if (dt != DisplayType.ID) {
            sql += " order by " + nameField.getName();
        } else if (entity.containsField(EntityHelper.ModifiedOn)) {
            sql += " order by modifiedOn desc";
        }

        Object[][] array = (usePrivileges ? Application.createQueryNoFilter(sql) : Application.createQuery(sql))
                .setLimit(10)
                .array();

        List<Object> result = new ArrayList<>();
        for (Object[] o : array) {
            ID recordId = (ID) o[0];
            if (MetadataHelper.isBizzEntity(entity.getEntityCode())
                    && (!UserHelper.isActive(recordId) || recordId.equals(UserService.SYSTEM_USER))) {
                continue;
            }

            String label = (String) FieldValueWrapper.instance.wrapFieldValue(o[1], nameField, true);
            if (StringUtils.isBlank(label)) {
                label = FieldValueWrapper.NO_LABEL_PREFIX + recordId.toLiteral().toUpperCase();
            }
            result.add(FieldValueWrapper.wrapMixValue(recordId, label));
        }
        return result;
    }

    // 获取记录的名称字段值
    @GetMapping("read-labels")
    public void referenceLabel(HttpServletRequest request, HttpServletResponse response) {
        String ids = getParameter(request, "ids", null);
        if (ids == null) {
            writeSuccess(response);
            return;
        }

        Map<String, String> labels = new HashMap<>();
        for (String id : ids.split("\\|")) {
            if (!ID.isId(id)) {
                continue;
            }
            String label = FieldValueWrapper.getLabelNotry(ID.valueOf(id));
            labels.put(id, label);
        }
        writeSuccess(response, labels);
    }

    /**
     * @see com.rebuild.web.general.GeneralListController#pageList(String, HttpServletRequest, HttpServletResponse)
     */
    @GetMapping("reference-search")
    public ModelAndView referenceSearchPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] fieldAndEntity = getParameterNotNull(request, "field").split("\\.");
        if (!MetadataHelper.checkAndWarnField(fieldAndEntity[1], fieldAndEntity[0])) {
            response.sendError(404);
            return null;
        }

        Entity entity = MetadataHelper.getEntity(fieldAndEntity[1]);
        Field field = entity.getField(fieldAndEntity[0]);
        Entity searchEntity = field.getReferenceEntity();

        ModelAndView mv = createModelAndView("/general/reference-search");
        putEntityMeta(mv, searchEntity);

        JSON config = DataListManager.instance.getFieldsLayout(searchEntity.getName(), getRequestUser(request));
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));

        // 是否启用了字段过滤
        String referenceDataFilter = EasyMeta.valueOf(field).getExtraAttr("referenceDataFilter");
        if (referenceDataFilter != null && referenceDataFilter.length() > 10) {
            mv.getModel().put("referenceFilter", "ref:" + getParameter(request, "field"));
        } else {
            mv.getModel().put("referenceFilter", StringUtils.EMPTY);
        }
        return mv;
    }
}
