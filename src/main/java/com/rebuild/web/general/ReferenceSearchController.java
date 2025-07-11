/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.ClassificationManager;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserFilters;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.general.RecentlyUsedHelper;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.general.ProtocolFilterParser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.EntityController;
import com.rebuild.web.EntityParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 引用字段搜索
 *
 * @author Zixin (RB)
 * @see RecentlyUsedSearchController
 * @since 08/24/2018
 */
@Slf4j
@RestController
@RequestMapping({"/commons/search/","/app/entity/"})
public class ReferenceSearchController extends EntityController {

    private static final String _SELF = "{@CURRENT}";

    // 引用字段-快速搜索
    @RequestMapping({"reference", "quick"})
    public JSON referenceSearch(@EntityParam Entity entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        Field referenceField = entity.getField(getParameterNotNull(request, "field"));
        Entity searchEntity = referenceField.getReferenceEntity();
        // v4.1
        if (referenceField.getType() == FieldType.ANY_REFERENCE) {
            String anyrefEntity = getParameter(request, "anyrefEntity");
            if (StringUtils.isBlank(anyrefEntity)) return JSONUtils.EMPTY_ARRAY;
            searchEntity = MetadataHelper.getEntity(anyrefEntity);
        }

        // 引用字段数据过滤:级联
        String cascadingValue = getParameter(request, "cascadingValue");
        // v4.1 附加过滤条件使用表单字段变量
        String varRecord = getParameter(request, "varRecord");
        ProtocolFilterParser fp = new ProtocolFilterParser();
        if (StringUtils.isNotBlank(varRecord)) {
            varRecord = CodecUtils.urlDecode(varRecord);
            if (JSONUtils.wellFormat(varRecord)) fp.setVarRecord(JSON.parseObject(varRecord));
        }
        String protocolFilter = fp.parseRef(referenceField.getName() + "." + entity.getName(), cascadingValue);

        String q = StringUtils.trim(getParameter(request, "q"));

        // 强制搜索 H5
        boolean forceResults = getBoolParameter(request, "forceResults");
        boolean forceSearchs = getBoolParameter(request, "forceSearchs");

        // 为空则加载最近使用的
        if (StringUtils.isBlank(q) && !forceSearchs) {
            ID[] used = RecentlyUsedHelper.gets(
                    user, searchEntity.getName(), getParameter(request, "type"), protocolFilter);

            if (used.length == 0) {
                if (!forceResults) return JSONUtils.EMPTY_ARRAY;
            } else {
                return RecentlyUsedSearchController.formatSelect2(used, Language.L("最近使用"));
            }
        }

        int pageSize = getIntParameter(request, "pageSize", 20);
        return buildResultSearch(
                searchEntity, getParameter(request, "quickFields"), q, protocolFilter, pageSize, user);
    }

    // 搜索指定实体的指定字段
    @GetMapping("search")
    public JSON commonSearch(@EntityParam Entity searchEntity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        // 强制搜索 H5
        boolean forceResults = getBoolParameter(request, "forceResults");

        String q = StringUtils.trim(getParameter(request, "q"));
        // 为空则加载最近使用的
        if (StringUtils.isBlank(q)) {
            String type = getParameter(request, "type");
            ID[] recently = RecentlyUsedHelper.gets(user, searchEntity.getName(), type);

            if (recently.length == 0) {
                if (forceResults);  // NOOP
                else return JSONUtils.EMPTY_ARRAY;
            } else {
                return RecentlyUsedSearchController.formatSelect2(recently, Language.L("最近使用"));
            }
        }

        int pageSize = getIntParameter(request, "pageSize", 20);
        return buildResultSearch(
                searchEntity, getParameter(request, "quickFields"), q, null, pageSize, user);
    }

    // 构建查询
    private JSON buildResultSearch(Entity searchEntity, String quickFields, String q, String appendWhere, int maxResults, ID user) {
        String searchWhere = "(1=1)";

        if (StringUtils.isNotBlank(q)) {
            // 查询字段
            Set<String> searchFields = ParseHelper.buildQuickFields(searchEntity, quickFields);
            if (searchFields.isEmpty()) {
                return JSONUtils.EMPTY_ARRAY;
            }

            q = CommonsUtils.escapeSql(q);
            String like = " like '%" + q + "%'";
            searchWhere = StringUtils.join(searchFields.iterator(), like + " or ") + like;
        }

        if (appendWhere != null) {
            searchWhere = String.format("(%s) and (%s)", appendWhere, searchWhere);
        } else {
            searchWhere = String.format("(%s)", searchWhere);
        }

        final int sEntityCode = searchEntity.getEntityCode();
        if (MetadataHelper.isBizzEntity(sEntityCode)) {
            String s = UserFilters.getBizzFilter(sEntityCode, user);
            if (s != null) searchWhere += " and " + s;
            s = UserFilters.getEnableBizzPartFilter(sEntityCode, user);
            if (s != null) searchWhere += " and " + s;
        }

        List<Object> result = resultSearch(searchWhere, searchEntity, maxResults);
        // v35 本人/本部门
        if ("self".equals(q)) {
            if (sEntityCode == EntityHelper.User || sEntityCode == EntityHelper.Department) {
                result.add(JSONUtils.toJSONObject(
                        new String[]{ "id", "text" }, new Object[] { _SELF, Language.L("本人/本部门") }));
            }
        }

        return (JSON) JSON.toJSON(result);
    }

