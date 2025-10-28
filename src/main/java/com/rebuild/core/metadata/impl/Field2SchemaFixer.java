/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.impl;

import cn.devezhao.commons.ThrowableUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.util.support.Table;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyField;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.support.general.RecordBuilder;
import com.rebuild.core.support.i18n.Language;
import lombok.extern.slf4j.Slf4j;

import java.sql.DataTruncation;

import static com.rebuild.core.metadata.impl.EasyFieldConfigProps.DATETIME_FORMAT;
import static com.rebuild.core.metadata.impl.EasyFieldConfigProps.DATE_FORMAT;
import static com.rebuild.core.metadata.impl.EasyFieldConfigProps.NUMBER_CALCFORMULA;
import static com.rebuild.core.metadata.impl.EasyFieldConfigProps.NUMBER_CALCFORMULABACKEND;
import static com.rebuild.core.metadata.impl.EasyFieldConfigProps.NUMBER_NOTNEGATIVE;

/**
 * @author Zixin
 * @since 2025/6/23
 */
@Slf4j
public class Field2SchemaFixer extends Field2Schema {

    /**
     * 类型转换
     *
     * @param field
     * @param toType
     * @param force
     * @return
     */
    public boolean castType(Field field, DisplayType toType, boolean force) {
        EasyField fieldEasy = EasyMetaFactory.valueOf(field);
        ID metaRecordId = fieldEasy.getMetaId();
        if (fieldEasy.isBuiltin() || metaRecordId == null) {
            throw new MetadataModificationException(Language.L("系统内置，不允许转换"));
        }

        if (!force) {
            long count;
            if ((count = checkRecordCount(field.getOwnEntity())) > 1000000) {
                throw new MetadataModificationException(Language.L("实体记录过多 (%d)，转换字段可能导致表损坏", count));
            }
        }

        Record fieldMeta = EntityHelper.forUpdate(metaRecordId, getUser(), false);
        fieldMeta.setString("displayType", toType.name());
        // 长度
        if (toType.getMaxLength() != FieldType.NO_NEED_LENGTH) {
            fieldMeta.setInt("maxLength", toType.getMaxLength());
        }
        // 保留部分扩展配置，其余移除避免冲突
        JSONObject extraAttrs = fieldEasy.getExtraAttrs();
        if (!extraAttrs.isEmpty()) {
            extraAttrs.remove(DATE_FORMAT);
            extraAttrs.remove(DATETIME_FORMAT);
            Object notNegative = extraAttrs.remove(NUMBER_NOTNEGATIVE);
            Object calcFormula = extraAttrs.remove(NUMBER_CALCFORMULA);
            Object calcFormulaBackend = extraAttrs.remove(NUMBER_CALCFORMULABACKEND);

            extraAttrs.clear();
            if (notNegative != null) extraAttrs.put(NUMBER_NOTNEGATIVE, notNegative);
            if (calcFormula != null) extraAttrs.put(NUMBER_CALCFORMULA, calcFormula);
            if (calcFormulaBackend instanceof Boolean && (Boolean) calcFormulaBackend) extraAttrs.put(NUMBER_CALCFORMULABACKEND, true);

            if (extraAttrs.isEmpty()) fieldMeta.setNull("extConfig");
            else fieldMeta.setString("extConfig", extraAttrs.toJSONString());
        }
        Application.getCommonsService().update(fieldMeta, false);

        // 类型生效
        DynamicMetadataContextHolder.setSkipLanguageRefresh();
        MetadataHelper.getMetadataFactory().refresh();
        field = MetadataHelper.getField(field.getOwnEntity().getName(), field.getName());

        // 去除默认值
        try {
            java.lang.reflect.Field defaultValueOfField = field.getClass().getDeclaredField("defaultValue");
            defaultValueOfField.setAccessible(true);
            defaultValueOfField.set(field, null);
            java.lang.reflect.Field nullableOfField = field.getClass().getDeclaredField("nullable");
            nullableOfField.setAccessible(true);
            nullableOfField.set(field, true);
        } catch (ReflectiveOperationException ignored) {
        }

        String alterTypeSql = null;
        try {
            Dialect dialect = Application.getPersistManagerFactory().getDialect();
            final Table table = new Table40(field.getOwnEntity(), dialect);
            StringBuilder ddl = new StringBuilder();
            table.generateFieldDDL(field, ddl);

            alterTypeSql = String.format("alter table `%s` change column `%s` ",
                    field.getOwnEntity().getPhysicalName(), field.getPhysicalName());
            alterTypeSql += ddl.toString().trim().replaceAll("\\s+", " ");

            Application.getSqlExecutor().executeBatch(new String[]{alterTypeSql}, DDL_TIMEOUT);
            log.info("Cast field type : {}", alterTypeSql);

        } catch (Throwable ex) {
            // 还原
            fieldMeta.setString("displayType", EasyMetaFactory.getDisplayType(field).name());
            Application.getCommonsService().update(fieldMeta, false);

            log.error("DDL ERROR : \n{}", alterTypeSql, ex);

            Throwable cause = ThrowableUtils.getRootCause(ex);
            String causeMsg = cause.getLocalizedMessage();
            if (cause instanceof DataTruncation) {
                causeMsg = Language.L("已有数据内容长度超出限制，无法完成转换");
            }
            throw new MetadataModificationException(causeMsg);

        } finally {
            MetadataHelper.getMetadataFactory().refresh();
            DynamicMetadataContextHolder.isSkipLanguageRefresh(true);
        }

        return true;
    }

