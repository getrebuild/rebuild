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

package com.rebuild.web.base.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.dataimport.DataExporter;
import com.rebuild.server.helper.datalist.BatchOperatorQuery;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author ZHAO
 * @since 2019/11/18
 */
@Controller
public class DataExportControll extends BaseControll {

    @RequestMapping("/app/{entity}/data-export/submit")
    public void export(@PathVariable String entity,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        Assert.isTrue(Application.getSecurityManager().allow(user, ZeroEntry.AllowDataExport), "没有权限");

        int dataRange = getIntParameter(request, "dr", 2);
        JSONObject queryData = (JSONObject) ServletUtils.getRequestJson(request);
        queryData = new BatchOperatorQuery(dataRange, queryData).wrapQueryData(DataExporter.MAX_ROWS);

        try {
            File file = new DataExporter(queryData).setUser(user).export();
            writeSuccess(response, file.getName());
        } catch (Exception ex) {
            writeFailure(response, ex.getLocalizedMessage());
        }
    }
}
