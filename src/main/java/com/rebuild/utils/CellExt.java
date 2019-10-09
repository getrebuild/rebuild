/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.utils;

import cn.devezhao.commons.excel.Cell;

/**
 * 带行列索引的 Cell
 *
 * @author devezhao
 * @since 2019/9/30
 */
public class CellExt extends Cell {

    final private int rowNo;
    final private int columnNo;

    public CellExt(String cell, int rowNo, int columnNo) {
        super(cell);
        this.rowNo = rowNo;
        this.columnNo = columnNo;
    }

    public int getRowNo() {
        return rowNo;
    }

    public int getColumnNo() {
        return columnNo;
    }
}
