/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.files;

import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.MetadataSorter;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.files.FilesHelper;
import com.rebuild.core.service.project.ProjectManager;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author devezhao
 * @since 2019/11/12
 */
@RestController
@RequestMapping("/files/")
public class FileListController extends BaseController {

    private static final String CK_LASTPATH = "rb.lastFilesPath";

    @GetMapping({"home", "attachment", "docs"})
    public ModelAndView pageIndex(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        if (path.contains("/files/docs")) {
            path = "docs";
        } else if (path.contains("/files/attachment")) {
            path = "attachment";
        } else {
            path = ServletUtils.readCookie(request, CK_LASTPATH);
            if (path == null) {
                path = "attachment";
            }
        }
        // 记住最后一次访问的文件类型
        ServletUtils.addCookie(response, CK_LASTPATH, path);

        return createModelAndView("/files/" + path);
    }

    @GetMapping("list-file")
    public JSON listFile(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 100);

        String sort = getParameter(request, "sort");
        String q = getParameter(request, "q");
        // 从相关记录
        ID related = getIdParameter(request, "related");

        // Entity or Folder
        String entry = getParameter(request, "entry");

        int useEntity = 0;
        ID useFolder = null;

        if (NumberUtils.isNumber(entry)) {
            useEntity = NumberUtils.toInt(entry);
        } else if (ID.isId(entry)) {
            useFolder = ID.valueOf(entry);
        }

        // 支持查询 ID
        if (related == null && ID.isId(q) && useEntity > 0) {
            related = ID.valueOf(q);
            q = null;
        }

        List<String> sqlWhere = new ArrayList<>();
        if (StringUtils.isNotBlank(q)) {
            sqlWhere.add(String.format("filePath like '%%%s%%'", StringEscapeUtils.escapeSql(q)));
        }

        // 附件还是文档
        if (useEntity > 0) {
            if (useEntity == EntityHelper.Feeds) {
                sqlWhere.add(String.format(
                        "(belongEntity = %d or belongEntity = %d)", useEntity, EntityHelper.FeedsComment));
            } else if (useEntity == EntityHelper.ProjectTask) {
                sqlWhere.add(String.format(
                        "(belongEntity = %d or belongEntity = %d)", useEntity, EntityHelper.ProjectTaskComment));
            } else if (useEntity > 1) {
                Entity entityMeta = MetadataHelper.getEntity(useEntity);
                if (entityMeta.getDetailEntity() != null) {
                    sqlWhere.add(String.format(
                            "(belongEntity = %d or belongEntity = %d)", useEntity, entityMeta.getDetailEntity().getEntityCode()));
                } else {
                    sqlWhere.add("belongEntity = " + useEntity);
                }

            } else {
                // 查看全部实体
                if (UserHelper.isAdmin(user)) {
                    sqlWhere.add("belongEntity > 0");
                } else {
                    String esql = "( belongEntity = " +
                            StringUtils.join(getAllowEntities(user, true), " or belongEntity = ") + " )";
                    sqlWhere.add(esql);
                }
            }

        } else {
            sqlWhere.add("belongEntity = 0");

            Set<ID> ps = FilesHelper.getPrivateFolders(user);

            if (useFolder != null) {
                Set<ID> fs = new HashSet<>();
                fs.add(useFolder);
                fs.addAll(FilesHelper.getChildFolders(useFolder));
                Collection<ID> fs2 = CollectionUtils.removeAll(fs, ps);

                sqlWhere.add(String.format(
                        "inFolder in ('%s')", StringUtils.join(fs2, "','")));

            } else if (!ps.isEmpty()) {
                sqlWhere.add(String.format(
                        "(inFolder is null or inFolder not in ('%s'))", StringUtils.join(ps, "','")));
            }
        }

        if (related != null) {
            sqlWhere.add(String.format("relatedRecord = '%s'", related));
        }

        String sql = "select attachmentId,filePath,fileType,fileSize,createdBy,modifiedOn,inFolder,relatedRecord" +
                " from Attachment where (1=1) and (isDeleted = ?)";
        sql = sql.replace("(1=1)", StringUtils.join(sqlWhere.iterator(), " and "));
        if ("older".equals(sort)) {
            sql += " order by createdOn asc";
        } else {
            sql += " order by modifiedOn desc";
        }

        Object[][] array = Application.createQueryNoFilter(sql)
                .setParameter(1, false)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        JSONArray files = new JSONArray();
        for (Object[] o : array) {
            JSONObject item = new JSONObject();
            item.put("id", o[0]);
            item.put("filePath", o[1]);
            item.put("fileType", o[2]);
            item.put("fileSize", FileUtils.byteCountToDisplaySize(ObjectUtils.toLong(o[3])));
            item.put("uploadBy", new Object[]{o[4], UserHelper.getName((ID) o[4])});
            item.put("uploadOn", I18nUtils.formatDate((Date) o[5]));
            item.put("inFolder", o[6]);

            ID relatedRecord = (ID) o[7];
            if (relatedRecord != null && MetadataHelper.containsEntity(relatedRecord.getEntityCode())) {
                Entity belongEntity = MetadataHelper.getEntity(relatedRecord.getEntityCode());
                item.put("relatedRecord", new Object[]{relatedRecord, EasyMetaFactory.getLabel(belongEntity)});
            }

            files.add(item);
        }
        return files;
    }

    // 文档树（目录）
    @GetMapping("tree-folder")
    public JSON listFolder(HttpServletRequest request) {
        return FilesHelper.getFolders(getRequestUser(request), null);
    }

    // 实体树
    @GetMapping("tree-entity")
    public JSON listEntity(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSONArray ret = new JSONArray();
        for (int entity : getAllowEntities(user, false)) {
            ret.add(formatEntityJson(MetadataHelper.getEntity(entity)));
        }
        return ret;
    }

    private Integer[] getAllowEntities(ID user, boolean inQuery) {
        List<Integer> allows = new ArrayList<>();

        // 动态
        allows.add(EntityHelper.Feeds);
        if (inQuery) allows.add(EntityHelper.FeedsComment);

        // 项目
        if (ProjectManager.instance.getAvailable(user).length > 0) {
            allows.add(EntityHelper.ProjectTask);
            if (inQuery) allows.add(EntityHelper.ProjectTaskComment);
        }

        for (Entity e : MetadataSorter.sortEntities(user, false, false)) {
            // 有附件字段的实体才显示
            if (hasAttachmentFields(e)) {
                allows.add(e.getEntityCode());
            }
            if (e.getDetailEntity() != null && hasAttachmentFields(e.getDetailEntity())) {
                allows.add(e.getDetailEntity().getEntityCode());
            }
        }
        return allows.toArray(new Integer[0]);
    }

    private JSONObject formatEntityJson(Entity entity) {
        return JSONUtils.toJSONObject(
                new String[] { "id", "text" },
                new Object[] { entity.getEntityCode(), Language.L(entity) });
    }

    private boolean hasAttachmentFields(Entity entity) {
        return MetadataSorter.sortFields(entity, DisplayType.FILE, DisplayType.IMAGE).length > 0;
    }
}
