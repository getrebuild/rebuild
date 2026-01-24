/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.ConfigurationException;
import com.rebuild.core.configuration.general.AdvFilterManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.approval.RobotApprovalConfigService;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 非业务实体操作（如系统实体）
 *
 * @author devezhao
 * @see Application#getService(int)
 * @see GeneralOperatingController
 * @since 2020/11/6
 */
@Slf4j
@RestController
@RequestMapping("/app/entity/")
public class CommonOperatingController extends BaseController {

    @PostMapping("common-save")
    public JSONAware save(HttpServletRequest request) {
        JSON formJson = (JSON) getRequestBody(request, true);
        Record record;
        try {
            record = EntityHelper.parse((JSONObject) formJson, getRequestUser(request));
        } catch (DataSpecificationException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        // 特殊处理
        if (getBoolParameter(request, "force")
                && record.getEntity().getEntityCode() == EntityHelper.RobotApprovalConfig) {
            RobotApprovalConfigService.setForceSave();
        }

        return saveRecord(record);
    }

    @RequestMapping("common-delete")
    public JSON delete(@IdParam ID recordId) {
        return deleteRecord(recordId);
    }

    @RequestMapping("common-get")
    public RespBody get(@IdParam ID recordId, HttpServletRequest request) {
        Entity getEntity = MetadataHelper.getEntity(recordId.getEntityCode());
        String fields = getParameter(request, "fields");
        if (StringUtils.isEmpty(fields)) fields = getAllFields(getEntity);

        // fix:CVE
        if (MetadataHelper.isBizzEntity(getEntity) && !UserHelper.isAdmin(getRequestUser(request))) {
            return RespBody.error("无权读取此记录或记录已被删除");
        }

        Record record = Application.getQueryFactory().record(recordId, fields.split("[,;]"));
        if (record == null) return RespBody.error("无权读取此记录或记录已被删除");
        return RespBody.ok(record);
    }

    @RequestMapping("common-find")
    public RespBody find(HttpServletRequest request) {
        String k = getParameterNotNull(request, "k");
        Object id = getParameterNotNull(request, "id");

        String[] ef = k.split("\\.");
        Entity findEntity = MetadataHelper.getEntity(ef[0]);
        // fix:CVE
        if (MetadataHelper.isBizzEntity(findEntity) && !UserHelper.isAdmin(getRequestUser(request))) {
            return RespBody.error("无权读取此记录或记录已被删除");
        }

        Field findField = findEntity.getField(ef[1]);
        String sql = String.format("select %s from %s where %s = ?",
                findEntity.getPrimaryField().getName(), findEntity.getName(), findField.getName());
        // 引用字段查名称
        if (findField.getType() == FieldType.REFERENCE) {
            if (ID.isId(id)) {
                id = ID.valueOf(id.toString());
            } else {
                Set<Field> queryFields = new HashSet<>();
                queryFields.add(findField.getReferenceEntity().getNameField());
                CollectionUtils.addAll(queryFields,
                        MetadataSorter.sortFields(findField.getReferenceEntity(), DisplayType.SERIES));
                id = QueryHelper.queryIdValue(queryFields.toArray(new Field[0]), id.toString(), false);
            }
        }

        Object[] found = id == null ? null
                : Application.createQuery(sql).setParameter(1, id).unique();

        if (found != null) return RespBody.ok(JSONUtils.toJSONObject("id", found[0]));
        return RespBody.ok(JSONUtils.toJSONObject("entity", findEntity.getName()));
    }

    @RequestMapping("common-list")
    public RespBody list(HttpServletRequest request) {
        JSONObject queryBody = (JSONObject) ServletUtils.getRequestJson(request);

        // FIXME SAFE!
        String entity = queryBody.getString("entity");
        String fields = queryBody.getString("fields");
        JSONObject filter = queryBody.getJSONObject("filter");
        String sort = queryBody.getString("sort");
        int limit = queryBody.getIntValue("limit");
        if (limit < 1) limit = 20;
        if (limit > 500) limit = 500;

        Entity listEntity = MetadataHelper.getEntity(entity);
        if (StringUtils.isBlank(fields)) fields = getAllFields(listEntity);

        String sql = String.format("select %s from %s",
                StringUtils.join(fields.split("[,;]"), ","), listEntity.getName());
        if (ParseHelper.validAdvFilter(filter)) {
            String filterWhere = new AdvFilterParser(filter, listEntity).toSqlWhere();
            if (filterWhere != null) sql += " where " + filterWhere;
        }
        if (StringUtils.isNotBlank(sort)) {
            sql += " order by " + sort.replace(":", " ");
        }

        List<Record> list = Application.createQuery(sql).setLimit(limit).list();
        return RespBody.ok(list);
    }

    // 获取全部字段
    private String getAllFields(Entity entity) {
        List<String> fs = new ArrayList<>();
        for (Field field : entity.getFields()) {
            if (MetadataHelper.isSystemField(field.getName())) continue;
            if (!EasyMetaFactory.valueOf(field).isQueryable()) continue;
            fs.add(field.getName());
        }
        return StringUtils.join(fs, ",");
    }

    @RequestMapping("check-readable")
    public RespBody checkReadable(@IdParam ID recordId, HttpServletRequest request) {
        boolean r = Application.getPrivilegesManager().allowRead(getRequestUser(request), recordId);
        return RespBody.ok(r);
    }

    /**
     * 保存记录
     *
     * @param record
     * @return
     */
    static JSONAware saveRecord(Record record) {
        try {
            record = Application.getService(record.getEntity().getEntityCode()).createOrUpdate(record);
            return JSONUtils.toJSONObject("id", record.getPrimary());
        } catch (DataSpecificationException | AccessDeniedException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }
    }

    /**
     * 删除记录
     *
     * @param recordId
     * @return
     */
    static JSON deleteRecord(ID recordId) {
        int del = Application.getService(recordId.getEntityCode()).delete(recordId);
        return JSONUtils.toJSONObject(new String[]{"deleted", "requests"}, new Object[]{del, del});
    }

    @GetMapping("filter-badge")
    public RespBody filterBadge(HttpServletRequest request) {
        ID filterId = getIdParameterNotNull(request, "filter");
        ConfigBean cb;
        try {
            cb = AdvFilterManager.instance.getAdvFilter(filterId);
        } catch (ConfigurationException miss) {
            log.warn("No [filter] found : {}", filterId);
            return RespBody.error(miss.getMessage());
        }

        String sqlWhere = new AdvFilterParser((JSONObject) cb.getJSON("filter")).toSqlWhere();
        String countSql = MessageFormat.format(
                "select count({0}Id) from {0} where {1}", cb.getString("entity"), sqlWhere);

        Object[] c = Application.getQueryFactory().uniqueNoFilter(countSql);
        return RespBody.ok(c == null ? 0 : c[0]);
    }
}
