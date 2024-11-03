/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.record.FieldValueException;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.general.AutoFillinManager;
import com.rebuild.core.privileges.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * @author Zhao Fangfang
 * @see MetadataHelper
 * @since 1.0, 2018-6-26
 */
@Slf4j
public class EntityHelper {

    // 虚拟 ID 后缀
    private static final String UNSAVED_ID_SUFFIX = "-0000000000000000";
    // 将新建的记录 ID
    public static final ID UNSAVED_ID = ID.valueOf("000" + UNSAVED_ID_SUFFIX);

    /**
     * 解析 JSON 为 Record
     *
     * @param data
     * @return
     * @see #parse(JSONObject, ID, boolean, boolean)
     */
    public static Record parse(JSONObject data) {
        ID user = (ID) ObjectUtils.defaultIfNull(UserContextHolder.getUser(true), UserService.SYSTEM_USER);
        log.info("Use '{}' do parse", user);
        return parse(data, user, true, false);
    }

    /**
     * 解析 JSON 为 Record
     *
     * @param data
     * @param user
     * @return
     * @see #parse(JSONObject, ID, boolean, boolean)
     */
    public static Record parse(JSONObject data, ID user) {
        return parse(data, user, true, false);
    }

    /**
     * 解析 JSON 为 Record
     *
     * @param data
     * @param user
     * @param safetyUrl 附件/图片字段值是否不允许外链（默认是）
     * @param forceFillin 是否强制表单回填（默认否）
     * @return
     * @see EntityRecordCreator
     */
    public static Record parse(JSONObject data, ID user, boolean safetyUrl, boolean forceFillin) {
        JSONObject metadata = data.getJSONObject(EntityRecordCreator.META_FIELD);
        if (metadata == null) {
            throw new FieldValueException(
                    com.rebuild.core.support.i18n.Language.L("无效实体数据格式 : %s", data.toJSONString()));
        }

        String entityName = metadata.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            String id = metadata.getString("id");
            if (!ID.isId(id)) {
                throw new FieldValueException(
                        com.rebuild.core.support.i18n.Language.L("无效实体数据格式 : %s", data.toJSONString()));
            }
            entityName = MetadataHelper.getEntityName(ID.valueOf(id));
        }

        if (metadata.getBooleanValue("delete")) {
            String id = metadata.getString("id");
            return new DeleteRecord(ID.valueOf(id), user);
        }

        Record record = new EntityRecordCreator(MetadataHelper.getEntity(entityName), data, user, safetyUrl)
                .create(false);

        // v3.4 表单后端回填
        if (MetadataHelper.isBusinessEntity(record.getEntity())) {
            AutoFillinManager.instance.fillinRecord(record, forceFillin);
        }

