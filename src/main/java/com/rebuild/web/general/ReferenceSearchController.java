/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.general.RecentlyUsedHelper;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.general.ProtocolFilterParser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.EntityParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
@Slf4j
@RestController
@RequestMapping("/commons/search/")
public class ReferenceSearchController extends EntityController {

    // 快速搜索引用字段
    @GetMapping({"reference", "quick"})
    public JSON referenceSearch(@EntityParam Entity entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String field = getParameterNotNull(request, "field");

        Field referenceField = entity.getField(field);
        if (!(referenceField.getType() == FieldType.REFERENCE || referenceField.getType() == FieldType.REFERENCE_LIST)) {
            return JSONUtils.EMPTY_ARRAY;
        }

        // 查询引用字段的实体
        Entity searchEntity = referenceField.getReferenceEntity();

        // 引用字段数据过滤
        // 启用数据过滤后最近搜索将不可用
        String protocolFilter = new ProtocolFilterParser(null)
                .parseRef(field + "." + entity.getName(), request.getParameter("cascadingValue"));

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        // 强制返回结果 H5
        if (StringUtils.isBlank(q) && !getBoolParameter(request, "forceResults")) {
            if (protocolFilter != null) return JSONUtils.EMPTY_ARRAY;

            String type = getParameter(request, "type");
            ID[] recently = RecentlyUsedHelper.gets(user, searchEntity.getName(), type);

            if (recently == null || recently.length == 0) {
                return JSONUtils.EMPTY_ARRAY;
            } else {
                return RecentlyUsedSearchController.formatSelect2(recently, Language.L("最近使用"));
            }
        }

        int pageSize = getIntParameter(request, "pageSize", 10);
        return buildResultSearch(
                searchEntity, getParameter(request, "quickFields"), q, protocolFilter, pageSize);
    }

    // 搜索指定实体的指定字段
    @GetMapping("search")
    public JSON search(@EntityParam Entity searchEntity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = getParameter(request, "type");
            ID[] recently = RecentlyUsedHelper.gets(user, searchEntity.getName(), type);

            if (recently.length == 0) {
                return JSONUtils.EMPTY_ARRAY;
            } else {
                return RecentlyUsedSearchController.formatSelect2(recently, null);
            }
        }