    /**
     * @param field
     * @return
     */
    public boolean fixDatetime40(Field field) {
        if (field.getType() != FieldType.TIMESTAMP) return false;

        changeColumn(field);
        return true;
    }

    /**
     * @param field
     * @return
     */
    public boolean fixUploadNumber41(Field field) {
        EasyField em = EasyMetaFactory.valueOf(field);
        if (em.getMetaId() == null) return false;
        if (!(em.getDisplayType() == DisplayType.FILE || em.getDisplayType() == DisplayType.IMAGE)) return false;

        changeColumn(field);

        JSONObject attrs = em.getExtraAttrs();
        attrs.put("uploadNumber", "0,99");
        Record r = RecordBuilder.builder(em.getMetaId())
                .add("extConfig", attrs.toJSONString())
                .build(UserService.SYSTEM_USER);
        Application.getCommonsService().update(r, false);

        MetadataHelper.getMetadataFactory().refresh();
        return true;
    }

    /**
     * @param entity
     * @return
     */
    public boolean addSeqField(Entity entity) {
        if (entity.getMainEntity() == null) return false;
        if (entity.containsField(EntityHelper.Seq)) return false;

        Field seqField = createUnsafeField(entity, EntityHelper.Seq, "SEQ", DisplayType.NUMBER,
                true, false, false, true, false, null, null, null, null, null);
        schema2Database(entity, new Field[]{seqField});
        return true;
    }

    /**
     * 修改数据库行
     *
     * @param field
     */
    private void changeColumn(Field field) {
        Dialect dialect = Application.getPersistManagerFactory().getDialect();
        final Table table = new Table40(field.getOwnEntity(), dialect);
        StringBuilder ddl = new StringBuilder();
        table.generateFieldDDL(field, ddl);

        String dmlSql = String.format("alter table `%s` change column `%s` ",
                field.getOwnEntity().getPhysicalName(), field.getPhysicalName());
        dmlSql += ddl.toString().trim().replaceAll("\\s+", " ");
        // v4.2
        dmlSql = dmlSql.replace(" varchar(700) ", " text ");

        Application.getSqlExecutor().executeBatch(new String[]{dmlSql}, DDL_TIMEOUT);
        log.info("Fixed column of field : {}", dmlSql);
    }
}
