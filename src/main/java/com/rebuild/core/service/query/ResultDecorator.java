/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.query;

import cn.devezhao.persist4j.dialect.FieldType;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.persist4j.query.AjqlQuery;
import cn.devezhao.persist4j.query.AjqlResultImpl;
import cn.devezhao.persist4j.query.compiler.SelectItem;
import cn.devezhao.persist4j.query.compiler.SelectItemType;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.general.N2NReferenceSupport;
import com.rebuild.core.support.general.TagSupport;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 1. 自动查询 N2N 值列表（必须有主键字段）
 * 2. 自动查询 TAG 值列表（必须有主键字段）
 *
 * @author RB
 * @since 2022/06/21
 */
@Slf4j
public class ResultDecorator extends AjqlResultImpl {
    private static final long serialVersionUID = 5721780167663489323L;

    private int pkIndex = -1;
    private Map<Integer, String> N2NIndexes = new HashMap<>();
    private Map<Integer, String> TagIndexes = new HashMap<>();

    protected ResultDecorator(AjqlQuery query) {
        super(query);

        String pk = query.getRootEntity().getPrimaryField().getName();
        for (SelectItem item : query.getSelectItems()) {
            if (item.getType() != SelectItemType.Field) continue;

            if (pk.equalsIgnoreCase(item.getFieldPath())) {
                this.pkIndex = item.getIndex();
            } else if (item.getField().getType() == FieldType.REFERENCE_LIST) {
                this.N2NIndexes.put(item.getIndex(), item.getFieldPath());
            } else if (EasyMetaFactory.getDisplayType(item.getField()) == DisplayType.TAG) {
                this.TagIndexes.put(item.getIndex(), item.getFieldPath());
            }
        }

        if (pkIndex == -1 && N2NIndexes.size() + TagIndexes.size() > 0) {
            log.warn("No field of primary in select items, cannot provide n2n-value or tag-value");
        }
    }

    @Override
    protected Object[] readRow(SelectItem[] selectItems, ResultSet rs) throws SQLException {
        Object[] row = super.readRow(selectItems, rs);
        if (pkIndex == -1) return row;

        final ID pkValue = (ID) row[pkIndex];

        for (Map.Entry<Integer, String> e : N2NIndexes.entrySet()) {
            ID[] hasValue = (ID[]) row[e.getKey()];
            if (hasValue != null && hasValue.length > 0) {
                row[e.getKey()] = N2NReferenceSupport.items(e.getValue(), pkValue);
            }
        }

        for (Map.Entry<Integer, String> e : TagIndexes.entrySet()) {
            String hasValue = (String) row[e.getKey()];
            if (hasValue != null) {
                row[e.getKey()] = TagSupport.items(e.getValue(), pkValue);
            }
        }

        return row;
    }
}
