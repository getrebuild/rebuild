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
import com.rebuild.api.RespBody;
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
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            path = "attachment".equals(path) ? path : "docs";
        }

        // 记住最后一次访问的文件类型
        ServletUtils.addCookie(response, CK_LASTPATH, path);

        return createModelAndView("/files/" + path);
    }

    @GetMapping("list-file")
    public RespBody listFile(HttpServletRequest request) {
        final ID user = getRequestUser(request);
        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 100);

        String sort = getParameter(request, "sort");
        String q = StringUtils.trim(getParameter(request, "q"));

        // 文件列表 Entity(code) or Folder(ID/ALL)
        String entry = getParameter(request, "entry");
        // 详情页 相关记录附件
        ID related = getIdParameter(request, "related");

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
            sqlWhere.add(String.format("fileName like '%%%s%%'", CommonsUtils.escapeSql(q)));
        }

        // 附件
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
                    List<String> s = new ArrayList<>();
                    for (Entity de : entityMeta.getDetialEntities()) {
                        s.add(String.format("belongEntity = %d", de.getEntityCode()));
                    }
                    sqlWhere.add("(" + StringUtils.join(s, " or ") + ")");
                } else {
                    sqlWhere.add("belongEntity = " + useEntity);
                }

            } else {
                // 查看全部实体
                if (UserHelper.isAdmin(user)) {
                    sqlWhere.add("belongEntity > 0");
                } else {
                    String entityWhere = "( belongEntity = " +
                            StringUtils.join(getAllowEntities(user, true), " or belongEntity = ") + " )";
                    sqlWhere.add(entityWhere);
                }
            }
        }
        // 文件
        else {
            sqlWhere.add("belongEntity = 0");

            // 可访问目录
            Set<ID> accessable = FilesHelper.getAccessableFolders(user);

            if (useFolder != null) {
                if (accessable.contains(useFolder)) {
                    Set<ID> fs = new HashSet<>();
                    fs.add(useFolder);
                    fs.addAll(FilesHelper.getChildFolders(useFolder));
                    // 移除不可访问的
                    fs.removeIf(id -> !accessable.contains(id));
                    sqlWhere.add(String.format(
                            "inFolder in ('%s')", StringUtils.join(fs, "','")));
                } else {
                    sqlWhere.add("(1=2)");
                }

            } else {
                if (accessable.isEmpty()) {
                    sqlWhere.add("inFolder is null");
                } else {
                    sqlWhere.add(String.format(
                            "(inFolder is null or inFolder in ('%s'))", StringUtils.join(accessable, "','")));
                }
            }
        }

        if (related != null) {
            sqlWhere.add(String.format("relatedRecord = '%s'", related));
        }

        String sql = "select attachmentId,filePath,fileType,fileSize,createdBy,modifiedOn,inFolder,relatedRecord,fileName,belongField from Attachment where (1=1) and (isDeleted <> 'T')";
        sql = sql.replace("(1=1)", StringUtils.join(sqlWhere.iterator(), " and "));
        if ("size".equals(sort)) {
            sql += " order by fileSize desc";
        } else if ("older".equals(sort)) {
            sql += " order by createdOn asc";
        } else if (sort != null && sort.startsWith("name:")) {
            sql += " order by fileName " + (sort.endsWith(":desc") ? "desc" : "asc");
        } else {
            sql += " order by modifiedOn desc";
        }

        Object[][] array;
        int nextOffset = -1;
        if (!UserHelper.isAdmin(user) && related == null && useEntity > 0) {
            int offset = getIntParameter(request, "offset", -1);
            Object[] result = queryAttachmentsWithPermission(sql, user, pageNo, pageSize, offset);
            array = (Object[][]) result[0];
            nextOffset = (int) result[1];
        } else {
            array = Application.createQueryNoFilter(sql)
                    .setLimit(pageSize, pageNo * pageSize - pageSize)
                    .array();
        }

        JSONArray files = new JSONArray();
        for (Object[] o : array) {
            JSONObject item = new JSONObject();
            item.put("id", o[0]);
            String fileName = (String) o[8];
            if (fileName == null) fileName = QiniuCloud.parseFileName((String) o[1]);
            item.put("fileName", fileName);
            item.put("fileType", o[2]);
            item.put("fileSize", FileUtils.byteCountToDisplaySize(ObjectUtils.toLong(o[3])));
            item.put("uploadBy", new Object[]{o[4], UserHelper.getName((ID) o[4])});
            item.put("uploadOn", I18nUtils.formatDate((Date) o[5]));
            item.put("inFolder", o[6]);

            ID relatedRecord = (ID) o[7];
            if (relatedRecord != null && MetadataHelper.containsEntity(relatedRecord.getEntityCode())) {
                Entity belongEntity = MetadataHelper.getEntity(relatedRecord.getEntityCode());
                item.put("relatedRecord", new Object[]{relatedRecord, EasyMetaFactory.getLabel(belongEntity)});
                item.put("belongField", o[9]);
            }

            files.add(item);
        }

        RespBody res = RespBody.ok(files);
        if (nextOffset >= 0) res.putExtra("next_offset", nextOffset);
        return res;
    }

    // 文档树（目录）
    @GetMapping("tree-folder")
    public JSON listFolder(HttpServletRequest request) {
        return FilesHelper.getAccessableFolders(getRequestUser(request), null);
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
                new String[]{"id", "text", "icon"},
                new Object[]{entity.getEntityCode(), Language.L(entity), EasyMetaFactory.valueOf(entity).getIcon()});
    }

    private boolean hasAttachmentFields(Entity entity) {
        return MetadataSorter.sortFields(entity, DisplayType.FILE, DisplayType.IMAGE).length > 0;
    }

    // 带记录级权限过滤的附件查询
    // offset>=0 时从 offset 继续查（接续分页），否则从 0 查起收集 pageNo*pageSize 条后切片
    // 返回 Object[]{Object[][] result, int nextOffset}
    private Object[] queryAttachmentsWithPermission(String sql, ID user, int pageNo, int pageSize, int offset) {
        List<Object[]> filtered = new ArrayList<>();
        int queryOffset = Math.max(0, offset);
        final int batchSize = pageSize * 3;
        final int needed = offset >= 0 ? pageSize : pageNo * pageSize;
        int maxLoops = 50;
        int nextOffset = -1;

        outer:
        while (filtered.size() < needed && maxLoops-- > 0) {
            Object[][] batch = Application.createQueryNoFilter(sql)
                    .setLimit(batchSize, queryOffset)
                    .array();
            if (batch.length == 0) break;

            for (int i = 0; i < batch.length; i++) {
                ID relatedRecord = (ID) batch[i][7];
                if (relatedRecord != null && FilesHelper.isRecordReadable(relatedRecord, user)) {
                    filtered.add(batch[i]);
                    if (filtered.size() >= needed) {
                        nextOffset = queryOffset + i + 1;
                        break outer;
                    }
                }
            }

            queryOffset += batch.length;
            if (batch.length < batchSize) break;
        }

        if (nextOffset < 0) nextOffset = queryOffset;

        Object[][] result;
        if (offset >= 0) {
            result = filtered.toArray(new Object[0][]);
        } else {
            int start = (pageNo - 1) * pageSize;
            if (start >= filtered.size()) {
                result = new Object[0][];
            } else {
                int end = Math.min(start + pageSize, filtered.size());
                result = filtered.subList(start, end).toArray(new Object[0][]);
            }
        }

        return new Object[]{result, nextOffset};
    }
}
