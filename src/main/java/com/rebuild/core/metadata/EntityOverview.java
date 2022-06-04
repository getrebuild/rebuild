/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.TransformManager;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author RB
 * @since 2022/6/3
 */
public class EntityOverview {

    private final Entity entity;

    public EntityOverview(Entity entity) {
        this.entity = entity;
    }

    public Map<String, Object> overview() {
        Map<String, Object> map = new HashMap<>();

        // FIELD

        List<Object> FIELDS = new ArrayList<>();
        for (Field field : entity.getFields()) {
            FIELDS.add(new String[] { field.getName(), EasyMetaFactory.getLabel(field) });
        }
        map.put("FIELDS", FIELDS);

        // AUTOFILLIN

        Object[][] array = Application.createQuery(
                "select sourceField from AutoFillinConfig where belongEntity = ?")
                .setParameter(1, entity.getName()).array();
        map.put("AUTOFILLINS", array);

        // APPROVAL

        array = Application.createQuery(
                "select configId,name from RobotApprovalConfig where belongEntity = ?")
                .setParameter(1, entity.getName()).array();
        map.put("APPROVALS", array);

        // TRANSFORM

        List<Object> TRANSFORMS = new ArrayList<>();
        for (ConfigBean cb : TransformManager.instance.getRawTransforms(entity.getName())) {
            TRANSFORMS.add(new Object[] { cb.getID("id"), cb.getString("name") });
        }
        map.put("TRANSFORMS", TRANSFORMS);

        // TRIGGER

        array = Application.createQuery(
                "select configId,name from RobotTriggerConfig where belongEntity = ?")
                .setParameter(1, entity.getName()).array();
        map.put("TRIGGERS", array);

        // EXTFORM

        array = Application.createQuery(
                "select configId,name from ExtformConfig where belongEntity = ?")
                .setParameter(1, entity.getName()).array();
        map.put("EXTFORMS", array);

        // REPORT

        array = Application.createQuery(
                "select configId,name from DataReportConfig where belongEntity = ?")
                .setParameter(1, entity.getName()).array();
        map.put("REPORTS", array);

        return map;
    }

    public Map<String, Object> graph() {
        Map<String, Object> map = new HashMap<>();

        List<Object> REFS = new ArrayList<>();
        for (Field field : MetadataSorter.sortFields(entity, DisplayType.REFERENCE, DisplayType.ANYREFERENCE)) {
            if (MetadataHelper.isCommonsField(field)) continue;

            String name = field.getName();
            String label = EasyMetaFactory.getLabel(field) + " (" + EasyMetaFactory.getLabel(field.getReferenceEntity()) + ")";
            REFS.add(new Object[] { name, label });
        }
        map.put("REFS", REFS);

        List<Object> REFTOS = new ArrayList<>();
        for (Field field : entity.getReferenceToFields()) {
            if (MetadataHelper.isCommonsField(field)) continue;
            if (!MetadataHelper.isBusinessEntity(field.getOwnEntity())) continue;

            String name = field.getOwnEntity().getName() + "." + field.getName();
            String label = EasyMetaFactory.getLabel(field.getOwnEntity()) + "." + EasyMetaFactory.getLabel(field);
            REFTOS.add(new Object[] { name, label });
        }
        map.put("REFTOS", REFTOS);

        return map;
    }
}
