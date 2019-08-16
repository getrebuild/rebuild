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

package com.rebuild.server.business.datareport;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.metadata.Sheet;
import com.rebuild.server.RebuildException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 提取模板中的变量 ${foo} > foo
 *
 * @author devezhao
 * @since 2019/8/16
 */
public class ExtractTemplateVars {

    private File template;

    public ExtractTemplateVars(File template) {
        this.template = template;
    }

    /**
     * @return
     */
    public Set<String> extract() {
        List<Object> rows = null;
        try (InputStream is = new FileInputStream(this.template)) {
            rows = EasyExcelFactory.read(is, new Sheet(1, 0));
        } catch (IOException ex) {
            throw new RebuildException(ex);
        }

        Set<String> vars = new HashSet<>();
        for (Object row : rows) {
            List<?> list = (List<?>) row;
            for (Object cell : list) {
                if (cell != null && cell.toString().matches("\\$\\{\\w+\\}")) {
                    String cellVar = cell.toString();
                    cellVar = cellVar.substring(2, cellVar.length() - 1);
                    vars.add(cellVar);
                }
            }
        }
        return vars;
    }
}
