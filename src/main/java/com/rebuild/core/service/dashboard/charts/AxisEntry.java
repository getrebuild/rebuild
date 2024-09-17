/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.dashboard.charts;

import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * @author devezhao
 * @since 8/3/2024
 */
@ToString
public class AxisEntry {

    final private int index;
    final private Object[] key;
    final private Object value;

    public AxisEntry(Object[] rowValue, int index) {
        this.key = ArrayUtils.subarray(rowValue, 0, rowValue.length - 1);
        this.value = rowValue[rowValue.length - 1];
        this.index = index;
    }

    /**
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return
     */
    public Object[] getKeyRaw() {
        return key;
    }

    /**
     * @return
     */
    public String getKey() {
        return Arrays.toString(getKeyRaw());
    }

    /**
     * @return
     */
    public Object getValue() {
        return value;
    }
}
