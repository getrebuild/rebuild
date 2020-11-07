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
import com.rebuild.core.metadata.impl.EasyMeta;
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

    private static final String PLAIN_ENTITY_PRIVILEGES = "{C:true,D:true,U:true,R:true}";

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

        if (MetadataHelper.hasPrivilegesField(entityMeta)) {
            Privileges priv = Application.getPrivilegesManager().getPrivileges(user, entityMeta.getEntityCode());
            Permission[] actions = new Permission[]{
                    BizzPermission.CREATE,
                    BizzPermission.DELETE,
                    BizzPermission.UPDATE,
                    BizzPermission.READ,
                    BizzPermission.ASSIGN,
                    BizzPermission.SHARE,
            };
            Map<String, Boolean> actionMap = new HashMap<>();
            for (Permission act : actions) {
                actionMap.put(act.getName(), priv.allowed(act));
            }
            mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
        } else if (EasyMeta.valueOf(entityMeta).isPlainEntity()) {
            mv.getModel().put("entityPrivileges", PLAIN_ENTITY_PRIVILEGES);
        } else {
            mv.getModel().put("entityPrivileges", JSONUtils.EMPTY_OBJECT_STR);
        }
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
        Entity entity = MetadataHelper.getEntity(record.getEntityCode());
        putEntityMeta(mv, entity);

        // 使用主实体权限
        if (entity.getMainEntity() != null) {
            entity = entity.getMainEntity();
        }
        if (MetadataHelper.hasPrivilegesField(entity)) {
            Permission[] actions = new Permission[]{
                    BizzPermission.CREATE,
                    BizzPermission.DELETE,
                    BizzPermission.UPDATE,
                    BizzPermission.READ,
                    BizzPermission.ASSIGN,
                    BizzPermission.SHARE,
            };
            Map<String, Boolean> actionMap = new HashMap<>();
            for (Permission act : actions) {
                actionMap.put(act.getName(), Application.getPrivilegesManager().allow(user, record, act));
            }
            mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
        } else if (EasyMeta.valueOf(entity).isPlainEntity()) {
            mv.getModel().put("entityPrivileges", PLAIN_ENTITY_PRIVILEGES);
        } else {
            mv.getModel().put("entityPrivileges", JSONUtils.EMPTY_OBJECT_STR);
        }
        return mv;
    }

    /**
     * @param into
     * @param entity
     */
    protected void putEntityMeta(ModelAndView into, Entity entity) {
        EasyMeta easyMeta = EasyMeta.valueOf(entity);
        into.getModel().put("entityName", easyMeta.getName());
        into.getModel().put("entityLabel", easyMeta.getLabel());
        into.getModel().put("entityIcon", easyMeta.getIcon());

        EasyMeta main = null;
        EasyMeta detail = null;
        if (entity.getMainEntity() != null) {
            main = EasyMeta.valueOf(entity.getMainEntity());
            detail = EasyMeta.valueOf(entity);
        } else if (entity.getDetailEntity() != null) {
            main = EasyMeta.valueOf(entity);
            detail = EasyMeta.valueOf(entity.getDetailEntity());
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
