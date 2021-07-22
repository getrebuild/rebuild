/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.metadata.easymeta;

import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.metadata.impl.EasyFieldConfigProps;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.JSONable;
import org.apache.commons.lang.StringUtils;

/**
 * 元数据封装
 *
 * @author devezhao
 * @since 2020/11/17
 */
public abstract class BaseEasyMeta<T extends BaseMeta> implements BaseMeta, JSONable {
    private static final long serialVersionUID = 6088391941883022085L;

    private final T baseMeta;

    protected BaseEasyMeta(T baseMeta) {
        this.baseMeta = baseMeta;
    }

    @Override
    public String getName() {
        return baseMeta.getName();
    }

    @Override
    public String getPhysicalName() {
        return baseMeta.getPhysicalName();
    }

    /**
     * @return
     * @deprecated Use {@link #getLabel()}
     */
    @Deprecated
    @Override
    public String getDescription() {
        return baseMeta.getDescription();
    }

    @Override
    public boolean isCreatable() {
        return baseMeta.isCreatable();
    }

    @Override
    public boolean isUpdatable() {
        return baseMeta.isUpdatable();
    }

    // 允许使用
    @Override
    public boolean isQueryable() {
        return baseMeta.isQueryable();
    }

    @Override
    public JSONObject getExtraAttrs() {
        JSONObject clone = baseMeta.getExtraAttrs() == null ? JSONUtils.EMPTY_OBJECT : baseMeta.getExtraAttrs();
        return (JSONObject) JSONUtils.clone(clone);
    }

    /**
     * @param clearSystem
     * @return
     */
    public JSONObject getExtraAttrs(boolean clearSystem) {
        // see DynamicMetadataFactory
        if (clearSystem) {
            JSONObject clone = getExtraAttrs();
            clone.remove("metaId");
            clone.remove("comments");
            clone.remove("icon");
            clone.remove("displayType");
            return clone;
        }
        return getExtraAttrs();
    }

    /**
     * 获取扩展属性
     *
     * @param name
     * @return
     * @see EasyFieldConfigProps
     */
    public String getExtraAttr(String name) {
        return getExtraAttrs(false).getString(name);
    }

    /**
     * 自定义实体/字段 ID
     *
     * @return
     */
    public ID getMetaId() {
        String metaId = getExtraAttr("metaId");
        return metaId == null ? null : ID.valueOf(metaId);
    }

    /**
     * 系统内置字段/实体，不可更改
     *
     * @return
     */
    public boolean isBuiltin() {
        return this.getMetaId() == null;
    }

    /**
     * @return
     * @see com.rebuild.core.support.i18n.Language#L(BaseMeta)
     */
    public String getLabel() {
        return Language.L(getRawMeta());
    }

    /**
     * 取代 persist4j 中的 description，而 persist4j 中的 description 则表示 label
     *
     * @return
     */
    public String getComments() {
        String comments = getExtraAttr("comments");
        if (getMetaId() != null) {
            return comments;
        }
        return StringUtils.defaultIfBlank(comments, Language.L("系统内置"));
    }

    /**
     * @return
     */
    public T getRawMeta() {
        return baseMeta;
    }

    @Override
    public String toString() {
        return "EASY#" + baseMeta.toString();
    }
}
