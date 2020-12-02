/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.general.recyclebin;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedList;

/**
 * 数据转存
 *
 * @author devezhao
 * @since 2019/8/19
 */
public class RecycleStore {

    private ID user;
    private LinkedList<Object[]> data;

    /**
     * @param user
     */
    public RecycleStore(ID user) {
        this.user = user == null ? UserContextHolder.getUser() : user;
        this.data = new LinkedList<>();
    }

    /**
     * 添加待转存记录
     *
     * @param recordId
     */
    public void add(ID recordId) {
        this.add(recordId, null);
    }

    /**
     * 添加待转存记录
     *
     * @param recordId
     * @param with
     */
    public void add(ID recordId, ID with) {
        JSON s = new RecycleBean(recordId).serialize();
        data.add(new Object[]{recordId, s, with});
    }

    /**
     * 移除最后添加的转存记录
     */
    public void removeLast() {
        data.removeLast();
    }

    /**
     * 转存
     *
     * @return
     */
    public int store() {
        Record record = EntityHelper.forNew(EntityHelper.RecycleBin, UserService.SYSTEM_USER);
        record.setID("deletedBy", this.user);
        record.setDate("deletedOn", CalendarUtils.now());

        int affected = 0;
        for (Object[] o : data) {
            Record clone = record.clone();
            ID recordId = (ID) o[0];
            Entity belongEntity = MetadataHelper.getEntity(recordId.getEntityCode());
            clone.setString("belongEntity", belongEntity.getName());

            JSONObject recordContent = (JSONObject) o[1];
            String recordName = recordContent.getString(belongEntity.getNameField().getName());
            if (StringUtils.isBlank(recordName)) {
                recordName = FieldValueHelper.NO_LABEL_PREFIX + recordId.toLiteral().toUpperCase();
            }

            clone.setID("recordId", recordId);
            clone.setString("recordName", recordName);
            clone.setString("recordContent", recordContent.toJSONString());
            if (o[2] != null) {
                clone.setID("channelWith", (ID) o[2]);
            }
            Application.getCommonsService().create(clone);
            affected++;
        }

        return affected;
    }
}
