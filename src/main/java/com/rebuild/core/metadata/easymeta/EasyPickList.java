/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.configuration.general.PickListManager;
import com.rebuild.core.metadata.impl.FieldExtConfigProps;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.state.StateHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author devezhao
 * @since 2020/11/17
 */
@Slf4j
public class EasyPickList extends EasyField {
    private static final long serialVersionUID = 5971173892145230003L;

    protected EasyPickList(Field field, DisplayType displayType) {
        super(field, displayType);
    }

    @Override
    public Object convertCompatibleValue(Object value, EasyField targetField) {
        final String text = (String) wrapValue(value);

        DisplayType targetType = targetField.getDisplayType();
        boolean is2Text = targetType == DisplayType.TEXT || targetType == DisplayType.NTEXT;
        if (is2Text) {
            return text;
        }

        ID itemId = PickListManager.instance.findItemByLabel(text, targetField.getRawMeta());
        if (itemId == null) {
            log.warn("Cannot find value in PickList : " + text + " << " + targetField);
        }
        return itemId;
    }

    @Override
    public Object wrapValue(Object value) {
        String stateClass = getExtraAttr(FieldExtConfigProps.STATE_STATECLASS);
        return Language.L(StateHelper.valueOf(stateClass, (Integer) value));
    }
}
