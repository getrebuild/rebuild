/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.StandardRecord;
import cn.devezhao.persist4j.record.FieldValueException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

/**
 * @author Zhao Fangfang
 * @see MetadataHelper
 * @since 1.0, 2013-6-26
 */
public class EntityHelper {

    /**
     * @param data
     * @param user
     * @return
     */
    public static Record parse(JSONObject data, ID user) {
        JSONObject metadata = data.getJSONObject(EntityRecordCreator.META_FIELD);
        if (metadata == null) {
            throw new FieldValueException(
                    com.rebuild.core.support.i18n.Language.L("InvalidRecordJson") + " : " + data.toJSONString());
        }

        String entityName = metadata.getString("entity");
        if (StringUtils.isBlank(entityName)) {
            String id = metadata.getString("id");
            if (!ID.isId(id)) {
                throw new FieldValueException(
                        com.rebuild.core.support.i18n.Language.L("InvalidRecordJson") + " : " + data.toJSONString());
            }
            entityName = MetadataHelper.getEntityName(ID.valueOf(id));
        }

        EntityRecordCreator creator = new EntityRecordCreator(MetadataHelper.getEntity(entityName), data, user);
        Record record = creator.create(false);
        EntityRecordCreator.bindCommonsFieldsValue(record, record.getPrimary() == null);
        return record;
    }

    /**
     * @param recordId
     * @param user
     * @return
     */
    public static Record forUpdate(ID recordId, ID user) {
        return forUpdate(recordId, user, true);
    }

    /**
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
            EntityRecordCreator.bindCommonsFieldsValue(record, false);
        }
        return record;
    }

    /**
     * @param entity
     * @param user
     * @return
     */
    public static Record forNew(int entity, ID user) {
        return forNew(entity, user, true);
    }

    /**
     * @param entity
     * @param user
     * @return
     */
    public static Record forNew(int entity, ID user, boolean bindCommons) {
        return forNew(MetadataHelper.getEntity(entity), user, bindCommons);
    }

    /**
     * @param entity
     * @param user
     * @return
     */
    private static Record forNew(Entity entity, ID user, boolean bindCommons) {
        Assert.notNull(entity, "[entity] cannot be null");
        Assert.notNull(user, "[user] cannot be null");

        Record record = new StandardRecord(entity, user);
        if (bindCommons) {
            EntityRecordCreator.bindCommonsFieldsValue(record, true);
        }
        return record;
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

    // 权限

    public static final int User = 1;
    public static final int Department = 2;
    public static final int Role = 3;
    public static final int RolePrivileges = 4;
    public static final int RoleMember = 5;
    public static final int Team = 6;
    public static final int TeamMember = 7;

    // 配置

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

    // 动态

    public static final int Feeds = 40;
    public static final int FeedsComment = 41;
    public static final int FeedsLike = 42;
    public static final int FeedsMention = 43;

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
}
