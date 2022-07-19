/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.*;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.DeleteRecord;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.NoRecordFoundException;
import com.rebuild.core.service.approval.ApprovalHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import com.rebuild.core.service.general.series.SeriesGeneratorFactory;
import com.rebuild.core.service.notification.NotificationObserver;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.service.trigger.*;
import com.rebuild.core.service.trigger.impl.GroupAggregation;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 业务实体核心服务，所有业务实体都应该使用此类（或子类）
 * <br>- 有业务验证
 * <br>- 会带有系统设置规则的执行
 * <br>- 会开启一个事务，详见 <tt>application-bean.xml</tt> 配置
 *
 * 如有需要，其他实体可根据自身业务继承并复写
 *
 * FIXME 删除主记录时会关联删除明细记录（持久层实现），但明细记录不会触发业务规则
 *
 * @author Zixin (RB)
 * @since 11/06/2017
 */
@Slf4j
@Service
public class GeneralEntityService extends ObservableService implements EntityService {

    // 有明细
    public static final String HAS_DETAILS = "$DETAILS$";

    protected GeneralEntityService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);

        // 通知
        addObserver(new NotificationObserver());
        // 触发器
        addObserver(new RobotTriggerObserver());
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

        record = super.createOrUpdate(record);
        if (details == null || details.isEmpty()) return record;

        // 主记录+明细记录处理

        String dtf = MetadataHelper.getDetailToMainField(record.getEntity().getDetailEntity()).getName();
        ID mainid = record.getPrimary();

        boolean checkDetailsRepeated = rcm == GeneralEntityServiceContextHolder.RCM_CHECK_DETAILS
                || rcm == GeneralEntityServiceContextHolder.RCM_CHECK_ALL;

        // 使用明细实体 Service
        final EntityService des = Application.getEntityService(record.getEntity().getDetailEntity().getEntityCode());

        // 先删除
        for (Record d : details) {
            if (d instanceof DeleteRecord) des.delete(d.getPrimary());
        }

        // 再保存
        for (Record d : details) {
            if (d instanceof DeleteRecord) continue;

            if (checkDetailsRepeated) {
                d.setID(dtf, mainid);  // for check

                List<Record> repeated = des.getAndCheckRepeated(d, 20);
                if (!repeated.isEmpty()) {
                    throw new RepeatedRecordsException(repeated);
                }
            }

            if (d.getPrimary() == null) {
                d.setID(dtf, mainid);
                des.create(d);
            } else {
                des.update(d);
            }
        }

        return record;
    }

    @Override
    public Record create(Record record) {
        appendDefaultValue(record);
        checkModifications(record, BizzPermission.CREATE);
        setSeriesValue(record);
        return super.create(record);
    }

    @Override
    public Record update(Record record) {
        if (!checkModifications(record, BizzPermission.UPDATE)) {
            return record;
        }

        // 先更新
        record = super.update(record);

        // 传导给明细（若有）
        // 仅分组聚合触发器
        Entity de = record.getEntity().getDetailEntity();
        if (de != null) {
            TriggerAction[] hasTriggers = RobotTriggerManager.instance.getActions(de, TriggerWhen.UPDATE);
            boolean hasGroupAggregation = false;
            for (TriggerAction ta : hasTriggers) {
                if (ta instanceof GroupAggregation) {
                    hasGroupAggregation = true;
                    break;
                }
            }

            if (hasGroupAggregation) {
                RobotTriggerManual triggerManual = new RobotTriggerManual();
                ID opUser = UserService.SYSTEM_USER;

                for (ID did : QueryHelper.detailIdsNoFilter(record.getPrimary(), 1)) {
                    Record dUpdate = EntityHelper.forUpdate(did, opUser, false);
                    triggerManual.onUpdate(
                            OperatingContext.create(opUser, BizzPermission.UPDATE, dUpdate, dUpdate));
                }
            }
        }

        return record;
    }

    @Override
    public int delete(ID record) {
        return delete(record, null);
    }

    @Override
    public int delete(ID record, String[] cascades) {
        final ID currentUser = UserContextHolder.getUser();
        final RecycleStore recycleBin = useRecycleStore(record);

        this.deleteInternal(record);
        int affected = 1;

        Map<String, Set<ID>> recordsOfCascaded = getCascadedRecords(record, cascades, BizzPermission.DELETE);
        for (Map.Entry<String, Set<ID>> e : recordsOfCascaded.entrySet()) {
            log.info("Cascading delete - {} > {} ", e.getKey(), e.getValue());

            for (ID id : e.getValue()) {
                if (Application.getPrivilegesManager().allowDelete(currentUser, id)) {
                    if (recycleBin != null) recycleBin.add(id, record);

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
     * @param record
     * @return
     * @throws DataSpecificationException
     */
    protected int deleteInternal(ID record) throws DataSpecificationException {
        Record delete = EntityHelper.forUpdate(record, UserContextHolder.getUser());
        if (!checkModifications(delete, BizzPermission.DELETE)) {
            return 0;
        }

        // 手动删除明细记录，以执行系统规则（如触发器、附件删除等）
        Entity de = MetadataHelper.getEntity(record.getEntityCode()).getDetailEntity();
        if (de != null) {
            for (ID did : QueryHelper.detailIdsNoFilter(record, 0)) {
                // 明细无约束检查 checkModifications
                // 不使用明细实体 Service
                super.delete(did);
            }
        }

        return super.delete(record);
    }

    @Override
    public int assign(ID record, ID to, String[] cascades) {
        final User toUser = Application.getUserStore().getUser(to);
        final Record assignAfter = EntityHelper.forUpdate(record, (ID) toUser.getIdentity(), false);
        assignAfter.setID(EntityHelper.OwningUser, (ID) toUser.getIdentity());
        assignAfter.setID(EntityHelper.OwningDept, (ID) toUser.getOwningDept().getIdentity());

        // 分配前数据
        Record assignBefore = null;

        int affected = 0;
        if (to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
            // No need to change
            log.debug("The record owner has not changed, ignore : {}", record);
        } else {
            assignBefore = countObservers() > 0 ? recordSnap(assignAfter) : null;

            delegateService.update(assignAfter);
            Application.getRecordOwningCache().cleanOwningUser(record);
            affected = 1;
        }

        Map<String, Set<ID>> cass = getCascadedRecords(record, cascades, BizzPermission.ASSIGN);
        for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
            log.info("Cascading assign - {} > {}", e.getKey(), e.getValue());

            for (ID casid : e.getValue()) {
                affected += assign(casid, to, null);
            }
        }

        if (countObservers() > 0 && assignBefore != null) {
            setChanged();
            notifyObservers(OperatingContext.create(UserContextHolder.getUser(), BizzPermission.ASSIGN, assignBefore, assignAfter));
        }
        return affected;
    }

    @Override
    public int share(ID record, ID to, String[] cascades, int rights) {
        final ID currentUser = UserContextHolder.getUser();
        final String entityName = MetadataHelper.getEntityName(record);

        boolean fromTriggerNoDowngrade = GeneralEntityServiceContextHolder.isFromTrigger(false);
        if (!fromTriggerNoDowngrade) {
            // 如用户无更新权限，则降级为只读共享
            if ((rights & BizzPermission.UPDATE.getMask()) != 0) {
                if (!Application.getPrivilegesManager().allowUpdate(to, record.getEntityCode()) /* 目标用户无基础更新权限 */
                        || !Application.getPrivilegesManager().allow(currentUser, record, BizzPermission.UPDATE, true) /* 操作用户无记录更新权限 */) {
                    rights = BizzPermission.READ.getMask();
                    log.warn("Downgrade share rights to READ(8) : {}", record);
                }
            }
        }

        final Record sharedAfter = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
        sharedAfter.setID("recordId", record);
        sharedAfter.setID("shareTo", to);
        sharedAfter.setString("belongEntity", entityName);
        sharedAfter.setInt("rights", rights);

        Object[] hasShared = ((BaseService) delegateService).getPersistManagerFactory().createQuery(
                "select accessId,rights from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
                .setParameter(1, entityName)
                .setParameter(2, record)
                .setParameter(3, to)
                .unique();

        int affected = 0;
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
                log.debug("The record has been shared and has the same rights, ignore : {}", record);
            }

        } else {
            // 可以共享给自己
            if (log.isDebugEnabled()
                    && to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
                log.debug("Share to the same user as the record, ignore : {}", record);
            }

            delegateService.create(sharedAfter);
            affected = 1;
            shareChange = true;
        }

        Map<String, Set<ID>> cass = getCascadedRecords(record, cascades, BizzPermission.SHARE);
        for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
            log.info("Cascading share - {} > {}", e.getKey(), e.getValue());

            for (ID casid : e.getValue()) {
                affected += share(casid, to, null, rights);
            }
        }

        if (countObservers() > 0 && shareChange) {
            setChanged();
            notifyObservers(OperatingContext.create(currentUser, BizzPermission.SHARE, null, sharedAfter));
        }
        return affected;
    }

    @Override
    public int unshare(ID record, ID accessId) {
        final ID currentUser = UserContextHolder.getUser();

        Record unsharedBefore = null;
        if (countObservers() > 0) {
            unsharedBefore = EntityHelper.forUpdate(accessId, currentUser);
            unsharedBefore.setNull("belongEntity");
            unsharedBefore.setNull("recordId");
            unsharedBefore.setNull("shareTo");
            unsharedBefore = recordSnap(unsharedBefore);
        }

        delegateService.delete(accessId);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(currentUser, UNSHARE, unsharedBefore, null));
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
     * @param recordMain 主记录
     * @param cascadeEntities 级联实体
     * @param action 动作
     * @return
     */
    protected Map<String, Set<ID>> getCascadedRecords(ID recordMain, String[] cascadeEntities, Permission action) {
        if (cascadeEntities == null || cascadeEntities.length == 0) {
            return Collections.emptyMap();
        }

        final boolean fromTriggerNoFilter = GeneralEntityServiceContextHolder.isFromTrigger(false);

        Map<String, Set<ID>> entityRecordsMap = new HashMap<>();
        Entity mainEntity = MetadataHelper.getEntity(recordMain.getEntityCode());

        for (String cas : cascadeEntities) {
            if (!MetadataHelper.containsEntity(cas)) {
                log.warn("The entity not longer exists : {}", cas);
                continue;
            }

            Entity casEntity = MetadataHelper.getEntity(cas);
            Field[] reftoFields = MetadataHelper.getReferenceToFields(mainEntity, casEntity);
            if (reftoFields.length == 0) {
                log.warn("No any fields of refto found : {} << {}", cas, mainEntity.getName());
                continue;
            }

            List<String> or = new ArrayList<>();
            for (Field field : reftoFields) {
                or.add(String.format("%s = '%s'", field.getName(), recordMain));
            }

            String sql = String.format("select %s from %s where ( %s )",
                    casEntity.getPrimaryField().getName(), casEntity.getName(),
                    StringUtils.join(or.iterator(), " or "));

            Object[][] array;
            if (fromTriggerNoFilter) {
                array = Application.createQueryNoFilter(sql).array();
            } else {
                Filter filter = Application.getPrivilegesManager().createQueryFilter(UserContextHolder.getUser(), action);
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
        } else if (context.getAction() == UNSHARE) {
            return new BulkUnshare(context, this);
        } else if (context.getAction() == BizzPermission.UPDATE) {
            return new BulkBacthUpdate(context, this);
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
            if (mainEntity != null && MetadataHelper.hasApprovalField(record.getEntity())) {
                Field dtmField = MetadataHelper.getDetailToMainField(entity);
                ApprovalState state = ApprovalHelper.getApprovalState(record.getID(dtmField.getName()));

                if (state == ApprovalState.APPROVED || state == ApprovalState.PROCESSING) {
                    throw new DataSpecificationException(state == ApprovalState.APPROVED
                            ? Language.L("主记录已完成审批，不能添加明细")
                            : Language.L("主记录正在审批中，不能添加明细"));
                }
            }

        } else {
            final Entity checkEntity = mainEntity != null ? mainEntity : entity;
            ID recordId = record.getPrimary();

            if (checkEntity.containsField(EntityHelper.ApprovalId)) {
                // 需要验证主记录
                String recordType = Language.L("记录");
                if (mainEntity != null) {
                    recordId = QueryHelper.getMainIdByDetail(recordId);
                    recordType = Language.L("主记录");
                }

                ApprovalState currentState;
                ApprovalState changeState = null;
                try {
                    currentState = ApprovalHelper.getApprovalState(recordId);
                    if (record.hasValue(EntityHelper.ApprovalState)) {
                        changeState = (ApprovalState) ApprovalState.valueOf(record.getInt(EntityHelper.ApprovalState));
                    }

                } catch (NoRecordFoundException ignored) {
                    log.warn("No record found for check : " + recordId);
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

            EasyField easyField = EasyMetaFactory.valueOf(field);
            if (easyField.getDisplayType() == DisplayType.SERIES) continue;

            Object defaultValue = easyField.exprDefaultValue();
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

            record.setString(field.getName(), SeriesGeneratorFactory.generate(field));
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

        if (checkFields.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder checkSql = new StringBuilder("select ")
                .append(entity.getPrimaryField().getName()).append(", ")  // 增加一个主键列
                .append(StringUtils.join(checkFields.iterator(), ", "))
                .append(" from ")
                .append(entity.getName())
                .append(" where ( ");
        for (String field : checkFields) {
            checkSql.append(field).append(" = ? or ");
        }
        checkSql.delete(checkSql.length() - 4, checkSql.length()).append(" )");

        // 排除自己
        if (checkRecord.getPrimary() != null) {
            checkSql.append(String.format(" and (%s <> '%s')",
                    entity.getPrimaryField().getName(), checkRecord.getPrimary()));
        }

        // 明细实体在主记录下检查重复
        if (entity.getMainEntity() != null) {
            String dtf = MetadataHelper.getDetailToMainField(entity).getName();
            ID mainid = checkRecord.getID(dtf);
            if (mainid == null) {
                log.warn("Check all detail records for repeated");
            } else {
                checkSql.append(String.format(" and (%s = '%s')", dtf, mainid));
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
    public void approve(ID record, ApprovalState state, ID approvalUser) {
        Assert.isTrue(
                state == ApprovalState.REVOKED || state == ApprovalState.APPROVED,
                "Only REVOKED or APPROVED allowed");

        Record approvalRecord = EntityHelper.forUpdate(record, UserService.SYSTEM_USER, false);
        approvalRecord.setInt(EntityHelper.ApprovalState, state.getState());
        if (state == ApprovalState.APPROVED
                && approvalUser != null
                && MetadataHelper.getEntity(record.getEntityCode()).containsField(EntityHelper.ApprovalLastUser)) {
            approvalRecord.setID(EntityHelper.ApprovalLastUser, approvalUser);
        }

        delegateService.update(approvalRecord);

        // 触发器

        ID opUser = UserContextHolder.getUser();
        RobotTriggerManual triggerManual = new RobotTriggerManual();

        // 传导给明细（若有）

        Entity de = approvalRecord.getEntity().getDetailEntity();
        TriggerAction[] hasTriggers = de == null ? null : RobotTriggerManager.instance.getActions(de,
                state == ApprovalState.APPROVED ? TriggerWhen.APPROVED : TriggerWhen.REVOKED);
        if (hasTriggers != null && hasTriggers.length > 0) {
            for (ID did : QueryHelper.detailIdsNoFilter(record, 0)) {
                Record dAfter = EntityHelper.forUpdate(did, opUser, false);
                triggerManual.onApproved(
                        OperatingContext.create(opUser, BizzPermission.UPDATE, null, dAfter));
            }
        }

        Record before = approvalRecord.clone();
        if (state == ApprovalState.REVOKED) {
            before.setInt(EntityHelper.ApprovalState, ApprovalState.APPROVED.getState());
            triggerManual.onRevoked(
                    OperatingContext.create(opUser, BizzPermission.UPDATE, before, approvalRecord));
        } else {
            before.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
            triggerManual.onApproved(
                    OperatingContext.create(opUser, BizzPermission.UPDATE, before, approvalRecord));
        }
    }
}