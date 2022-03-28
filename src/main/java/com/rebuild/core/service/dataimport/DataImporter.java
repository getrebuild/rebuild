/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

import cn.devezhao.commons.ThreadPool;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Query;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.EntityRecordCreator;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.service.general.GeneralEntityServiceContextHolder;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
    private ID owningUser;

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

        owningUser = rule.getDefaultOwningUser() != null ? rule.getDefaultOwningUser() : getUser();
        GeneralEntityServiceContextHolder.setSkipSeriesValue();

        for (final Cell[] row : rows) {
            if (isInterrupt()) {
                this.setInterrupted();
                break;
            }

            Cell fc = row == null || row.length == 0 ? null : row[0];
            if (fc == null || fc.getRowNo() == 0) {
                continue;
            }

            try {
                Record record = checkoutRecord(row);
                if (record == null) {
                    traceLogs.add(new Object[] { fc.getRowNo(), "SKIP" });
                } else {
                    boolean isNew = record.getPrimary() == null;
                    record = Application.getEntityService(rule.getToEntity().getEntityCode()).createOrUpdate(record);
                    this.addSucceeded();

                    traceLogs.add(new Object[] { fc.getRowNo(),
                            isNew ? "CREATED" : "UPDATED", record.getPrimary(), cellTraces });
                }

            } catch (Exception ex) {
                traceLogs.add(new Object[] { fc.getRowNo(), "ERROR", ex.getLocalizedMessage() });
                log.error("ROW#{} > {}", fc.getRowNo(), ex.getLocalizedMessage());
            }

            ThreadPool.waitFor(1500);
            this.addCompleted();
        }
        return this.getSucceeded();
    }

    @Override
    protected void completedAfter() {
        super.completedAfter();
        GeneralEntityServiceContextHolder.isSkipSeriesValue(true);
    }

    /**
     * @param row
     * @return
     */
    protected Record checkoutRecord(Cell[] row) {
        Record recordHub = EntityHelper.forNew(rule.getToEntity().getEntityCode(), this.owningUser);

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
                checkout = EntityHelper.forUpdate(repeat, this.owningUser);
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
            new EntityRecordCreator(rule.getToEntity(), JSONUtils.EMPTY_OBJECT, null)
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

        log.info("Checking repeated : " + wheres);
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
     * @return
     */
    public List<Object[]> getTraceLogs() {
        return traceLogs;
    }
}
