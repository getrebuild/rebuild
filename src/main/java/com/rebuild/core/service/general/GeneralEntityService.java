/*
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
import com.rebuild.core.service.trigger.RobotTriggerManual;
import com.rebuild.core.service.trigger.RobotTriggerObserver;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.TaskExecutors;
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
 * 如有需要，其他实体可根据自身业务集成并实现/改写实现
 *
 * @author zhaofang123@gmail.com
 * @since 11/06/2017
 */
@Service
public class GeneralEntityService extends ObservableService implements EntityService {

    /**
     * @param aPMFactory
     */
    protected GeneralEntityService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);

        // 通知
        addObserver(new NotificationObserver());
        // 触发器
        addObserver(new RobotTriggerObserver());

        // Redis 队列（Redis 可用才有效）
        if (RebuildConfiguration.getBool(ConfigurationItem.RedisQueueEnable)) {
            addObserver(new RedisQueueObserver());
        }
    }

    @Override
    public int getEntityCode() {
        return 0;
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
        return super.update(record);
    }

    @Override
    public int delete(ID record) {
        return delete(record, null);
    }

    @Override
    public int delete(ID record, String[] cascades) {
        final ID currentUser = UserContextHolder.getUser();

        RecycleStore recycleBin = null;
        if (RebuildConfiguration.getInt(ConfigurationItem.RecycleBinKeepingDays) > 0) {
            recycleBin = new RecycleStore(currentUser);
        } else {
            LOG.warn("RecycleBin inactivated : " + record + " by " + currentUser);
        }

        if (recycleBin != null) {
            recycleBin.add(record);
        }
        this.deleteInternal(record);
        int affected = 1;

        Map<String, Set<ID>> recordsOfCascaded = getCascadedRecords(record, cascades, BizzPermission.DELETE);
        for (Map.Entry<String, Set<ID>> e : recordsOfCascaded.entrySet()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cascade delete - " + e.getKey() + " > " + e.getValue());
            }

            for (ID id : e.getValue()) {
                if (Application.getPrivilegesManager().allowDelete(currentUser, id)) {
                    if (recycleBin != null) {
                        recycleBin.add(id, record);
                    }

                    int deleted = 0;
                    try {
                        deleted = this.deleteInternal(id);
                    } catch (DataSpecificationException ex) {
                        LOG.warn("Cannot delete : " + id + " Ex : " + ex);
                    } finally {
                        if (deleted > 0) {
                            affected++;
                        } else if (recycleBin != null) {
                            recycleBin.removeLast();
                        }
                    }
                } else {
                    LOG.warn("No have privileges to DELETE : " + currentUser + " > " + id);
                }
            }
        }

        if (recycleBin != null) {
            recycleBin.store();
        }

        return affected;
    }

    /**
     * @param record
     * @return
     * @throws DataSpecificationException
     */
    private int deleteInternal(ID record) throws DataSpecificationException {
        Record delete = EntityHelper.forUpdate(record, UserContextHolder.getUser());
        if (!checkModifications(delete, BizzPermission.DELETE)) {
            return 0;
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("记录所属人未变化，忽略 : " + record);
            }
        } else {
            assignBefore = countObservers() > 0 ? record(assignAfter) : null;

            delegateService.update(assignAfter);
            Application.getRecordOwningCache().cleanOwningUser(record);
            affected = 1;
        }

        Map<String, Set<ID>> cass = getCascadedRecords(record, cascades, BizzPermission.ASSIGN);
        for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("级联分派 - " + e.getKey() + " > " + e.getValue());
            }
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
    public int share(ID record, ID to, String[] cascades) {
        final ID currentUser = UserContextHolder.getUser();
        final String entityName = MetadataHelper.getEntityName(record);

        final Record sharedAfter = EntityHelper.forNew(EntityHelper.ShareAccess, currentUser);
        sharedAfter.setID("recordId", record);
        sharedAfter.setID("shareTo", to);
        sharedAfter.setString("belongEntity", entityName);
        // TODO 目前仅共享读取权限
        sharedAfter.setInt("rights", BizzPermission.READ.getMask());

        Object[] hasShared = ((BaseService) delegateService).getPersistManagerFactory().createQuery(
                "select accessId from ShareAccess where belongEntity = ? and recordId = ? and shareTo = ?")
                .setParameter(1, entityName)
                .setParameter(2, record)
                .setParameter(3, to)
                .unique();

        int affected = 0;
        boolean shareChange = false;
        if (hasShared != null) {
            // No need to change
            if (LOG.isDebugEnabled()) {
                LOG.debug("记录已共享过，忽略 : " + record);
            }
        } else if (to.equals(Application.getRecordOwningCache().getOwningUser(record))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("共享至与记录所属为同一用户，忽略 : " + record);
            }
        } else {
            delegateService.create(sharedAfter);
            affected = 1;
            shareChange = true;
        }

        Map<String, Set<ID>> cass = getCascadedRecords(record, cascades, BizzPermission.SHARE);
        for (Map.Entry<String, Set<ID>> e : cass.entrySet()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("级联共享 - " + e.getKey() + " > " + e.getValue());
            }
            for (ID casid : e.getValue()) {
                affected += share(casid, to, null);
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
        ID currentUser = UserContextHolder.getUser();

        Record unsharedBefore = null;
        if (countObservers() > 0) {
            unsharedBefore = EntityHelper.forUpdate(accessId, currentUser);
            unsharedBefore.setNull("belongEntity");
            unsharedBefore.setNull("recordId");
            unsharedBefore.setNull("shareTo");
            unsharedBefore = record(unsharedBefore);
        }

        delegateService.delete(accessId);

        if (countObservers() > 0) {
            setChanged();
            notifyObservers(OperatingContext.create(currentUser, UNSHARE, unsharedBefore, null));
        }
        return 1;
    }

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
     * @param recordMain      主记录
     * @param cascadeEntities 级联实体
     * @param action          动作
     * @return
     */
    protected Map<String, Set<ID>> getCascadedRecords(ID recordMain, String[] cascadeEntities, Permission action) {
        if (cascadeEntities == null || cascadeEntities.length == 0) {
            return Collections.emptyMap();
        }

        Map<String, Set<ID>> entityRecordsMap = new HashMap<>();
        Entity mainEntity = MetadataHelper.getEntity(recordMain.getEntityCode());
        for (String cas : cascadeEntities) {
            Entity casEntity = MetadataHelper.getEntity(cas);

            StringBuilder sql = new StringBuilder(
                    String.format("select %s from %s where ( ", casEntity.getPrimaryField().getName(), casEntity.getName()));

            Field[] reftoFields = MetadataHelper.getReferenceToFields(mainEntity, casEntity);
            for (Field field : reftoFields) {
                sql.append(field.getName()).append(" = '").append(recordMain).append("' or ");
            }
            // remove last ' or '
            sql.replace(sql.length() - 4, sql.length(), " )");

            Filter filter = Application.getPrivilegesManager().createQueryFilter(UserContextHolder.getUser(), action);
            Object[][] array = Application.getQueryFactory().createQuery(sql.toString(), filter).array();

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
                    String stateType = state == ApprovalState.APPROVED ? "RecordApproved" : "RecordInApproval";
                    throw new DataSpecificationException(Language.L("MainRecordApprovedNotAddDetailTips", stateType));
                }
            }

        } else {
            final Entity checkEntity = mainEntity != null ? mainEntity : entity;
            ID recordId = record.getPrimary();

            if (checkEntity.containsField(EntityHelper.ApprovalId)) {
                // 需要验证主记录
                String recordType = "Record";
                if (mainEntity != null) {
                    recordId = getMainId(entity, recordId);
                    recordType = "MainRecord";
                }

                ApprovalState currentState;
                ApprovalState changeState = null;
                try {
                    currentState = ApprovalHelper.getApprovalState(recordId);
                    if (record.hasValue(EntityHelper.ApprovalState)) {
                        changeState = (ApprovalState) ApprovalState.valueOf(record.getInt(EntityHelper.ApprovalState));
                    }

                } catch (NoRecordFoundException ignored) {
                    LOG.warn("No record found for check : " + recordId);
                    return false;
                }

                boolean rejected = false;
                if (action == BizzPermission.DELETE) {
                    rejected = currentState == ApprovalState.APPROVED || currentState == ApprovalState.PROCESSING;
                } else if (action == BizzPermission.UPDATE) {
                    rejected = (currentState == ApprovalState.APPROVED && changeState != ApprovalState.CANCELED) /* 管理员撤销 */
                            || (currentState == ApprovalState.PROCESSING && !GeneralEntityServiceContextHolder.isAllowForceUpdateOnce() /* 审批时修改 */);
                }

                if (rejected) {
                    if (RobotTriggerObserver.getTriggerSource() != null) {
                        recordType = "RelatedRecord";
                    }

                    String errorMsg;
                    if (currentState == ApprovalState.APPROVED) {
                        errorMsg = Language.L("SomeRecordApprovedTips", recordType);
                    } else {
                        errorMsg = Language.L("SomeRecordInApprovalTips", recordType);
                    }
                    throw new DataSpecificationException(errorMsg);
                }
            }
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
        Field[] seriesFields = MetadataSorter.sortFields(record.getEntity(), DisplayType.SERIES);
        for (Field field : seriesFields) {
            // 不强制生成
            if (record.hasValue(field.getName())
                    && GeneralEntityServiceContextHolder.isSkipSeriesValue(false)) {
                continue;
            }
            record.setString(field.getName(), SeriesGeneratorFactory.generate(field));
        }
    }

    /**
     * 获取主记录ID
     *
     * @param detailEntity
     * @param detailId
     * @return
     * @throws NoRecordFoundException
     */
    private ID getMainId(Entity detailEntity, ID detailId) throws NoRecordFoundException {
        Field dtmField = MetadataHelper.getDetailToMainField(detailEntity);
        Object[] o = Application.getQueryFactory().uniqueNoFilter(detailId, dtmField.getName());
        if (o == null) {
            throw new NoRecordFoundException(detailId);
        }
        return (ID) o[0];
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
            checkSql.append(" and ").append(entity.getPrimaryField().getName())
                    .append(" <> ")
                    .append(String.format("'%s'", checkRecord.getPrimary().toLiteral()));
        }

        Query query = ((BaseService) delegateService).getPersistManagerFactory().createQuery(checkSql.toString());

        int index = 1;
        for (String field : checkFields) {
            query.setParameter(index++, checkRecord.getObjectValue(field));
        }
        return query.setLimit(limit).list();
    }

    @Override
    public void approve(ID record, ApprovalState state) {
        Assert.isTrue(
                state == ApprovalState.REVOKED || state == ApprovalState.APPROVED,
                "Only REVOKED or APPROVED allowed");

        Record approvalRecord = EntityHelper.forUpdate(record, UserService.SYSTEM_USER, false);
        approvalRecord.setInt(EntityHelper.ApprovalState, state.getState());
        super.update(approvalRecord);

        // 触发器

        ID opUser = UserContextHolder.getUser();
        Record before = approvalRecord.clone();
        RobotTriggerManual triggerManual = new RobotTriggerManual();

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
