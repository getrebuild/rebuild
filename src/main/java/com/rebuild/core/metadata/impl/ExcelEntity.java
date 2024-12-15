package com.rebuild.core.metadata.impl;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.RbAssert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * EXCEL 导入实体
 *
 * @author RB
 * @see CopyEntity
 * @since 2023/12/18
 */
@Slf4j
public class ExcelEntity extends Entity2Schema {

    /**
     * @param entityName
     * @param fields
     * @return
     */
    public String imports(String entityName, JSONArray fields) {
        final ID user = getUser();
        RbAssert.isSuperAdmin(user);

        // 1.实体
        String uniqueEntityName = createEntity(null, entityName, null, null, false, false);
        Entity entityNew = MetadataHelper.getEntity(uniqueEntityName);

        // 2.字段
        try {
            for (Object o : fields) {
                JSONArray item = (JSONArray) o;
                String name = item.getString(0);
                String type = item.getString(1);

                String refEntityOrClass = item.size() > 2 ? item.getString(2) : null;
                JSON extConfig = null;
                if ("CLASSIFICATION".equals(type)) {
                    Assert.notNull(refEntityOrClass, "[refEntityOrClass] cannot be null");
                    ID dataId = ID.valueOf(refEntityOrClass);
                    extConfig = JSONUtils.toJSONObject(EasyFieldConfigProps.CLASSIFICATION_USE, dataId);
                    refEntityOrClass = null;
                }
                
                createField(entityNew, name, DisplayType.valueOf(type), null, refEntityOrClass, extConfig);
            }

        } catch (Throwable ex) {
            // DROP
            dropEntity(entityNew, true);

            if (ex instanceof MetadataModificationException) {
                throw (MetadataModificationException) ex;
            }
            throw new MetadataModificationException(Language.L("无法导入实体 : %s", ex.getLocalizedMessage()));
        }

        return uniqueEntityName;
    }
}