        int pageSize = getIntParameter(request, "pageSize", 10);
        return buildResultSearch(
                searchEntity, getParameter(request, "quickFields"), q, null, pageSize);
    }

    private JSON buildResultSearch(Entity searchEntity, String quickFields, String q, String appendWhere, int maxResults) {
        String searchWhere = "(1=1)";

        if (StringUtils.isNotBlank(q)) {
            // 查询字段
            Set<String> searchFields = ParseHelper.buildQuickFields(searchEntity, quickFields);
            if (searchFields.isEmpty()) {
                return JSONUtils.EMPTY_ARRAY;
            }

            String like = " like '%" + StringEscapeUtils.escapeSql(q) + "%'";
            searchWhere = StringUtils.join(searchFields.iterator(), like + " or ") + like;
        }

        if (appendWhere != null) {
            searchWhere = String.format("(%s) and (%s)", appendWhere, searchWhere);
        }

        List<Object> result = resultSearch(searchWhere, searchEntity, maxResults);
        return (JSON) JSON.toJSON(result);
    }

    // 搜索分类字段
    @GetMapping("classification")
    public JSON searchClassification(@EntityParam Entity entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String field = getParameterNotNull(request, "field");

        Field fieldMeta = entity.getField(field);
        ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, false);
        if (useClassification == null) {
            return JSONUtils.EMPTY_ARRAY;
        }

        String q = getParameter(request, "q");
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = "d" + useClassification + ":" + ClassificationManager.instance.getOpenLevel(fieldMeta);
            ID[] recently = RecentlyUsedHelper.gets(user, "ClassificationData", type);
            if (recently.length == 0) {
                return JSONUtils.EMPTY_ARRAY;
            } else {
                return RecentlyUsedSearchController.formatSelect2(recently, null);
            }
        }

        q = StringEscapeUtils.escapeSql(q);

        int openLevel = ClassificationManager.instance.getOpenLevel(fieldMeta);
        String sqlWhere = String.format(
                "dataId = '%s' and level = %d and (fullName like '%%%s%%' or quickCode like '%%%s%%') order by fullName",
                useClassification.toLiteral(), openLevel, q, q);

        List<Object> result = resultSearch(
                sqlWhere, MetadataHelper.getEntity(EntityHelper.ClassificationData), 10);
        return (JSON) JSON.toJSON(result);
    }

    private List<Object> resultSearch(String sqlWhere, Entity entity, int maxResults) {
        Field nameField = entity.getNameField();

        String sql = MessageFormat.format("select {0},{1} from {2} where {3}",
                entity.getPrimaryField().getName(), nameField.getName(), entity.getName(), sqlWhere);

        if (!sqlWhere.contains(" order by ")) {
            DisplayType dt = EasyMetaFactory.getDisplayType(nameField);
            if (dt != DisplayType.ID) {
                sql += " order by " + nameField.getName();
            } else if (entity.containsField(EntityHelper.ModifiedOn)) {
                sql += " order by modifiedOn desc";
            }
        }

        Query query = MetadataHelper.hasPrivilegesField(entity)
                ? Application.createQuery(sql) : Application.createQueryNoFilter(sql);
        Object[][] array = query.setLimit(maxResults).array();

        List<Object> result = new ArrayList<>();
        for (Object[] o : array) {
            ID recordId = (ID) o[0];
            if (MetadataHelper.isBizzEntity(entity)
                    && (!UserHelper.isActive(recordId) || recordId.equals(UserService.SYSTEM_USER))) {
                continue;
            }

            String label = (String) FieldValueHelper.wrapFieldValue(o[1], nameField, true);
            if (StringUtils.isBlank(label)) {
                label = FieldValueHelper.NO_LABEL_PREFIX + recordId.toLiteral().toUpperCase();
            }
            result.add(FieldValueHelper.wrapMixValue(recordId, label));
        }
        return result;
    }

    // 获取记录的名称字段值
    @GetMapping("read-labels")
    public RespBody referenceLabel(HttpServletRequest request) {
        final String ids = getParameter(request, "ids", null);
        if (StringUtils.isBlank(ids)) {
            return RespBody.ok();
        }

        final ID user = getRequestUser(request);

        // 不存在的记录不返回
        boolean ignoreMiss = getBoolParameter(request, "ignoreMiss", false);
        // 检查权限，无权限的不返回
        boolean checkPrivileges = getBoolParameter(request, "checkPrivileges", false);

        Map<String, String> labels = new HashMap<>();
        for (String id : ids.split("[|,]")) {
            if (!ID.isId(id)) continue;

            ID recordId = ID.valueOf(id);
            if (checkPrivileges && !Application.getPrivilegesManager().allowRead(user, recordId)) continue;

            if (ignoreMiss) {
                try {
                    labels.put(id, FieldValueHelper.getLabel(recordId));
                } catch (NoRecordFoundException ignored) {
                }

            } else {
                labels.put(id, FieldValueHelper.getLabelNotry(recordId));
            }
        }

        return RespBody.ok(labels);
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

        final ID user = getRequestUser(request);

        Entity entity = MetadataHelper.getEntity(fieldAndEntity[1]);
        Field field = entity.getField(fieldAndEntity[0]);
        Entity searchEntity = field.getReferenceEntity();

        ModelAndView mv = createModelAndView("/general/reference-search");
        putEntityMeta(mv, searchEntity);

        JSON config = DataListManager.instance.getFieldsLayout(searchEntity.getName(), user);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));

        // 可新建
        mv.getModel().put("canCreate",
                Application.getPrivilegesManager().allowCreate(user, searchEntity.getEntityCode()));

        if (ProtocolFilterParser.getFieldDataFilter(field) != null
                || ProtocolFilterParser.hasFieldCascadingField(field)) {
            String protocolExpr = String.format("ref:%s:%s",
                    getParameterNotNull(request, "field"),
                    StringUtils.defaultString(getParameter(request, "cascadingValue"), ""));
            mv.getModel().put("referenceFilter", protocolExpr);
        } else {
            mv.getModel().put("referenceFilter", StringUtils.EMPTY);
        }

        return mv;
    }
}
