/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.exception.jdbc.GenericJdbcException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import com.rebuild.web.InvalidParameterException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.DataTruncation;
import java.util.*;

import static com.rebuild.core.support.i18n.Language.$L;

/**
 * 业务实体操作（增/改/删/分派/共享）
 *
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 * @see Application#getEntityService(int)
 * @see CommonOperatingController
 */
@Slf4j
@RestController
@RequestMapping("/app/entity/")
public class GeneralOperatingController extends BaseController {

    // 重复字段值
    public static final int CODE_REPEATED_VALUES = 499;

    @PostMapping("record-save")
    public JSONAware save(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final JSON formJson = ServletUtils.getRequestJson(request);

        Record record;
        try {
            record = EntityHelper.parse((JSONObject) formJson, user);
        } catch (DataSpecificationException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        // 兼容所有类型的实体
        if (!MetadataHelper.isBusinessEntity(record.getEntity())) {
            return CommonOperatingController.saveRecord(record);
        }

        final EntityService ies = Application.getEntityService(record.getEntity().getEntityCode());

        // 检查重复值
        List<Record> repeated = ies.getAndCheckRepeated(record, 100);
        if (!repeated.isEmpty()) {
            return new RespBody(CODE_REPEATED_VALUES, $L("存在重复记录"), buildRepeatedData(repeated));
        }

        try {
            record = ies.createOrUpdate(record);

        } catch (AccessDeniedException | DataSpecificationException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());

        } catch (GenericJdbcException ex) {
            if (ex.getCause() instanceof DataTruncation) {
                return RespBody.errorl("字段长度超过限制");
            }

            log.error(null, ex);
            return RespBody.error(ex.getLocalizedMessage());
        }

        JSONObject ret = new JSONObject();
        ret.put("id", record.getPrimary());

        // 单字段修改立即返回新值
        boolean viaSingle = getBoolParameter(request, "single");
        if (viaSingle) {
            for (String field : record.getAvailableFields()) {
                Field fieldMeta = record.getEntity().getField(field);
                if (MetadataHelper.isCommonsField(field) || fieldMeta.getType() == FieldType.PRIMARY) {
                    continue;
                }

                Object newValue = FormsBuilder.instance.wrapFieldValue(record, EasyMetaFactory.valueOf(fieldMeta));
                ret.put(field, newValue);
            }
        }

        return ret;
    }

    @PostMapping("record-delete")
    public JSONAware delete(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            return RespBody.errorl("没有要删除的记录");
        }

        final ID firstId = records[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());

        // 兼容所有类型的实体
        if (!MetadataHelper.isBusinessEntity(entity)) {
            return CommonOperatingController.deleteRecord(firstId);
        }

        final EntityService ies = Application.getEntityService(entity.getEntityCode());

        String[] cascades = parseCascades(request);

        int affected;
        try {
            if (records.length == 1) {
                affected = ies.delete(firstId, cascades);
            } else {
                BulkContext context = new BulkContext(user, BizzPermission.DELETE, null, cascades, records);
                affected = ies.bulk(context);
            }

        } catch (AccessDeniedException | DataSpecificationException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        return JSONUtils.toJSONObject(
                new String[] { "deleted", "requests" },
                new Object[] { affected, records.length });
    }

    @PostMapping("record-assign")
    public JSONAware assign(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            return RespBody.errorl("没有要分派的记录");
        }

