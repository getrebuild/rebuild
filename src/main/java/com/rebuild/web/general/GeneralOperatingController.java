/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.exception.jdbc.GenericJdbcException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.FormsBuilder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.ServiceSpec;
import com.rebuild.core.service.general.BulkContext;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.support.general.FieldValueWrapper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.InvalidParameterException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.DataTruncation;
import java.util.*;

/**
 * 记录操作（增/改/删/分派/共享）
 *
 * @author zhaofang123@gmail.com
 * @since 08/30/2018
 */
@Controller
@RequestMapping("/app/entity/")
public class GeneralOperatingController extends BaseController {

    // 重复字段值
    public static final int CODE_REPEATED_VALUES = 499;

    @PostMapping("record-save")
    public void save(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final JSON formJson = ServletUtils.getRequestJson(request);

        Record record;
        try {
            record = EntityHelper.parse((JSONObject) formJson, user);
        } catch (DataSpecificationException known) {
            writeFailure(response, known.getLocalizedMessage());
            return;
        }

        // 检查重复值
        List<Record> repeated = Application.getGeneralEntityService().getCheckRepeated(record, 100);
        if (!repeated.isEmpty()) {
            JSONObject map = new JSONObject();
            map.put("error_code", CODE_REPEATED_VALUES);
            map.put("error_msg", getLang(request, "RecordRepeated"));
            map.put("data", buildRepeatedData(repeated));
            writeJSON(response, map);
            return;
        }

        final EntityService ies = Application.getEntityService(record.getEntity().getEntityCode());

        try {
            record = ies.createOrUpdate(record);
        } catch (AccessDeniedException | DataSpecificationException known) {
            writeFailure(response, known.getLocalizedMessage());
            return;
        } catch (GenericJdbcException ex) {
            if (ex.getCause() instanceof DataTruncation) {
                writeFailure(response, getLang(request, "DataTruncation"));
            } else {
                LOG.error(null, ex);
                writeFailure(response, ex.getLocalizedMessage());
            }
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getPrimary());

        // 单字段修改立即返回新值
        boolean viaSingle = getBoolParameter(request, "single");
        if (viaSingle) {
            Map<String, Object> fieldsVal = new HashMap<>();
            for (String field : record.getAvailableFields()) {
                Field fieldMeta = record.getEntity().getField(field);
                if (MetadataHelper.isCommonsField(field) || fieldMeta.getType() == FieldType.PRIMARY) {
                    continue;
                }

                Object newValue = FormsBuilder.instance.wrapFieldValue(record, EasyMeta.valueOf(fieldMeta));
                fieldsVal.put(field, newValue);
            }
            map.putAll(fieldsVal);
        }

        writeSuccess(response, map);
    }

    @PostMapping("record-delete")
    public void delete(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            writeFailure(response, getLang(request, "NoSelect4Delete"));
            return;
        }

