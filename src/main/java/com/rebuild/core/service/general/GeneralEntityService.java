/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.commons.ReflectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Filter;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.DeleteRecord;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.EasyEntityConfigProps;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.InternalPermission;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.SafeObserver;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import com.rebuild.core.service.general.series.SeriesGeneratorFactory;
import com.rebuild.core.service.notification.NotificationObserver;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.core.service.trigger.RobotTriggerManual;
import com.rebuild.core.service.trigger.RobotTriggerObserver;
import com.rebuild.core.service.trigger.TriggerAction;
import com.rebuild.core.service.trigger.TriggerWhen;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.rebuild.core.service.approval.ApprovalHelper.getSpecTriggers;

/**
 * 业务实体核心服务，所有业务实体都应该使用此类（或子类）
 * <br>- 有业务验证
 * <br>- 会带有系统设置规则的执行
 * <br>- 会开启一个事务，详见 `application-bean.xml` 配置
 *
 * <p>如有需要，其他实体可根据自身业务继承并复写</p>
 *
 * @author Zixin (RB)
 * @since 11/06/2019
 */
@Slf4j
@Service("rbGeneralEntityService")
public class GeneralEntityService extends ObservableService implements EntityService {

    // 有明细
    public static final String HAS_DETAILS = "$DETAILS$";

