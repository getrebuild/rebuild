/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * for Role
 *
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Service
public class RoleService extends BaseServiceImpl implements AdminGuard {

    /**
     * 管理员权限
     */
    public static final ID ADMIN_ROLE = ID.valueOf("003-0000000000000001");

    protected RoleService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Role;
    }

    @Override
    public Record create(Record record) {
        record = super.create(record);
        Application.getUserStore().refreshRole(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(record);
        Application.getUserStore().refreshRole(record.getPrimary());
        return record;
    }

    @Override
    public int delete(ID roleId) {
        deleteAndTransfer(roleId, null);
        return 1;
    }

    /**
     * TODO 删除后转移成员到其他角色
     *
     * @param roleId
     * @param transferTo
     */
    public void deleteAndTransfer(ID roleId, ID transferTo) {
        if (roleId.equals(ADMIN_ROLE)) {
            throw new OperationDeniedException("SUPER ROLE");
        }

        super.delete(roleId);
        Application.getUserStore().removeRole(roleId, transferTo);
    }

    /**
     * @param roleId
     * @param definition
     */
    public void updatePrivileges(ID roleId, JSONObject definition) {
        final ID user = Application.getCurrentUser();

        Object[][] array = Application.createQuery(
                "select privilegesId,definition,entity,zeroKey from RolePrivileges where roleId = ?")
                .setParameter(1, roleId)
                .array();
        Map<String, Object[]> existsPrivileges = new HashMap<>();
        for (Object[] o : array) {
            if ((int) o[2] == 0) {
                o[2] = o[3];
            }
            existsPrivileges.put(o[2].toString(), o);
        }

        JSONObject entityPrivileges = definition.getJSONObject("entity");
        JSONObject zeroPrivileges = definition.getJSONObject("zero");
        JSONObject allPrivileges = new JSONObject();
        allPrivileges.putAll(entityPrivileges);
        allPrivileges.putAll(zeroPrivileges);
        zeroPrivileges.clear();

        for (Map.Entry<String, Object> e : allPrivileges.entrySet()) {
            String name = e.getKey();
            String def = e.getValue().toString();
            if (existsPrivileges.containsKey(name)) {
                Object[] exists = existsPrivileges.get(name);
                // Unchanged
                if (def.equalsIgnoreCase(exists[1].toString())) {
                    continue;
                }

                Record privileges = EntityHelper.forUpdate((ID) exists[0], user);
                privileges.setString("definition", def);
                super.update(privileges);
            } else {
                Record privileges = EntityHelper.forNew(EntityHelper.RolePrivileges, user);
                privileges.setID("roleId", roleId);
                if (entityPrivileges.containsKey(name)) {
                    privileges.setInt("entity", Integer.parseInt(name));
                } else {
                    privileges.setInt("entity", 0);
                    privileges.setString("zeroKey", name);
                }
                privileges.setString("definition", def);
                super.create(privileges);
            }
        }

        Application.getUserStore().refreshRole(roleId);
    }
}
