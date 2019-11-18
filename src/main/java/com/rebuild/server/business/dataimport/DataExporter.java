/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.dataimport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.datalist.DataList;
import com.rebuild.server.helper.datalist.DefaultDataList;

import java.io.File;

/**
 * 数据导出
 *
 * @author ZHAO
 * @since 2019/11/18
 * @see com.rebuild.server.helper.datalist.DefaultDataList
 */
public class DataExporter extends SetUser<DataExporter> {

    private JSONObject query;

    /**
     * @param query
     */
    public DataExporter(JSONObject query) {
        this.query = query;
    }

    /**
     * 导出
     *
     * @return
     */
    public File export() {
        File tmp = SysConfiguration.getFileOfTemp(String.format("导出-%d.xls", System.currentTimeMillis()));
        export(tmp);
        return tmp;
    }

    /**
     * 导出到指定文件
     *
     * @param dest
     */
    public void export(File dest) {
        DataList control = new DefaultDataList(query, getUser());
        JSON data = control.getJSONResult();

        System.out.println(data);
    }
}
