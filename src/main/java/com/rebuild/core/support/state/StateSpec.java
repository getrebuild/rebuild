/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support.state;

/**
 * 对开发友好的状态字段（State）规范。
 * 开发时可以更方便的使用状态值（明确的数字值），系统对此类型的字段提供了完整的支持。
 * 此字段与列表字段（PickList）表现相同。
 *
 * @author devezhao
 * @see com.rebuild.core.metadata.impl.DisplayType#STATE
 * @see com.rebuild.core.service.approval.ApprovalState
 * @see HowtoState
 * @since 09/05/2019
 */
public interface StateSpec {

    /**
     * 实际值（数据库中的值）
     *
     * @return
     */
    int getState();

    /**
     * 显示值
     * 请使用多语言 {@link com.rebuild.core.support.i18n.Language#L(StateSpec)}
     *
     * @return
     */
    String getName();

    /**
     * 是否默认值
     *
     * @return
     */
    boolean isDefault();
}