    /**
     * 搜索分类字段
     * @see PickListDataController#fetchClassification(HttpServletRequest)
     */
    @GetMapping("classification")
    public JSON searchClassification(@EntityParam Entity entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final String field = getParameterNotNull(request, "field");

        Field fieldMeta = entity.getField(field);
        ID useClassification = ClassificationManager.instance.getUseClassification(fieldMeta, false);
        if (useClassification == null) return JSONUtils.EMPTY_ARRAY;

        String q = StringUtils.trim(getParameter(request, "q"));
        int openLevel = ClassificationManager.instance.getOpenLevel(fieldMeta);
        // 直接显示
        boolean useSimple37 = StringUtils.isBlank(q) && openLevel == 0;

        // 为空则加载最近使用的
        if (StringUtils.isBlank(q) && !useSimple37) {
            String type = "d" + useClassification + ":" + ClassificationManager.instance.getOpenLevel(fieldMeta);
            ID[] used = RecentlyUsedHelper.gets(user, "ClassificationData", type);

            if (used.length == 0) {
                return JSONUtils.EMPTY_ARRAY;
            } else {
                return RecentlyUsedSearchController.formatSelect2(used, Language.L("最近使用"));
            }
        }

        String sqlWhere;
        if (useSimple37) {
            sqlWhere = String.format("dataId = '%s' and parent is null order by code,fullName",
                    useClassification.toLiteral());
        } else {
            q = CommonsUtils.escapeSql(q);
            sqlWhere = String.format(
                    "dataId = '%s' and level = %d and (fullName like '%%%s%%' or quickCode like '%%%s%%' or code like '%s%%') order by code,fullName",
                    useClassification.toLiteral(), openLevel, q, q, q);
        }

        List<Object> result = resultSearch(
                sqlWhere, MetadataHelper.getEntity(EntityHelper.ClassificationData), useSimple37 ? 2000 : 20);
        return (JSON) JSON.toJSON(result);
    }

    // 查询结果
    private List<Object> resultSearch(String sqlWhere, Entity entity, int maxResults) {
        Field nameField = entity.getNameField();
        boolean classCode = entity.getEntityCode() == EntityHelper.ClassificationData;

        String sql = MessageFormat.format("select {0},{1} from {2} where {3}",
                entity.getPrimaryField().getName(), nameField.getName(), entity.getName(), sqlWhere);
        if (classCode) sql = sql.replace(" from ", ",code,quickCode from ");

        if (!sqlWhere.contains(" order by ")) {
            DisplayType dt = EasyMetaFactory.getDisplayType(nameField);
            if (dt != DisplayType.ID) {
                sql += " order by " + nameField.getName();
            } else if (entity.containsField(EntityHelper.ModifiedOn)) {
                sql += " order by modifiedOn desc";
            }
        }

        if (Application.devMode()) log.info("[DEV] Reference search : {}", sql);
        Object[][] array = QueryHelper.createQuery(sql, entity).setLimit(maxResults).array();

        List<Object> res = new ArrayList<>();
        for (Object[] o : array) {
            ID id = (ID) o[0];
            String label = (String) FieldValueHelper.wrapFieldValue(o[1], nameField, true);
            if (StringUtils.isBlank(label)) {
                label = FieldValueHelper.NO_LABEL_PREFIX + id.toLiteral().toUpperCase();
            }

            JSONObject item = FieldValueHelper.wrapMixValue(id, label);
            if (classCode) item.put("code", StringUtils.defaultIfBlank((String) o[2], (String) o[3]));
            res.add(item);
        }
        return res;
    }