        return record;
    }

    /**
     * 构建更新 Record
     *
     * @param recordId
     * @return
     * @see #forUpdate(ID, ID, boolean)
     */
    public static Record forUpdate(ID recordId) {
        ID user = (ID) ObjectUtils.defaultIfNull(UserContextHolder.getUser(true), UserService.SYSTEM_USER);
        log.info("Use '{}' do forUpdate", user);
        return forUpdate(recordId, user, true);
    }

    /**
     * 构建更新 Record
     *
     * @param recordId
     * @param user
     * @return
     * @see #forUpdate(ID, ID, boolean)
     */
    public static Record forUpdate(ID recordId, ID user) {
        return forUpdate(recordId, user, true);
    }

    /**
     * 构建更新 Record
     *
     * @param recordId
     * @param user
     * @param bindCommons 是否自动补充公共字段
     * @return
     */
    public static Record forUpdate(ID recordId, ID user, boolean bindCommons) {
        Assert.notNull(recordId, "[recordId] cannot be null");
        Assert.notNull(user, "[user] cannot be null");

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        Record record = new StandardRecord(entity, user);
        record.setID(entity.getPrimaryField().getName(), recordId);
        if (bindCommons) {
            bindCommonsFieldsValue(record, false);
        }
        return record;
    }

    /**
     * 构建新建 Record
     *
     * @param entity
     * @return
     * @see #forNew(int, ID, boolean)
     */
    public static Record forNew(int entity) {
        ID user = (ID) ObjectUtils.defaultIfNull(UserContextHolder.getUser(true), UserService.SYSTEM_USER);
        log.info("Use '{}' do forNew", user);
        return forNew(entity, user, true);
    }

    /**
     * 构建新建 Record
     *
     * @param entity
     * @param user
     * @return
     * @see #forNew(int, ID, boolean)
     */
    public static Record forNew(int entity, ID user) {
        return forNew(entity, user, true);
    }

    /**
     * 构建新建 Record
     *
     * @param entity
     * @param user
     * @param bindCommons
     * @return
     */
    public static Record forNew(int entity, ID user, boolean bindCommons) {
        Assert.isTrue(MetadataHelper.containsEntity(entity), "[entity] does not exists : " + entity);
        Assert.notNull(user, "[user] cannot be null");

        Record record = new StandardRecord(MetadataHelper.getEntity(entity), user);
        if (bindCommons) {
            bindCommonsFieldsValue(record, true);
        }
        return record;
    }

    /**
     * 绑定公用/权限字段值
     *
     * @param r
     * @param isNew
     */
    public static void bindCommonsFieldsValue(Record r, boolean isNew) {
        final Date now = CalendarUtils.now();
        final Entity entity = r.getEntity();

        if (entity.containsField(EntityHelper.ModifiedOn)) {
            r.setDate(EntityHelper.ModifiedOn, now);
        }
        if (entity.containsField(EntityHelper.ModifiedBy)) {
            r.setID(EntityHelper.ModifiedBy, r.getEditor());
        }

        if (isNew) {
            if (entity.containsField(EntityHelper.CreatedOn)) {
                r.setDate(EntityHelper.CreatedOn, now);
            }
            if (entity.containsField(EntityHelper.CreatedBy)) {
                r.setID(EntityHelper.CreatedBy, r.getEditor());
            }
            if (entity.containsField(EntityHelper.OwningUser)) {
                r.setID(EntityHelper.OwningUser, r.getEditor());
            }
            if (entity.containsField(EntityHelper.OwningDept)) {
                com.rebuild.core.privileges.bizz.User user = r.getEditor() == null
                        ? null : Application.getUserStore().getUser(r.getEditor());
                if (user == null || user.getOwningDept() == null) {
                    throw new PrivilegesException("Bad member! No dept found in user : " + r.getEditor());
                }
                r.setID(EntityHelper.OwningDept, (ID) user.getOwningDept().getIdentity());
            }
        }
    }

    /**
     * 未保存记录 ID
     *
     * @param entityCode
     * @return
     * @see #isUnsavedId(Object)
     */
    public static ID newUnsavedId(int entityCode) {
        if (entityCode == 0) return UNSAVED_ID;
        return ID.valueOf(String.format("%03d", entityCode) + UNSAVED_ID_SUFFIX);
    }

    /**
     * @param id
     * @return
     * @see #newUnsavedId(int)
     */
    public static boolean isUnsavedId(Object id) {
        boolean s = ID.isId(id) && (UNSAVED_ID.equals(id) || id.toString().endsWith(UNSAVED_ID_SUFFIX));
        if (!s) return false;
        return !UserService.SYSTEM_USER.equals(id);
    }
    
    // 公共字段/保留字段

    public static final String CreatedOn = "createdOn";
    public static final String CreatedBy = "createdBy";
    public static final String ModifiedOn = "modifiedOn";
    public static final String ModifiedBy = "modifiedBy";
    public static final String OwningUser = "owningUser";
    public static final String OwningDept = "owningDept";

    public static final String AutoId = "autoId";
    public static final String QuickCode = "quickCode";
    public static final String IsDeleted = "isDeleted";

    public static final String ApprovalId = "approvalId";
    public static final String ApprovalState = "approvalState";
    public static final String ApprovalStepNode = "approvalStepNode";
    public static final String ApprovalStepNodeName = "approvalStepNodeName";
    public static final String ApprovalStepUsers = "approvalStepUsers";
    public static final String ApprovalLastUser = "approvalLastUser";
    public static final String ApprovalLastTime = "approvalLastTime";
    public static final String ApprovalLastRemark = "approvalLastRemark";

    // 用户

    public static final int User = 1;
    public static final int Department = 2;
    public static final int Role = 3;
    public static final int RolePrivileges = 4;
    public static final int RoleMember = 5;
    public static final int Team = 6;
    public static final int TeamMember = 7;
    public static final int ExternalUser = 8;

    // 系统

    public static final int MetaEntity = 10;
    public static final int MetaField = 11;
    public static final int PickList = 12;
    public static final int LayoutConfig = 13;
    public static final int FilterConfig = 14;
    public static final int DashboardConfig = 16;
    public static final int ChartConfig = 17;
    public static final int Classification = 18;
    public static final int ClassificationData = 19;
    public static final int ShareAccess = 20;
    public static final int SystemConfig = 21;
    public static final int Notification = 22;
    public static final int Attachment = 23;
    public static final int AttachmentFolder = 24;
    public static final int LoginLog = 25;
    public static final int AutoFillinConfig = 26;
    public static final int RobotTriggerConfig = 27;
    public static final int RobotApprovalConfig = 28;
    public static final int RobotApprovalStep = 29;
    public static final int RebuildApi = 30;
    public static final int RebuildApiRequest = 31;
    public static final int DataReportConfig = 32;
    public static final int RecycleBin = 33;
    public static final int RevisionHistory = 34;
    public static final int SmsendLog = 35;
    public static final int Language = 36;
    public static final int TransformConfig = 37;
    public static final int FrontjsCode = 38;
    public static final int NreferenceItem = 39;
    public static final int TagItem = 80;
    public static final int RobotSopConfig = 70;
    public static final int RobotSopStep = 71;

    // 动态

    public static final int Feeds = 40;
    public static final int FeedsComment = 41;
    public static final int FeedsLike = 42;
    public static final int FeedsMention = 43;
    public static final int FeedsStatus = 44;

    // 项目

    public static final int ProjectConfig = 50;
    public static final int ProjectPlanConfig = 51;
    public static final int ProjectTask = 52;
    public static final int ProjectTaskRelation = 53;
    public static final int ProjectTaskComment = 54;
    public static final int ProjectTaskTag = 55;
    public static final int ProjectTaskTagRelation = 56;

    // 外部表单

    public static final int ExtformConfig = 60;

    // 锁/日志/短链

    public static final int ShortUrl = 97;
    public static final int CommonsLock = 98;
    public static final int CommonsLog = 99;

}
