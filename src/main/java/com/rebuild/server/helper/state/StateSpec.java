/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.state;

/**
 * 对开发友好的状态字段（State）规范。
 * 开发时可以更方便的使用状态值（明确的数字值），系统对此类型的字段提供了完整的支持。
 * 此字段与列表字段（PickList）表现相同。
 *
 * @author devezhao
 * @since 09/05/2019
 *
 * @see com.rebuild.server.metadata.entity.DisplayType#STATE
 * @see com.rebuild.server.business.approval.ApprovalState
 * @see HowtoState
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