    // 获取记录的名称字段值
    @GetMapping("read-labels")
    public RespBody referenceLabel(HttpServletRequest request) {
        final String ids = getParameter(request, "ids", getParameter(request, "id", null));
        if (StringUtils.isBlank(ids)) return RespBody.ok();

        final ID user = getRequestUser(request);

        // 不存在的记录不返回
        boolean ignoreMiss = getBoolParameter(request, "ignoreMiss", false);
        // 检查权限，无权限的不返回
        boolean checkPrivileges = getBoolParameter(request, "checkPrivileges", false);

        Map<String, String> labels = new HashMap<>();
        for (String id : ids.split("[|,]")) {
            if (!ID.isId(id)) {
                if (_SELF.equals(id)) labels.put(_SELF, Language.L("本人/本部门"));
                continue;
            }

            final ID recordId = ID.valueOf(id);
            if (checkPrivileges && !Application.getPrivilegesManager().allowRead(user, recordId)) continue;

            if (ignoreMiss) {
                try {
                    labels.put(id, FieldValueHelper.getLabel(recordId));
                } catch (NoRecordFoundException ignored) {}
            } else {
                labels.put(id, FieldValueHelper.getLabelNotry(recordId));
            }
        }

        return RespBody.ok(labels);
    }

    /**
     * 引用字段搜索页面
     * @see GeneralListController#pageList(String, HttpServletRequest, HttpServletResponse)
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
        // v3.9 支持主键字段
        Entity searchEntity = field.getType() == FieldType.PRIMARY ? entity : field.getReferenceEntity();

        ModelAndView mv = createModelAndView("/general/reference-search");
        putEntityMeta(mv, searchEntity);

        JSON config = DataListManager.instance.getListFields(
                searchEntity.getName(), user, DataListManager.SYS_REFERENCE);
        mv.getModel().put("DataListConfig", JSON.toJSONString(config));

        // 可新建
        mv.getModel().put("canCreate",
                searchEntity.getMainEntity() == null
                        && Application.getPrivilegesManager().allowCreate(user, searchEntity.getEntityCode()));

        if (ProtocolFilterParser.getFieldDataFilter(field) != null
                || ProtocolFilterParser.hasFieldCascadingField(field)) {
            String protocolExpr = String.format("%s:%s:%s", ProtocolFilterParser.P_REF,
                    getParameterNotNull(request, "field"),
                    getParameter(request, "cascadingValue", StringUtils.EMPTY));
            mv.getModel().put("referenceFilter", protocolExpr);
        } else {
            mv.getModel().put("referenceFilter", StringUtils.EMPTY);
        }

        // 快速查询
        mv.getModel().put("quickFieldsLabel", GeneralListController.getQuickFieldsLabel(searchEntity));

        return mv;
    }

    @GetMapping("suggest")
    public JSON commonSuggest(HttpServletRequest request) {
        ID user = getRequestUser(request);
        String entityAndField = getParameterNotNull(request, "e");
        String[] ef = entityAndField.split("\\.");
        Field field = MetadataHelper.getField(ef[0], ef[1]);
        DisplayType dtOfField = EasyMetaFactory.getDisplayType(field);

        String q = StringUtils.trim(getParameter(request, "q"));
        int pageSize = getIntParameter(request, "pageSize", 10);

        if (dtOfField == DisplayType.REFERENCE || dtOfField == DisplayType.N2NREFERENCE) {
            String quickFields = field.getReferenceEntity().getNameField().getName();
            if (field.getReferenceEntity().containsField(EntityHelper.QuickCode)) quickFields += ",quickCode";

            return buildResultSearch(
                    field.getReferenceEntity(), quickFields, q, null, pageSize, user);
        }
        else if (dtOfField == DisplayType.CLASSIFICATION) {
            ID useClassification = ClassificationManager.instance.getUseClassification(field, false);
            if (useClassification == null) return JSONUtils.EMPTY_ARRAY;
            int openLevel = ClassificationManager.instance.getOpenLevel(field);

            q = CommonsUtils.escapeSql(q);
            String sqlWhere = String.format(
                    "dataId = '%s' and level = %d and (fullName like '%%%s%%' or quickCode like '%%%s%%') order by code,fullName",
                    useClassification.toLiteral(), openLevel, q, q);

            Object result = resultSearch(
                    sqlWhere, MetadataHelper.getEntity(EntityHelper.ClassificationData), pageSize);
            return (JSON) JSON.toJSON(result);
        }
        else if (dtOfField == DisplayType.TEXT) {
            String quickFields = field.getOwnEntity().getNameField().getName();
            if (field.getOwnEntity().containsField(EntityHelper.QuickCode)) quickFields += ",quickCode";

            return buildResultSearch(
                    field.getOwnEntity(), quickFields, q, null, pageSize, user);
        }

        return JSONUtils.EMPTY_ARRAY;
    }
}