        final ID firstId = records[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
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
            writeFailure(response, known.getLocalizedMessage());
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "deleted", "requests" },
                new Object[] { affected, records.length });
        writeSuccess(response, ret);
    }

    @PostMapping("record-assign")
    public void assign(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            writeFailure(response, getLang(request, "NoSelect4Assign"));
            return;
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
            writeFailure(response, known.getLocalizedMessage());
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "assigned", "requests" },
                new Object[] { affected, records.length });
        writeSuccess(response, ret);
    }

    @PostMapping("record-share")
    public void share(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            writeFailure(response, getLang(request, "NoSelect4Share"));
            return;
        }

        final ID[] toUsers = parseUserList(request);
        if (toUsers.length == 0) {
            writeFailure(response, getLang(request, "NoSelect4ShareUser"));
            return;
        }

        final ID firstId = records[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
        final EntityService ies = Application.getEntityService(entity.getEntityCode());

        String[] cascades = parseCascades(request);

        int affected = 0;
        try {
            for (ID to : toUsers) {
                // 一条记录
                if (records.length == 1) {
                    affected += ies.share(firstId, to, cascades);
                } else {
                    BulkContext context = new BulkContext(user, BizzPermission.SHARE, to, cascades, records);
                    affected += ies.bulk(context);
                }
            }
        } catch (AccessDeniedException known) {
            writeFailure(response, known.getLocalizedMessage());
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "shared", "requests" },
                new Object[] { affected, records.length });
        writeSuccess(response, ret);
    }

    @PostMapping("record-unshare")
    public void unsharesa(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID record = getIdParameterNotNull(request, "record");  // Record ID
        final ID[] accessIds = parseIdList(request);  // ShareAccess IDs
        if (accessIds.length == 0) {
            writeFailure(response, getLang(request, "NoSelect4UnShare"));
            return;
        }

        final ID firstId = accessIds[0];
        final Entity entity = MetadataHelper.getEntity(firstId.getEntityCode());
        final EntityService ies = Application.getEntityService(entity.getEntityCode());

        int affected;
        try {
            if (accessIds.length == 1) {
                affected = ies.unshare(record, accessIds[0]);
            } else {
                BulkContext context = new BulkContext(user, EntityService.UNSHARE, accessIds, record);
                affected = ies.bulk(context);
            }
        } catch (AccessDeniedException known) {
            writeFailure(response, known.getLocalizedMessage());
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "unshared", "requests" },
                new Object[] { affected, accessIds.length });
        writeSuccess(response, ret);
    }

    @PostMapping("record-unshare-batch")
    public void unshareBatch(HttpServletRequest request, HttpServletResponse response) {
        final ID user = getRequestUser(request);
        final ID[] records = parseIdList(request);
        if (records.length == 0) {
            writeFailure(response, getLang(request, "NoSelect4UnShare"));
            return;
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
                writeFailure(response, getLang(request, "NoSelect4UnShareUser"));
                return;
            }
            accessSql += String.format(" and shareTo in ('%s')", StringUtils.join(toUsers, "','"));
        }

        Object[][] accessArray = Application.createQueryNoFilter(accessSql).array();
        if (accessArray.length == 0) {
            JSON ret = JSONUtils.toJSONObject(
                    new String[] { "unshared", "requests" },
                    new Object[] { 0, 0 });
            writeSuccess(response, ret);
            return;
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
            writeFailure(response, known.getLocalizedMessage());
            return;
        }

        JSON ret = JSONUtils.toJSONObject(
                new String[] { "unshared", "requests" },
                new Object[] { affected, records.length });
        writeSuccess(response, ret);
    }

    @GetMapping("shared-list")
    public void fetchSharedList(HttpServletRequest request, HttpServletResponse response) {
        final ID id = getIdParameterNotNull(request, "id");
        final Entity entity = MetadataHelper.getEntity(id.getEntityCode());

        Object[][] array = Application.createQueryNoFilter(
                "select shareTo,accessId,createdOn,createdBy from ShareAccess where belongEntity = ? and recordId = ?")
                .setParameter(1, entity.getName())
                .setParameter(2, id)
                .array();
        for (Object[] o : array) {
            o[0] = new String[]{o[0].toString(), UserHelper.getName((ID) o[0])};
            o[2] = CalendarUtils.getUTCDateTimeFormat().format(o[2]);
            o[3] = UserHelper.getName((ID) o[3]);
        }
        writeSuccess(response, array);
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
                throw new InvalidParameterException(Language.L("BatchOpMustSameEntity"));
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
                LOG.warn("Unknown entity in cascades : " + c);
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
            fieldsJson.add(EasyMeta.getLabel(entity.getField(field)));
        }

        JSONArray data = new JSONArray();
        data.add(fieldsJson);

        for (Record r : records) {
            JSONArray valuesJson = new JSONArray();
            for (String field : fields) {
                Object value = r.getObjectValue(field);
                value = FieldValueWrapper.instance.wrapFieldValue(value, entity.getField(field), true);
                valuesJson.add(value);
            }
            data.add(valuesJson);
        }
        return data;
    }
}
