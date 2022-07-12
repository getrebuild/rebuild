/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.general.FieldValueHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author devezhao
 * @since 2020/11/17
 */
public class EasyReference extends EasyField implements MixValue {
    private static final long serialVersionUID = -5001745527956303569L;

    protected static final String VAR_CURRENT = "{CURRENT}";

    protected EasyReference(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return FieldValueHelper.getLabelNotry((ID) value);
        }

        if (targetType == DisplayType.N2NREFERENCE) {
            return new ID[] {(ID) value};
        }

        // ID
        return value;
    }

    @Override
    public Object exprDefaultValue() {
        String valueExpr = (String) getRawMeta().getDefaultValue();
        if (StringUtils.isBlank(valueExpr)) return null;

        if (valueExpr.contains(VAR_CURRENT)) {
            Object id = exprCurrent();
            if (id instanceof ID[]) return ((ID[]) id)[0];
            else return id;
        } else {
            return ID.isId(valueExpr) ? ID.valueOf(valueExpr) : null;
        }
    }

    /**
     * @see #VAR_CURRENT
     * @return returns ID or ID[]
     */
    protected Object exprCurrent() {
        final ID cu = UserContextHolder.getUser(true);
        if (cu == null) return null;

        Entity ref = getRawMeta().getReferenceEntity();
        if (ref.getEntityCode() == EntityHelper.User) return cu;

        User user = Application.getUserStore().getUser(cu);

        if (ref.getEntityCode() == EntityHelper.Department) {
            Department dept = user.getOwningDept();
            return dept == null ? null : dept.getIdentity();
        }

        if (ref.getEntityCode() == EntityHelper.Team) {
            List<ID> ts = new ArrayList<>();
            for (Team t : user.getOwningTeams()) {
                ts.add((ID) t.getIdentity());
            }
            return ts.isEmpty() ? null : ts.toArray(new ID[0]);
        }

        return null;
    }

    @Override
    public Object wrapValue(Object value) {
        ID idValue = (ID) value;
        Object text = idValue.getLabelRaw();
        if (text == null) {
            text = FieldValueHelper.getLabelNotry(idValue);
        }
        // 名称字段为引用字段，其引用实体的名称字段又是引用字段
        // 可能死循环
        else if (text instanceof ID) {
            text = FieldValueHelper.getLabelNotry((ID) text);
        }

        return FieldValueHelper.wrapMixValue(idValue, text == null ? null : text.toString());
    }

    @Override
    public JSON toJSON() {
        JSONObject map = (JSONObject) super.toJSON();

        Entity refEntity = getRawMeta().getReferenceEntity();
        map.put("ref", new String[] { refEntity.getName(),
                EasyMetaFactory.getDisplayType(refEntity.getNameField()).name() });
        return map;
    }
}
