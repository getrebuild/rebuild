/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.utils.JSONUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 实体页面（列表、视图） Controller
 *
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class EntityController extends BaseController {

    private static final JSON PLAIN_ENTITY_PRIVILEGES = (JSON) JSON.parse("{C:true,D:true,U:true,R:true}");

    /**
     * @param page
     * @param entity
     * @param user
     * @return
     */
    protected ModelAndView createModelAndView(String page, String entity, ID user) {
        ModelAndView mv = createModelAndView(page);
        Entity entityMeta = MetadataHelper.getEntity(entity);
        putEntityMeta(mv, entityMeta);
        mv.getModel().put("entityPrivileges", buildEntityPrivileges(entityMeta, user));
        return mv;
    }

    /**
     * @param page
     * @param record
     * @param user
     * @return
     */
    protected ModelAndView createModelAndView(String page, ID record, ID user) {
        ModelAndView mv = createModelAndView(page);
        putEntityMeta(mv, MetadataHelper.getEntity(record.getEntityCode()));
        mv.getModel().put("entityPrivileges", buildEntityPrivileges(record, user));
        return mv;
    }

    /**
     * @param recordIdOrEntity
     * @param user
     * @return
     */
    protected JSON buildEntityPrivileges(Object recordIdOrEntity, ID user) {
        Entity useEntity;
        ID useRecordId = null;
        if (recordIdOrEntity instanceof Entity) {
            useEntity = (Entity) recordIdOrEntity;
        } else {
            useRecordId = (ID) recordIdOrEntity;
            useEntity = MetadataHelper.getEntity(useRecordId.getEntityCode());
        }

        // 使用主实体权限
        if (useEntity.getMainEntity() != null) useEntity = useEntity.getMainEntity();

        if (MetadataHelper.hasPrivilegesField(useEntity)) {
            Permission[] actions = new Permission[] {
                    BizzPermission.CREATE,
                    BizzPermission.DELETE,
                    BizzPermission.UPDATE,
                    BizzPermission.READ,
                    BizzPermission.ASSIGN,
                    BizzPermission.SHARE,
            };

            Map<String, Boolean> actionMap = new HashMap<>();
            if (useRecordId != null) {
                for (Permission a : actions) {
                    actionMap.put(a.getName(), Application.getPrivilegesManager().allow(user, useRecordId, a));
                }
            } else {
                Privileges priv = Application.getPrivilegesManager().getPrivileges(user, useEntity.getEntityCode());
                for (Permission a : actions) {
                    actionMap.put(a.getName(), priv.allowed(a));
                }
            }
            return (JSON) JSON.toJSON(actionMap);

        } else if (EasyMetaFactory.valueOf(useEntity).isPlainEntity()) {
            return PLAIN_ENTITY_PRIVILEGES;
        } else {
            return JSONUtils.EMPTY_OBJECT;
        }
    }

    /**
     * @param into
     * @param entity
     */
    protected void putEntityMeta(ModelAndView into, Entity entity) {
        EasyEntity easyMeta = EasyMetaFactory.valueOf(entity);
        into.getModel().put("entityName", easyMeta.getName());
        into.getModel().put("entityLabel", easyMeta.getLabel());
        into.getModel().put("entityIcon", easyMeta.getIcon());

        EasyEntity main = null;
        EasyEntity detail = null;
        if (entity.getMainEntity() != null) {
            main = EasyMetaFactory.valueOf(entity.getMainEntity());
            detail = EasyMetaFactory.valueOf(entity);
        } else if (entity.getDetailEntity() != null) {
            main = EasyMetaFactory.valueOf(entity);
            detail = EasyMetaFactory.valueOf(entity.getDetailEntity());
        } else {
            into.getModel().put("mainEntity", easyMeta.getName());
        }

        if (main != null) {
            into.getModel().put("mainEntity", main.getName());
            into.getModel().put("mainEntityLabel", main.getLabel());
            into.getModel().put("mainEntityIcon", main.getIcon());
            into.getModel().put("detailEntity", detail.getName());
            into.getModel().put("detailEntityLabel", detail.getLabel());
            into.getModel().put("detailEntityIcon", detail.getIcon());
        }
    }
}
