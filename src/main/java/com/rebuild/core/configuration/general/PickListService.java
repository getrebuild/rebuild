/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.configuration.general;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.configuration.BaseConfigurationService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.metadata.impl.DisplayType;
import com.rebuild.core.metadata.impl.EasyMeta;
import com.rebuild.core.privileges.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * 下拉列表
 *
 * @author zhaofang123@gmail.com
 * @since 09/07/2018
 */
@Service
public class PickListService extends BaseConfigurationService implements AdminGuard {

    protected PickListService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.PickList;
    }

    /**
     * 保存配置
     *
     * @param field
     * @param config
     */
    public void updateBatch(Field field, JSONObject config) {
        Assert.notNull(config, "[config] cannot be null");
        ID user = UserContextHolder.getUser();

        JSONArray showItems = config.getJSONArray("show");
        JSONArray hideItems = config.getJSONArray("hide");

        Object[][] itemsHold = Application.createQueryNoFilter(
                "select itemId from PickList where belongEntity = ? and belongField = ?")
                .setParameter(1, field.getOwnEntity().getName())
                .setParameter(2, field.getName())
                .array();
        Set<ID> itemsHoldList = new HashSet<>();
        for (Object[] o : itemsHold) {
            itemsHoldList.add((ID) o[0]);
        }

        if (hideItems != null) {
            for (Object o : hideItems) {
                JSONObject item = (JSONObject) o;
                String id = item.getString("id");
                if (!ID.isId(id)) {
                    continue;
                }

                ID id2id = ID.valueOf(id);
                Record r = EntityHelper.forUpdate(id2id, user);
                r.setBoolean("isHide", true);
                r.setString("text", item.getString("text"));
                super.update(r);
                itemsHoldList.remove(id2id);
                cleanCache(id2id);
            }
        }

        // MultiSelect 专用
        long nextMaskValue = 0;
        if (EasyMetaFactory.getDisplayType(field) == DisplayType.MULTISELECT) {
            Object[] max = Application.createQueryNoFilter(
                    "select max(maskValue) from PickList where belongEntity = ? and belongField = ?")
                    .setParameter(1, field.getOwnEntity().getName())
                    .setParameter(2, field.getName())
                    .unique();
            nextMaskValue = ObjectUtils.toLong(max[0]);
            if (nextMaskValue < 1) {
                nextMaskValue = 1;
            } else {
                nextMaskValue *= 2;
            }
        }

        int seq = 0;
        for (Object o : showItems) {
            JSONObject item = (JSONObject) o;
            String id = item.getString("id");
            ID id2id = ID.isId(id) ? ID.valueOf(id) : null;

            Record r = id2id == null
                    ? EntityHelper.forNew(EntityHelper.PickList, user) : EntityHelper.forUpdate(id2id, user);
            r.setInt("seq", seq++);
            r.setString("text", item.getString("text"));
            r.setBoolean("isHide", false);
            r.setBoolean("isDefault", item.getBoolean("default"));
            if (id2id == null) {
                r.setString("belongEntity", field.getOwnEntity().getName());
                r.setString("belongField", field.getName());

                if (nextMaskValue > 0) {
                    r.setLong("maskValue", nextMaskValue);
                    nextMaskValue *= 2;
                }
            }
            super.createOrUpdate(r);

            if (id2id != null) {
                itemsHoldList.remove(id2id);
                cleanCache(id2id);
            }
        }

        for (ID item : itemsHoldList) {
            cleanCache(item);
            super.delete(item);
        }
        PickListManager.instance.clean(field);
    }

    @Override
    protected void cleanCache(ID cfgid) {
        PickListManager.instance.clean(cfgid);
    }
}
