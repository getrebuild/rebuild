/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.persist4j.PersistManager;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * for Role
 *
 * @author Zixin (RB)
 * @since 08/03/2018
 */
@Service
public class RoleService extends BaseService implements AdminGuard {

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
            throw new OperationDeniedException("内置角色禁止删除");
        }

        super.delete(roleId);
        Application.getUserStore().removeRole(roleId, transferTo);
    }

    /**
     * @param roleId
     * @param definition
     */
    public void updatePrivileges(ID roleId, JSONObject definition) {
        final ID user = UserContextHolder.getUser();

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

    /**
     * @param fromRole
     * @param toRoles
     */
    public void updateWithCopyTo(ID fromRole, ID[] toRoles) {
        List<Record> fromPrivileges = Application.createQuery(
                "select definition,entity,zeroKey from RolePrivileges where roleId = ?")
                .setParameter(1, fromRole)
                .list();

        final PersistManager pm = getPersistManagerFactory().createPersistManager();

        for (ID to : toRoles) {
            if (fromRole.equals(to)) continue;

            // 1.清空
            String dsql = String.format("delete from `role_privileges` where `ROLE_ID` = '%s'", to);
            Application.getSqlExecutor().execute(dsql);

            // 2.复制
            for (Record p : fromPrivileges) {
                Record clone = p.clone();
                clone.setID("roleId", to);
                pm.save(clone);
            }

            // 3.刷新
            Application.getUserStore().refreshRole(to);
        }
    }
}
