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

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserService;
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
     */
    public RecycleStore() {
        this(null);
    }

    /**
     * @param user
     */
    public RecycleStore(ID user) {
        this.user = user == null ? Application.getCurrentUser() : user;
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
        data.add(new Object[] { recordId, s, with });
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
                recordName = recordId.toLiteral().toUpperCase();
            }

            clone.setID("recordId", recordId);
            clone.setString("recordName", recordName);
            clone.setString("recordContent", recordContent.toJSONString());
            if (o[2] != null) {
                clone.setID("channelWith", (ID) o[2]);
            }
            Application.getCommonService().create(clone);
            affected++;
        }

        return affected;
    }
}
