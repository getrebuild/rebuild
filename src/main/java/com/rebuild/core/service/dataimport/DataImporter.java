/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dataimport;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 数据导入
 *
 * @author devezhao
 * @see DisplayType
 * @since 01/09/2019
 */
public class DataImporter extends HeavyTask<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(DataImporter.class);

    final private ImportRule rule;
    private ID owningUser;

    // 记录每一行的错误日志
    private Map<Integer, Object> eachLogs = new LinkedHashMap<>();

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

            Cell firstCell = row == null || row.length == 0 ? null : row[0];
            if (firstCell == null || firstCell.getRowNo() == 0) {
                continue;
            }

            try {
                Record record = checkoutRecord(row);
                if (record != null) {
                    record = Application.getEntityService(rule.getToEntity().getEntityCode()).createOrUpdate(record);
                    this.addSucceeded();
                    eachLogs.put(firstCell.getRowNo(), record.getPrimary());
                }
            } catch (Exception ex) {
                eachLogs.put(firstCell.getRowNo(), ex.getLocalizedMessage());
                LOG.error(firstCell.getRowNo() + " > " + ex);
            }
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
     * 获取错误日志（按错误行）
     *
     * @return
     */
    public Map<Integer, Object> getEachLogs() {
        return eachLogs;
    }

    /**
     * @param row
     * @return
     */
    protected Record checkoutRecord(Cell[] row) {
        Record recordNew = EntityHelper.forNew(rule.getToEntity().getEntityCode(), this.owningUser);

        // 新建
        Record record = new RecordCheckout(rule.getFiledsMapping()).checkout(recordNew, row);

        // 检查重复
        if (rule.getRepeatOpt() < ImportRule.REPEAT_OPT_IGNORE) {
            final ID repeat = getRepeatedRecordId(rule.getRepeatFields(), recordNew);

            if (repeat != null && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_SKIP) {
                return null;
            }

            if (repeat != null && rule.getRepeatOpt() == ImportRule.REPEAT_OPT_UPDATE) {
                // 更新
                record = EntityHelper.forUpdate(repeat, this.owningUser);
                for (Iterator<String> iter = recordNew.getAvailableFieldIterator(); iter.hasNext(); ) {
                    String field = iter.next();
                    if (MetadataHelper.isCommonsField(field)) {
                        continue;
                    }
                    record.setObjectValue(field, recordNew.getObjectValue(field));
                }
            }
        }

        // Verify new record
        if (record.getPrimary() == null) {
            EntityRecordCreator verifier = new EntityRecordCreator(rule.getToEntity(), JSONUtils.EMPTY_OBJECT, null);
            verifier.verify(record, true);
        }
        return record;
    }

    /**
     * @param repeatFields
     * @param data
     * @return
     */
    protected ID getRepeatedRecordId(Field[] repeatFields, Record data) {
        Map<String, Object> wheres = new HashMap<>();
        for (Field c : repeatFields) {
            String cName = c.getName();
            if (data.hasValue(cName)) {
                wheres.put(cName, data.getObjectValue(cName));
            }
        }

        LOG.info("Checking repeated : " + wheres);
        if (wheres.isEmpty()) {
            return null;
        }

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
}