    protected GeneralEntityService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);

        addObserver(new NotificationObserver());
        addObserver(new RobotTriggerObserver());
        try {
            addObserver((SafeObserver) ReflectUtils.newObject("com.rebuild.rbv.sop.RobotSopObserver"));
        } catch (Exception ignoredClassNotFound){}
    }

    @Override
    public int getEntityCode() {
        return 0;
    }

    // 此方法具备明细实体批处理能力
    // 此方法具备重复检查能力
    @Override
    public Record createOrUpdate(Record record) {
        @SuppressWarnings("unchecked")
        final List<Record> details = (List<Record>) record.removeValue(HAS_DETAILS);

        final int rcm = GeneralEntityServiceContextHolder.getRepeatedCheckModeOnce();

        if (rcm == GeneralEntityServiceContextHolder.RCM_CHECK_MAIN
                || rcm == GeneralEntityServiceContextHolder.RCM_CHECK_ALL) {
            List<Record> repeated = getAndCheckRepeated(record, 20);
            if (!repeated.isEmpty()) {
                throw new RepeatedRecordsException(repeated);
            }
        }

        // 有明细
        final boolean hasDetails = details != null && !details.isEmpty();

        // 保证执行顺序
        Map<Integer, ID> detaileds = new TreeMap<>();

        try {
            if (hasDetails) {
                RobotTriggerObserver.setLazyTriggers();
                record = record.getPrimary() == null ? create(record) : update(record, true);
            } else {
                record = record.getPrimary() == null ? create(record) : update(record);
                return record;
            }

            // 明细记录处理

            final Entity detailEntity = record.getEntity().getDetailEntity();
            final String dtfField = MetadataHelper.getDetailToMainField(detailEntity).getName();
            final ID mainid = record.getPrimary();

            final boolean checkDetailsRepeated = rcm == GeneralEntityServiceContextHolder.RCM_CHECK_DETAILS
                    || rcm == GeneralEntityServiceContextHolder.RCM_CHECK_ALL;

            // 明细可能有自己的 Service
            EntityService des = Application.getEntityService(detailEntity.getEntityCode());
            if (des.getEntityCode() == 0) des = this;

            // 先删除
            for (int i = 0; i < details.size(); i++) {
                Record d = details.get(i);
                if (d instanceof DeleteRecord) {
                    des.delete(d.getPrimary());
                    detaileds.put(i, d.getPrimary());
                }
            }

            // 再保存
            for (int i = 0; i < details.size(); i++) {
                Record d = details.get(i);
                if (d instanceof DeleteRecord) continue;

                if (checkDetailsRepeated) {
                    d.setID(dtfField, mainid);  // for check

                    List<Record> repeated = des.getAndCheckRepeated(d, 20);
                    if (!repeated.isEmpty()) {
                        throw new RepeatedRecordsException(repeated);
                    }
                }

                if (d.getPrimary() == null) {
                    d.setID(dtfField, mainid);
                    des.create(d);
                } else {
                    des.update(d);
                }
                detaileds.put(i, d.getPrimary());
            }

            record.setObjectValue(HAS_DETAILS, detaileds.values());
            return record;

        } finally {
            RobotTriggerObserver.executeLazyTriggers(this);
        }
    }

    /**
     * 优先使用 `#createOrUpdate`。直接使用此方法请注意调用重复检查 `#getAndCheckRepeated`
     *
     * @param record
     * @return
     * @see #createOrUpdate(Record)
     * @see #getAndCheckRepeated(Record, int)
     */
    @Override
    public Record create(Record record) {
        appendDefaultValue(record);
        checkModifications(record, BizzPermission.CREATE);
        setSeriesValue(record);
        return super.create(record);
    }

    /**
     * 优先使用 `#createOrUpdate`。直接使用此方法请注意调用重复检查 `#getAndCheckRepeated`
     *
     * @param record
     * @return
     * @see #createOrUpdate(Record)
     * @see #getAndCheckRepeated(Record, int)
     */
    @Override
    public Record update(Record record) {
        return update(record, false);
    }

    /**
     * @param record
     * @param ignoreTriggers
     * @return
     */
    private Record update(Record record, boolean ignoreTriggers) {
        if (!checkModifications(record, BizzPermission.UPDATE)) {
            return record;
        }

        record = super.update(record);
        if (ignoreTriggers) return record;

        // ND 主记录修改时传导给明细（若有），以便触发聚合触发器
        // v3.7 只触发一个明细就够了

        for (Entity de : record.getEntity().getDetialEntities()) {
            TriggerAction[] deHasTriggersGG = getSpecTriggers(de, ActionType.GROUPAGGREGATION, TriggerWhen.UPDATE);
            TriggerAction[] deHasTriggersFG = getSpecTriggers(de, ActionType.FIELDAGGREGATION, TriggerWhen.UPDATE);
            if (deHasTriggersGG.length > 0 || deHasTriggersFG.length > 0) {
                RobotTriggerManual triggerManual = new RobotTriggerManual();
                ID opUser = UserService.SYSTEM_USER;

                for (ID did : QueryHelper.detailIdsNoFilter(record.getPrimary(), de)) {
                    Record dUpdate = EntityHelper.forUpdate(did, opUser, false);
                    triggerManual.onUpdate(
                            OperatingContext.create(opUser, BizzPermission.UPDATE, dUpdate, dUpdate));
                    break;
                }
            }
        }

        return record;
    }

    @Override
    public int delete(ID recordId) {
        return delete(recordId, null);
    }

    @Override
    public int delete(ID recordId, String[] cascades) {
        final ID currentUser = getCurrentUser();
        final RecycleStore recycleBin = useRecycleStore(recordId);

        int affected = this.deleteInternal(recordId);
        if (affected == 0) return 0;
        affected = 1;

        Map<String, Set<ID>> recordsOfCascaded = getCascadedRecords(recordId, cascades, BizzPermission.DELETE);
        for (Map.Entry<String, Set<ID>> e : recordsOfCascaded.entrySet()) {
            log.info("Cascading delete - {} > {} ", e.getKey(), e.getValue());

            for (ID id : e.getValue()) {
                if (Application.getPrivilegesManager().allowDelete(currentUser, id)) {
                    if (recycleBin != null) recycleBin.add(id, recordId);

                    int deleted = 0;
                    try {
                        deleted = this.deleteInternal(id);
                    } catch (DataSpecificationException ex) {
                        log.warn("Cannot delete {} because {}", id, ex.getLocalizedMessage());
                    } finally {
                        if (deleted > 0) {
                            affected++;
                        } else if (recycleBin != null) {
                            recycleBin.removeLast();  // If not delete
                        }
                    }
                } else {
                    log.warn("No have privileges to DELETE : {} > {}", currentUser, id);
                }
            }
        }

        if (recycleBin != null) recycleBin.store();

        return affected;
    }

    /**
     * @param recordId
     * @return
     * @throws DataSpecificationException
     */
    protected int deleteInternal(ID recordId) throws DataSpecificationException {
        Record delete = EntityHelper.forUpdate(recordId, getCurrentUser());
        if (!checkModifications(delete, BizzPermission.DELETE)) {
            return 0;
        }

        // ND 手动删除。传导给明细触发器、附件删除等
        for (Entity de : MetadataHelper.getEntity(recordId.getEntityCode()).getDetialEntities()) {
            for (ID did : QueryHelper.detailIdsNoFilter(recordId, de)) {
                // 明细无约束检查 checkModifications
                // 不使用明细实体 Service
                super.delete(did);
            }
        }

        return super.delete(recordId);
    }

    @Override
    public int assign(ID recordId, ID toUserId, String[] cascades) {
        final User toUser = Application.getUserStore().getUser(toUserId);
        final ID recordOrigin = recordId;
        // v3.2.2 若为明细则转为主记录
        if (MetadataHelper.getEntity(recordId.getEntityCode()).getMainEntity() != null) {
            recordId = QueryHelper.getMainIdByDetail(recordId);
        }

        final Record assignAfter = EntityHelper.forUpdate(recordId, (ID) toUser.getIdentity(), Boolean.FALSE);
        assignAfter.setID(EntityHelper.OwningUser, (ID) toUser.getIdentity());
        assignAfter.setID(EntityHelper.OwningDept, (ID) toUser.getOwningDept().getIdentity());

        // 分配前数据
        Record assignBefore = null;

        int affected;
        if (toUserId.equals(Application.getRecordOwningCache().getOwningUser(recordId))) {
            // No need to change
            log.debug("The record owner has not changed, ignore : {}", recordId);
            affected = 1;
        } else {
            assignBefore = countObservers() > 0 ? recordSnap(assignAfter, false) : null;

            delegateService.update(assignAfter);
            Application.getRecordOwningCache().cleanOwningUser(recordId);
            affected = 1;
        }

        Map<String, Set<ID>> cass = getCascadedRecords(recordOrigin, cascades, BizzPermission.ASSIGN);
        for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
            log.info("Cascading assign - {} > {}", e.getKey(), e.getValue());

            for (ID casid : e.getValue()) {
                affected += assign(casid, toUserId, null);
            }
        }

        if (countObservers() > 0 && assignBefore != null) {
            notifyObservers(OperatingContext.create(getCurrentUser(), BizzPermission.ASSIGN, assignBefore, assignAfter));
        }
        return affected;
    }

    @Override
    public int share(ID recordId, ID toUserId, String[] cascades, int rights) {
        final ID currentUser = getCurrentUser();
        final ID recordOrigin = recordId;
        // v3.2.2 若为明细则转为主记录
        if (MetadataHelper.getEntity(recordId.getEntityCode()).getMainEntity() != null) {
            recordId = QueryHelper.getMainIdByDetail(recordId);
        }

        boolean fromTriggerNoDowngrade = GeneralEntityServiceContextHolder.isFromTrigger(false);
        if (!fromTriggerNoDowngrade) {
            // 如用户无更新权限，则降级为只读共享
            if ((rights & BizzPermission.UPDATE.getMask()) != 0) {
                if (!Application.getPrivilegesManager().allowUpdate(toUserId, recordId.getEntityCode()) /* 目标用户无基础更新权限 */
                        || !Application.getPrivilegesManager().allow(currentUser, recordId, BizzPermission.UPDATE, true) /* 操作用户无记录更新权限 */) {
                    rights = BizzPermission.READ.getMask();
                    log.warn("Downgrade share rights to READ({}) : {}", BizzPermission.READ.getMask(), recordId);
                }
            }
        }

        final String entityName = MetadataHelper.getEntityName(recordId);
        final Record sharedAfter = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
        sharedAfter.setID("recordId", recordId);
        sharedAfter.setID("shareTo", toUserId);
        sharedAfter.setString("belongEntity", entityName);
        sharedAfter.setInt("rights", rights);

        Object[] hasShared = ((BaseService) delegateService).getPersistManagerFactory().createQuery(
                "select accessId,rights from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
                .setParameter(1, entityName)
                .setParameter(2, recordId)
                .setParameter(3, toUserId)
                .unique();

        int affected;
        boolean shareChange = false;
        if (hasShared != null) {
            if ((int) hasShared[1] != rights) {
                Record updateRights = EntityHelper.forUpdate((ID) hasShared[0], currentUser);
                updateRights.setInt("rights", rights);
                delegateService.update(updateRights);
                affected = 1;
                shareChange = true;
                sharedAfter.setID("accessId", (ID) hasShared[0]);

            } else {
                log.debug("The record has been shared and has the same rights, ignore : {}", recordId);
                affected = 1;
            }

        } else {
            // 可以共享给自己
            if (log.isDebugEnabled()
                    && toUserId.equals(Application.getRecordOwningCache().getOwningUser(recordId))) {
                log.debug("Share to the same user as the record, ignore : {}", recordId);
            }

            delegateService.create(sharedAfter);
            affected = 1;
            shareChange = true;
        }

        Map<String, Set<ID>> cass = getCascadedRecords(recordOrigin, cascades, BizzPermission.SHARE);
        for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
            log.info("Cascading share - {} > {}", e.getKey(), e.getValue());

            for (ID casid : e.getValue()) {
                affected += share(casid, toUserId, null, rights);
            }
        }

        if (countObservers() > 0 && shareChange) {
            notifyObservers(OperatingContext.create(currentUser, BizzPermission.SHARE, null, sharedAfter));
        }
        return affected;
    }

    @Override
    public int unshare(ID recordId, ID accessId) {
        final ID currentUser = getCurrentUser();

        Record unsharedBefore = null;
        if (countObservers() > 0) {
            unsharedBefore = EntityHelper.forUpdate(accessId, currentUser);
            unsharedBefore.setNull("belongEntity");
            unsharedBefore.setNull("recordId");
            unsharedBefore.setNull("shareTo");
            unsharedBefore = recordSnap(unsharedBefore, false);
        }

        delegateService.delete(accessId);

        if (countObservers() > 0) {
            notifyObservers(OperatingContext.create(currentUser, InternalPermission.UNSHARE, unsharedBefore, null));
        }
        return 1;
    }

    // FIXME Transaction rolled back because it has been marked as rollback-only
    // 20210722 删除时出错会报以上错误
    // 20220715 批量删除时有多个触发器，数据校验未通过可能会发生
    @Override
    public int bulk(BulkContext context) {
        BulkOperator operator = buildBulkOperator(context);
        try {
            return operator.exec();
        } catch (RebuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RebuildException(ex);
        }
    }

    @Override
    public String bulkAsync(BulkContext context) {
        BulkOperator operator = buildBulkOperator(context);
        return TaskExecutors.submit(operator, context.getOpUser());
    }

    /**
     * 获取级联操作记录
     *
     * @param mainRecordId 主记录
     * @param cascadeEntities 级联实体
     * @param action 动作
     * @return
     */
    protected Map<String, Set<ID>> getCascadedRecords(ID mainRecordId, String[] cascadeEntities, Permission action) {
        if (cascadeEntities == null || cascadeEntities.length == 0) {
            return Collections.emptyMap();
        }

        final boolean fromTriggerIgnorePrivileges = GeneralEntityServiceContextHolder.isFromTrigger(false);

        Map<String, Set<ID>> entityRecordsMap = new HashMap<>();
        Entity mainEntity = MetadataHelper.getEntity(mainRecordId.getEntityCode());

        for (String cas : cascadeEntities) {
            if (!MetadataHelper.containsEntity(cas)) {
                log.warn("The entity not longer exists : {}", cas);
                continue;
            }

            Entity casEntity = MetadataHelper.getEntity(cas);
            Field[] reftoFields = MetadataHelper.getReferenceToFields(mainEntity, casEntity, true);
            if (reftoFields.length == 0) {
                log.warn("No any fields of refto found : {} << {}", cas, mainEntity.getName());
                continue;
            }

            List<String> or = new ArrayList<>();
            // 有多个字段引用会一并获取
            for (Field field : reftoFields) {
                if (field.getType() == FieldType.REFERENCE) {
                    or.add(String.format("%s = '%s'", field.getName(), mainRecordId));
                } else {
                    // N2N
                    String exists = String.format(
                            "exists (select recordId from NreferenceItem where ^%s = recordId and belongField = '%s' and referenceId = '%s')",
                            field.getOwnEntity().getPrimaryField().getName(), field.getName(), mainRecordId);
                    or.add(exists);
                }
            }

            String sql = String.format("select %s from %s where ( %s )",
                    casEntity.getPrimaryField().getName(), casEntity.getName(),
                    StringUtils.join(or.iterator(), " or "));

            Object[][] array;
            if (fromTriggerIgnorePrivileges) {
                array = Application.createQueryNoFilter(sql).array();
            } else {
                Filter filter = Application.getPrivilegesManager().createQueryFilter(getCurrentUser(), action);
                array = Application.getQueryFactory().createQuery(sql, filter).array();
            }

            Set<ID> records = new HashSet<>();
            for (Object[] o : array) {
                records.add((ID) o[0]);
            }
            entityRecordsMap.put(cas, records);
        }
        return entityRecordsMap;
    }

    /**
     * 构造批处理操作
     *
     * @param context
     * @return
     */
    private BulkOperator buildBulkOperator(BulkContext context) {
        if (context.getAction() == BizzPermission.DELETE) {
            return new BulkDelete(context, this);
        } else if (context.getAction() == BizzPermission.ASSIGN) {
            return new BulkAssign(context, this);
        } else if (context.getAction() == BizzPermission.SHARE) {
            return new BulkShare(context, this);
        } else if (context.getAction() == InternalPermission.UNSHARE) {
            return new BulkUnshare(context, this);
        } else if (context.getAction() == BizzPermission.UPDATE) {
            return new BulkBatchUpdate(context, this);
        } else if (context.getAction() == InternalPermission.APPROVAL) {
            return (BulkOperator) ReflectUtils.newObject(
                    "com.rebuild.rbv.approval.BulkBatchApprove", context, this);
        }

        throw new UnsupportedOperationException("Unsupported bulk action : " + context.getAction());
    }

    /**
     * 系统相关约束检查。此方法有 3 种结果：
     * 1. true - 检查通过
     * 2. false - 检查不通过，但可以忽略的错误（如删除一条不存在的记录）
     * 3. 抛出异常 - 不可忽略的错误
     *
     * @param record
     * @param action [CREATE|UPDATE|DELDETE]
     * @return
     * @throws DataSpecificationException
     */
    protected boolean checkModifications(Record record, Permission action) throws DataSpecificationException {
        final Entity entity = record.getEntity();
        final Entity mainEntity = entity.getMainEntity();

        if (action == BizzPermission.CREATE) {
            // 验证审批状态
            // 仅验证新建明细（相当于更新主记录）
            if (mainEntity != null && MetadataHelper.hasApprovalField(mainEntity)) {
                Field dtmField = MetadataHelper.getDetailToMainField(entity);
                ID dtmFieldValue = record.getID(dtmField.getName());
                if (dtmFieldValue == null) {
                    throw new DataSpecificationException(Language.L("%s 不允许为空", EasyMetaFactory.getLabel(dtmField)));
                }

                ApprovalState state = ApprovalHelper.getApprovalState(dtmFieldValue);
                if (state == ApprovalState.APPROVED || state == ApprovalState.PROCESSING) {
                    throw new DataSpecificationException(state == ApprovalState.APPROVED
                            ? Language.L("主记录已完成审批，不能添加明细")
                            : Language.L("主记录正在审批中，不能添加明细"));
                }
            }

        } else {
            final Entity checkEntity = mainEntity != null ? mainEntity : entity;
            ID checkRecordId = record.getPrimary();

            if (checkEntity.containsField(EntityHelper.ApprovalId)) {
                // 需要验证主记录
                String recordType = Language.L("记录");
                if (mainEntity != null) {
                    checkRecordId = QueryHelper.getMainIdByDetail(checkRecordId);
                    recordType = Language.L("主记录");
                }

                ApprovalState currentState;
                ApprovalState changeState = null;
                try {
                    currentState = ApprovalHelper.getApprovalState(checkRecordId);
                    if (record.hasValue(EntityHelper.ApprovalState)) {
                        changeState = (ApprovalState) ApprovalState.valueOf(record.getInt(EntityHelper.ApprovalState));
                    }

                } catch (NoRecordFoundException ignored) {
                    log.warn("No record found for check ({}) : {}", action.getName(), checkRecordId);
                    return false;
                }

                boolean unallow = false;
                if (action == BizzPermission.DELETE) {
                    unallow = currentState == ApprovalState.APPROVED || currentState == ApprovalState.PROCESSING;
                } else if (action == BizzPermission.UPDATE) {
                    unallow = currentState == ApprovalState.APPROVED || currentState == ApprovalState.PROCESSING;

                    // 管理员撤销
                    if (unallow) {
                        boolean adminCancel = currentState == ApprovalState.APPROVED && changeState == ApprovalState.CANCELED;
                        if (adminCancel) unallow = false;
                    }

                    // 审批时/已通过强制修改
                    if (unallow) {
                        boolean forceUpdate = GeneralEntityServiceContextHolder.isAllowForceUpdateOnce();
                        if (forceUpdate) unallow = false;
                    }
                }

                if (unallow) {
                    if (RobotTriggerObserver.getTriggerSource() != null) {
                        recordType = Language.L("关联记录");
                    }

                    throw new DataSpecificationException(currentState == ApprovalState.APPROVED
                            ? Language.L("%s已完成审批，禁止操作", recordType)
                            : Language.L("%s正在审批中，禁止操作", recordType));
                }
            }
        }

        if (action == BizzPermission.CREATE || action == BizzPermission.UPDATE) {
            // TODO 父级级联字段强校验，兼容问题???
        }

        return true;
    }

    /**
     * 补充默认值
     *
     * @param recordOfNew
     */
    private void appendDefaultValue(Record recordOfNew) {
        Assert.isNull(recordOfNew.getPrimary(), "Must be new record");

        Entity entity = recordOfNew.getEntity();
        if (MetadataHelper.isBizzEntity(entity) || !MetadataHelper.hasPrivilegesField(entity)) {
            return;
        }

        for (Field field : entity.getFields()) {
            if (MetadataHelper.isCommonsField(field)
                    || recordOfNew.hasValue(field.getName(), true)) {
                continue;
            }

            Object defaultValue = EasyMetaFactory.valueOf(field).exprDefaultValue();
            if (defaultValue != null) {
                recordOfNew.setObjectValue(field.getName(), defaultValue);
            }
        }
    }

    /**
     * 自动编号
     *
     * @param record
     */
    private void setSeriesValue(Record record) {
        boolean skip = GeneralEntityServiceContextHolder.isSkipSeriesValue(false);
        Field[] seriesFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.SERIES);

        for (Field field : seriesFields) {
            // 不强制生成
            if (record.hasValue(field.getName()) && skip) {
                continue;
            }

            record.setString(field.getName(), SeriesGeneratorFactory.generate(field, record));
        }
    }

    @Override
    public List<Record> getAndCheckRepeated(Record checkRecord, int limit) {
        final Entity entity = checkRecord.getEntity();

        List<String> checkFields = new ArrayList<>();
        for (Iterator<String> iter = checkRecord.getAvailableFieldIterator(); iter.hasNext(); ) {
            Field field = entity.getField(iter.next());
            if (field.isRepeatable()
                    || !checkRecord.hasValue(field.getName(), false)
                    || MetadataHelper.isCommonsField(field)
                    || EasyMetaFactory.getDisplayType(field) == DisplayType.SERIES) {
                continue;
            }
            checkFields.add(field.getName());
        }

        if (checkFields.isEmpty()) return Collections.emptyList();

        // OR AND
        final String orAnd = StringUtils.defaultString(
                EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.REPEAT_FIELDS_CHECK_MODE), "or");

        StringBuilder checkSql = new StringBuilder("select ")
                .append(entity.getPrimaryField().getName()).append(", ")  // 增加一个主键列
                .append(StringUtils.join(checkFields.iterator(), ", "))
                .append(" from ")
                .append(entity.getName())
                .append(" where ( ");
        for (String field : checkFields) {
            checkSql.append(field).append(" = ? ").append(orAnd).append(" ");
        }
        checkSql.delete(checkSql.lastIndexOf("?") + 1, checkSql.length()).append(" )");

        // 排除自己
        if (checkRecord.getPrimary() != null) {
            checkSql.append(String.format(" and (%s <> '%s')",
                    entity.getPrimaryField().getName(), checkRecord.getPrimary()));
        }

        // 明细实体
        if (entity.getMainEntity() != null) {
            String globalRepeat = EasyMetaFactory.valueOf(entity).getExtraAttr(EasyEntityConfigProps.DETAILS_GLOBALREPEAT);
            // v3.4
            if (!BooleanUtils.toBoolean(globalRepeat)) {
                String dtf = MetadataHelper.getDetailToMainField(entity).getName();
                ID mainid = checkRecord.getID(dtf);
                if (mainid == null) {
                    log.warn("Check all records of detail for repeatable");
                } else {
                    checkSql.append(String.format(" and (%s = '%s')", dtf, mainid));
                }
            }
        }

        Query query = ((BaseService) delegateService).getPersistManagerFactory().createQuery(checkSql.toString());

        int index = 1;
        for (String field : checkFields) {
            query.setParameter(index++, checkRecord.getObjectValue(field));
        }
        return query.setLimit(limit).list();
    }

    @Override
    public void approve(ID recordId, ApprovalState state, ID approvalUser) {
        Assert.isTrue(
                state == ApprovalState.REVOKED || state == ApprovalState.APPROVED,
                "Only REVOKED or APPROVED allowed");

        if (approvalUser == null) {
            approvalUser = UserService.SYSTEM_USER;
            log.warn("Use '{}' do approve : {}", approvalUser, recordId);
        }

        Record approvalRecord = EntityHelper.forUpdate(recordId, approvalUser, false);
        approvalRecord.setInt(EntityHelper.ApprovalState, state.getState());
        delegateService.update(approvalRecord);

        // 触发器

        final RobotTriggerManual triggerManual = new RobotTriggerManual();

        // ND 传导给明细触发器（若有）
        for (Entity de : approvalRecord.getEntity().getDetialEntities()) {
            TriggerAction[] deHasTriggers = getSpecTriggers(de, null,
                    state == ApprovalState.APPROVED ? TriggerWhen.APPROVED : TriggerWhen.REVOKED);
            if (deHasTriggers.length > 0) {
                for (ID did : QueryHelper.detailIdsNoFilter(recordId, de)) {
                    Record dAfter = EntityHelper.forUpdate(did, approvalUser, false);
                    if (state == ApprovalState.REVOKED) {
                        triggerManual.onRevoked(
                                OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, dAfter));
                    } else {
                        triggerManual.onApproved(
                                OperatingContext.create(approvalUser, InternalPermission.APPROVAL, null, dAfter));
                    }
                }
            }
        }

        // 主记录
        Record before = approvalRecord.clone();
        if (state == ApprovalState.REVOKED) {
            before.setInt(EntityHelper.ApprovalState, ApprovalState.APPROVED.getState());
            triggerManual.onRevoked(
                    OperatingContext.create(approvalUser, InternalPermission.APPROVAL, before, approvalRecord));
        } else {
            before.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
            triggerManual.onApproved(
                    OperatingContext.create(approvalUser, InternalPermission.APPROVAL, before, approvalRecord));
        }

        // 手动记录历史
        new RevisionHistoryObserver().onApprovalManual(
                OperatingContext.create(approvalUser, InternalPermission.APPROVAL, before, approvalRecord));
    }

    @Override
    public String toString() {
        return getEntityCode() + "#" + super.toString();
    }
}