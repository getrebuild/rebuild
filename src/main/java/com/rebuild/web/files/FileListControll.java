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

package com.rebuild.web.files;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.files.FilesHelper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.DisplayType;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author devezhao
 * @since 2019/11/12
 */
@Controller
@RequestMapping("/files/")
public class FileListControll extends BasePageControll {

    private static final String CK_LASTPATH = "rb.lastFilesPath";

    @RequestMapping({ "home", "attachment", "docs" })
    public ModelAndView pageIndex(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI();
        if (path.contains("/files/docs")) {
            path = "docs";
        } else if (path.contains("/files/attachment")) {
            path = "attachment";
        } else {
            path = ServletUtils.readCookie(request,CK_LASTPATH);
            if (path == null) {
                path = "attachment";
            }
        }
        // 记住最后一次访问的文件类型
        ServletUtils.addCookie(response, CK_LASTPATH, path);

        return createModelAndView("/files/" + path + ".jsp");
    }

    @RequestMapping("list-file")
    public void listFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);
        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 100);

        String sort = getParameter(request, "sort");
        String q = getParameter(request, "q");

        // Entity or Folder
        String entry = getParameter(request, "entry");
        int entity = 0;
        ID inFolder = null;
        if (NumberUtils.isNumber(entry)) {
            entity = NumberUtils.toInt(entry);
        } else if (ID.isId(entry)) {
            inFolder = ID.valueOf(entry);
        }

        List<String> sqlWhere = new ArrayList<>();
        if (StringUtils.isNotBlank(q)) {
            sqlWhere.add(String.format("filePath like '%%%s%%'", StringEscapeUtils.escapeSql(q)));
        }

        // 附件还是文档
        if (entity > 0) {
            if (entity == EntityHelper.Feeds) {
                sqlWhere.add(String.format("(belongEntity = %d or belongEntity = %d)", entity, EntityHelper.FeedsComment));
            } else if (entity > 1) {
                Entity entityMeta = MetadataHelper.getEntity(entity);
                if (entityMeta.getSlaveEntity() != null) {
                    sqlWhere.add(String.format("(belongEntity = %d or belongEntity = %d)", entity, entityMeta.getSlaveEntity().getEntityCode()));
                } else {
                    sqlWhere.add("belongEntity = " + entity);
                }
            } else {
                sqlWhere.add("belongEntity > 0");
            }
        } else {
            sqlWhere.add("belongEntity = 0");
            if (inFolder != null) {
                sqlWhere.add("inFolder = '" + inFolder + "'");
            } else {
                ID[] ps = FilesHelper.getPrivateFolders(user);
                if (ps.length > 0) {
                    sqlWhere.add(String.format("(inFolder is null or inFolder not in ('%s'))", StringUtils.join(ps,"','")));
                }
            }
        }

        String sql = "select attachmentId,filePath,fileType,fileSize,createdBy,modifiedOn,inFolder,relatedRecord from Attachment where (1=1)";
        sql = sql.replace("(1=1)", StringUtils.join(sqlWhere.iterator(), " and "));
        if ("older".equals(sort)) {
            sql += " order by createdOn asc";
        } else {
            sql += " order by modifiedOn desc";
        }
        System.out.println(sql);
        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        JSONArray files = new JSONArray();
        for (Object[] o : array) {
            JSONObject item = new JSONObject();
            item.put("id", o[0]);
            item.put("filePath", o[1]);
            item.put("fileType", o[2]);
            item.put("fileSize", FileUtils.byteCountToDisplaySize(ObjectUtils.toLong(o[3])));
            item.put("uploadBy", new Object[] { o[4], UserHelper.getName((ID) o[4]) });
            item.put("uploadOn", Moment.moment((Date) o[5]).fromNow());
            item.put("inFolder", o[6]);

            ID relatedRecord = (ID) o[7];
            if (relatedRecord != null && MetadataHelper.containsEntity(relatedRecord.getEntityCode())) {
                Entity belongEntity = MetadataHelper.getEntity(relatedRecord.getEntityCode());
                item.put("relatedRecord", new Object[] { relatedRecord, EasyMeta.getLabel(belongEntity) });
            }

            files.add(item);
        }
        writeSuccess(response, files);
    }

    // 文档目录
    @RequestMapping("list-folder")
    public void listFolder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSONArray folders = FilesHelper.getFolders(user);
        writeSuccess(response, folders);
    }

    // 附件实体
    @RequestMapping("list-entity")
    public void listEntity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);

        JSONArray ret = new JSONArray();
        // 动态
        ret.add(toEntityJson(MetadataHelper.getEntity(EntityHelper.Feeds)));
        for (Entity e : MetadataSorter.sortEntities(user)) {
            // 明细实体会合并到主实体显示
            if (MetadataHelper.isSlaveEntity(e.getEntityCode())) {
                continue;
            }

            // 有附件字段的实体才显示
            if (hasAttachmentFields(e)
                    || (e.getSlaveEntity() != null && hasAttachmentFields(e.getSlaveEntity()))) {
                ret.add(toEntityJson(e));
            }
        }
        writeSuccess(response, ret);
    }

    private JSONObject toEntityJson(Entity entity) {
        return JSONUtils.toJSONObject(
                new String[] { "id", "text" },
                new Object[] { entity.getEntityCode(), EasyMeta.getLabel(entity) });
    }

    private boolean hasAttachmentFields(Entity entity) {
        for (Field field : entity.getFields()) {
            DisplayType dt = EasyMeta.getDisplayType(field);
            if (dt == DisplayType.FILE || dt == DisplayType.IMAGE) return true;
        }
        return false;
    }
}