        final ID firstId = records[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
        final EntityService ies = Application.getEntityService(entity.getEntityCode());

        String[] cascades = parseCascades(request);
        ID assignTo = getIdParameterNotNull(request, "to");

        int affected;
        try {
            // 仅一条记录
            if (records.length == 1) {
                affected = ies.assign(firstId, assignTo, cascades);
            } else {
                BulkContext context = new BulkContext(user, BizzPermission.ASSIGN, assignTo, cascades, records);
                affected = ies.bulk(context);
            }

        } catch (AccessDeniedException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        return JSONUtils.toJSONObject(
                new String[] { "assigned", "requests" },
                new Object[] { affected, records.length });
    }

    @PostMapping("record-share")
    public JSONAware share(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            return RespBody.errorl("没有要共享的记录");
        }

        final ID[] toUsers = parseUserList(request);
        if (toUsers.length == 0) {
            return RespBody.errorl("没有要共享的用户");
        }

        final ID firstId = records[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
        final EntityService ies = Application.getEntityService(entity.getEntityCode());

        String[] cascades = parseCascades(request);

        int shareRights = BizzPermission.READ.getMask();
        if (getBoolParameter(request, "withUpdate")) {
            shareRights += BizzPermission.UPDATE.getMask();
        }

        int affected = 0;
        try {
            for (ID to : toUsers) {
                // 一条记录
                if (records.length == 1) {
                    affected += ies.share(firstId, to, cascades, shareRights);
                } else {
                    BulkContext context = new BulkContext(user, BizzPermission.SHARE, to, cascades, records);
                    context.addExtraParam("shareRights", shareRights);
                    affected += ies.bulk(context);
                }
            }

        } catch (AccessDeniedException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        return JSONUtils.toJSONObject(
                new String[] { "shared", "requests" },
                new Object[] { affected, records.length });
    }

    @PostMapping("record-unshare")
    public JSONAware unsharesa(@IdParam(name = "record") ID recordId, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final ID[] accessIds = parseIdList(request);  // ShareAccess IDs
        if (accessIds.length == 0) {
            return RespBody.errorl("没有要取消共享的记录");
        }

        final ID firstId = accessIds[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
        final EntityService ies = Application.getEntityService(entity.getEntityCode());

        int affected;
        try {
            if (accessIds.length == 1) {
                affected = ies.unshare(recordId, accessIds[0]);
            } else {
                BulkContext context = new BulkContext(user, EntityService.UNSHARE, accessIds, recordId);
                affected = ies.bulk(context);
            }

        } catch (AccessDeniedException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        return JSONUtils.toJSONObject(
                new String[] { "unshared", "requests" },
                new Object[] { affected, accessIds.length });
    }

    @PostMapping("record-unshare-batch")
    public JSONAware unshareBatch(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            return RespBody.errorl("没有要取消共享的记录");
        }

        // 查询共享记录ID
        String accessSql = String.format(
                "select recordId,accessId from ShareAccess where recordId in ('%s')",
                StringUtils.join(records, "','"));

        String to = getParameterNotNull(request, "to");
        // 非全部
        if (!"$ALL$".equals(to)) {
            ID[] toUsers = parseUserList(request);
            if (toUsers.length == 0) {
                return RespBody.errorl("没有要取消共享的用户");
            }

            accessSql += String.format(" and shareTo in ('%s')", StringUtils.join(toUsers, "','"));
        }

        Object[][] accessArray = Application.createQueryNoFilter(accessSql).array();
        if (accessArray.length == 0) {
            return JSONUtils.toJSONObject(
                    new String[] { "unshared", "requests" },
                    new Object[] { 0, 0 });
        }

        Map<ID, Set<ID>> accessListMap = new HashMap<>();
        for (Object[] o : accessArray) {
            ID record = (ID) o[0];
            Set<ID> access = accessListMap.computeIfAbsent(record, k -> new HashSet<>());
            access.add((ID) o[1]);
        }

        final EntityService ies = Application.getEntityService(records[0].getEntityCode());

        int affected = 0;
        try {
            for (Map.Entry<ID, Set<ID>> e : accessListMap.entrySet()) {
                ID record = e.getKey();
                Set<ID> accessList = e.getValue();
                BulkContext context = new BulkContext(
                        user, EntityService.UNSHARE, accessList.toArray(new ID[0]), record);
                // 每条记录一个事物
                affected += ies.bulk(context);
            }

        } catch (AccessDeniedException known) {
            log.warn(">>>>> {}", known.getLocalizedMessage());
            return RespBody.error(known.getLocalizedMessage());
        }

        return JSONUtils.toJSONObject(
                new String[] { "unshared", "requests" },
                new Object[] { affected, records.length });
    }

    @GetMapping("shared-list")
    public Object[][] fetchSharedList(@IdParam ID recordId) {
        final Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());

        Object[][] array = Application.createQueryNoFilter(
                "select shareTo,accessId,createdOn,createdBy,rights from ShareAccess where belongEntity = ? and recordId = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, recordId)
                .array();

        for (Object[] o : array) {
            o[0] = new String[] { o[0].toString(), UserHelper.getName((ID) o[0]) };
            o[2] = I18nUtils.formatDate((Date) o[2]);
            o[3] = UserHelper.getName((ID) o[3]);
        }
        return array;
    }

    /**
     * 操作 ID 列表
     *
     * @param request
     * @return
     */
    private ID[] parseIdList(HttpServletRequest request) {
        String ids = getParameterNotNull(request, "id");

        Set<ID> idList = new HashSet<>();
        int sameEntityCode = 0;
        for (String id : ids.split(",")) {
            if (!ID.isId(id)) {
                continue;
            }
            ID id0 = ID.valueOf(id);
            if (sameEntityCode == 0) {
                sameEntityCode = id0.getEntityCode();
            }
            if (sameEntityCode != id0.getEntityCode()) {
                throw new InvalidParameterException($L("只能批量处理同一实体的记录"));
            }
            idList.add(ID.valueOf(id));
        }
        return idList.toArray(new ID[0]);
    }

    /**
     * 用户列表
     *
     * @param request
     * @return
     */
    private ID[] parseUserList(HttpServletRequest request) {
        String to = getParameterNotNull(request, "to");

        Set<ID> users = UserHelper.parseUsers(Arrays.asList(to.split(",")), null, true);
        return users.toArray(new ID[0]);
    }

    /**
     * 级联操作实体
     *
     * @param request
     * @return
     */
    private String[] parseCascades(HttpServletRequest request) {
        String cascades = getParameter(request, "cascades");
        if (StringUtils.isBlank(cascades)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        List<String> casList = new ArrayList<>();
        for (String c : cascades.split(",")) {
            if (MetadataHelper.containsEntity(c)) {
                casList.add(c);
            } else {
                log.warn("Unknown entity in cascades : " + c);
            }
        }
        return casList.toArray(new String[0]);
    }

    /**
     * 转成二维数组（首行为字段名，首列为ID）
     *
     * @param records
     * @return
     */
    private JSON buildRepeatedData(List<Record> records) {
        final Entity entity = records.get(0).getEntity();

        // 准备字段
        List<String> fields = new ArrayList<>();
        fields.add(entity.getPrimaryField().getName());
        for (Record r : records) {
            for (Iterator<String> iter = r.getAvailableFieldIterator(); iter.hasNext(); ) {
                String field = iter.next();
                if (!fields.contains(field)) {
                    fields.add(field);
                }
            }
        }

        JSONArray fieldsJson = new JSONArray();
        for (String field : fields) {
            fieldsJson.add(EasyMetaFactory.getLabel(entity.getField(field)));
        }

        JSONArray data = new JSONArray();
        data.add(fieldsJson);

        for (Record r : records) {
            JSONArray valuesJson = new JSONArray();
            for (String field : fields) {
                Object value = r.getObjectValue(field);
                value = FieldValueHelper.wrapFieldValue(value, entity.getField(field), true);
                valuesJson.add(value);
            }
            data.add(valuesJson);
        }
        return data;
    }
}
