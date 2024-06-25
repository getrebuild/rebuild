/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.exception.JdbcException;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.general.EntityService;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.service.trigger.impl.FieldAggregation;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.KnownExceptionConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 数据导入
 *
 * @author devezhao
 * @see DisplayType
 * @since 01/09/2019
 */
@Slf4j
public class DataImporter extends HeavyTask<Integer> {

    final private ImportRule rule;

    final private List<Object[]> traceLogs = new ArrayList<>();
    private String cellTraces = null;

    /**
     * @param rule
     */
    public DataImporter(ImportRule rule) {
        this.rule = rule;
    }

    @Override
    protected Integer exec() {
        final List<Cell[]> rows = new DataFileParser(rule.getSourceFile()).parse();
        this.setTotal(rows.size() - 1);

        final ID defaultOwning = ObjectUtils.defaultIfNull(rule.getDefaultOwningUser(), getUser());

        final boolean isViaAdmin = UserHelper.isAdmin(getUser());
        final boolean isAllowCreate;
        if (isViaAdmin) {
            isAllowCreate = true;
        } else if (rule.getToEntity().getMainEntity() == null) {
            isAllowCreate = Application.getPrivilegesManager().allowCreate(getUser(), rule.getToEntity().getEntityCode());
        } else {
            isAllowCreate = Application.getPrivilegesManager().allowUpdate(getUser(), rule.getToEntity().getMainEntity().getEntityCode());
        }

        GeneralEntityServiceContextHolder.setSkipSeriesValue();
        final EntityService ies = Application.getEntityService(rule.getToEntity().getEntityCode());

        for (final Cell[] row : rows) {
            if (isInterruptState()) break;

            final Cell firstCell = row == null || row.length == 0 ? null : row[0];
            if (firstCell == null || firstCell.getRowNo() == 0) continue;

            try {
                Record record = checkoutRecord(row, defaultOwning);
                if (record == null) {
                    traceLogs.add(new Object[] { firstCell.getRowNo(), "SKIP" });
                } else {

                    final boolean isNew = record.getPrimary() == null;

                    if (isNew && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_UPDATE && rule.isOnlyUpdate()) {
                        traceLogs.add(new Object[] { firstCell.getRowNo(), "SKIP" });
                        continue;
                    }

                    if (!isViaAdmin) {
                        String error = null;
                        if (isNew) {
                            if (!isAllowCreate) {
                                error = Language.L("无新建权限");
                            }
                        } else {
                            if (!Application.getPrivilegesManager().allowUpdate(getUser(), record.getPrimary())) {
                                error = Language.L("无编辑权限");
                            }
                        }

                        if (error != null) {
                            traceLogs.add(new Object[] { firstCell.getRowNo(), "ERROR", error });
                            continue;
                        }
                    }

                    record = ies.createOrUpdate(record);
                    this.addSucceeded();

                    traceLogs.add(new Object[] { firstCell.getRowNo(),
                            isNew ? "CREATED" : "UPDATED", record.getPrimary(), cellTraces });
                }

            } catch (Exception ex) {
                String error = ThrowableUtils.getRootCause(ex).getLocalizedMessage();
                log.error("ROW#{} > {}", firstCell.getRowNo(), error, ex);

                if (ex instanceof JdbcException) {
                    String know = KnownExceptionConverter.convert2ErrorMsg(ex);
                    if (know != null) error = know;
                }
                traceLogs.add(new Object[] { firstCell.getRowNo(), "ERROR", error });

            } finally {

                // 可能有级联触发器
                Object ts = FieldAggregation.cleanTriggerChain();
                if (ts != null) log.info("Clean current-loop : {}", ts);

                this.addCompleted();
            }
        }

        return this.getSucceeded();
    }

    @Override
    protected void completedAfter() {
        super.completedAfter();
        GeneralEntityServiceContextHolder.isSkipSeriesValue(true);
    }

    /**
     *
     * @param row
     * @param defaultOwning
     * @return
     */
    protected Record checkoutRecord(Cell[] row, ID defaultOwning) {
        Record recordHub = EntityHelper.forNew(rule.getToEntity().getEntityCode(), defaultOwning);

        // 解析数据
        RecordCheckout recordCheckout = new RecordCheckout(rule.getFiledsMapping());
        Record checkout = recordCheckout.checkout(recordHub, row);

        if (recordCheckout.getTraceLogs().isEmpty()) {
            cellTraces = null;
        } else {
            cellTraces = StringUtils.join(recordCheckout.getTraceLogs(), ", ");
        }

        // 检查重复
        if (rule.getRepeatOpt() < ImportRule.REPEAT_OPT_IGNORE) {
            final ID repeat = findRepeatedRecordId(rule.getRepeatFields(), recordHub);

            if (repeat != null && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_SKIP) {
                return null;
            }

            if (repeat != null && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_UPDATE) {
                // 更新
                checkout = EntityHelper.forUpdate(repeat, defaultOwning);
                for (Iterator<String> iter = recordHub.getAvailableFieldIterator(); iter.hasNext(); ) {
                    String field = iter.next();
                    if (MetadataHelper.isCommonsField(field)) continue;

                    checkout.setObjectValue(field, recordHub.getObjectValue(field));
                }
            }
        }

        // Verify new record
        // Throws DataSpecificationException
        if (checkout.getPrimary() == null) {
            new EntityRecordCreator(rule.getToEntity(), JSONUtils.EMPTY_OBJECT, null, false)
                    .verify(checkout);
        }

        return checkout;
    }

    /**
     * @param repeatFields
     * @param data
     * @return
     */
    protected ID findRepeatedRecordId(Field[] repeatFields, Record data) {
        Map<String, Object> wheres = new HashMap<>();
        for (Field c : repeatFields) {
            String cName = c.getName();
            if (data.hasValue(cName)) {
                wheres.put(cName, data.getObjectValue(cName));
            }
        }

        log.info("Checking repeated : {}", wheres);
        if (wheres.isEmpty()) return null;

        Entity entity = data.getEntity();
        StringBuilder sql = new StringBuilder(String.format("select %s from %s where (1=1)",
                entity.getPrimaryField().getName(), entity.getName()));
        for (String c : wheres.keySet()) {
            sql.append(" and ").append(c).append(" = :").append(c);
        }

        Query query = Application.createQueryNoFilter(sql.toString());
        for (Map.Entry<String, Object> e : wheres.entrySet()) {
            query.setParameter(e.getKey(), e.getValue());
        }

        Object[] exists = query.unique();
        return exists == null ? null : (ID) exists[0];
    }

    /**
     * 错误日志
     *
     * @return
     */
    public List<Object[]> getTraceLogs() {
        return Collections.unmodifiableList(traceLogs);
    }
}
