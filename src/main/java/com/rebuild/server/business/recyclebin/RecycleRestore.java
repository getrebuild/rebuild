/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.recyclebin;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.engine.PersistManagerImpl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.TransactionManual;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据恢复
 *
 * @author devezhao
 * @since 2019/8/21
 */
public class RecycleRestore {

    private static final Log LOG = LogFactory.getLog(RecycleRestore.class);

    private ID recycleId;

    /**
     * @param recycleId
     */
    public RecycleRestore(ID recycleId) {
        this.recycleId = recycleId;
    }

    /**
     * 恢复数据
     *
     * @return
     */
    public int restore() {
        return restore(false);
    }

    /**
     * 恢复数据
     *
     * @param cascade 级联恢复
     * @return
     */
    public int restore(boolean cascade) {
        Object[] main = Application.createQueryNoFilter(
                "select recordContent,recordId,recycleId from RecycleBin where recycleId = ?")
                .setParameter(1, this.recycleId)
                .unique();
        // 可能已经（关联）恢复了
        if (main == null) {
            LOG.warn("No recycle found! Maybe restored : " + this.recycleId);
            return 0;
        }

        final List<ID> recycleIds = new ArrayList<>();

        final List<Record> willRestores = new ArrayList<>(toRecord(JSON.parseObject((String) main[0]), (ID) main[1]));
        if (willRestores.isEmpty()) {
            throw new RebuildException("记录的所属实体不存在");
        }
        recycleIds.add((ID) main[2]);

        if (cascade) {
            Object[][] array = Application.createQueryNoFilter(
                    "select recordContent,recordId,recycleId from RecycleBin where channelWith = ?")
                    .setParameter(1, main[1])
                    .array();
            for (Object[] o : array) {
                List<Record> records = toRecord(JSON.parseObject((String) o[0]), (ID) o[1]);
                if (!records.isEmpty()) {
                    willRestores.addAll(records);
                    recycleIds.add((ID) o[2]);
                }
            }
        }

        // 启动事物
        final TransactionStatus status = TransactionManual.newTransaction();

        int restored = 0;
        PersistManagerImpl PM = (PersistManagerImpl) Application.getPersistManagerFactory().createPersistManager();
        try {
            for (Record r : willRestores) {
                String primaryName = r.getEntity().getPrimaryField().getName();
                ID primaryId = (ID) r.removeValue(primaryName);
                PM.saveInternal(r, primaryId);

                // 非明细才计数
                if (r.getEntity().getMasterEntity() == null) {
                    restored++;
                }
            }

            PM.delete(recycleIds.toArray(new ID[0]));

            TransactionManual.commit(status);
            return restored;

        } catch (Throwable ex) {
            TransactionManual.rollback(status);
            throw new RebuildException("恢复数据失败", ex);
        }
    }

    /**
     * @param content
     * @param recordId
     * @return
     */
    private List<Record> toRecord(JSONObject content, ID recordId) {
        if (!MetadataHelper.containsEntity(recordId.getEntityCode())) {
            return Collections.emptyList();
        }

        JSONArray slaveList = content.getJSONArray(RecycleBean.NAME_SLAVELIST);
        if (slaveList != null) {
            content.remove(RecycleBean.NAME_SLAVELIST);
        }

        List<Record> records = new ArrayList<>();

        Entity entity = MetadataHelper.getEntity(recordId.getEntityCode());
        Record record = new RestoreRecordCreator(entity, content).create(true);
        records.add(record);

        Entity slaveEntity = entity.getSlaveEntity();
        if (slaveList != null && slaveEntity != null) {
            for (Object o : slaveList) {
                Record slave = new RestoreRecordCreator(slaveEntity, (JSONObject) o).create(true);
                records.add(slave);
            }
        }
        return records;
    }
}
