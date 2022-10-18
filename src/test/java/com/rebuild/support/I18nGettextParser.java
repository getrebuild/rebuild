/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.support;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.metadata.BaseMeta;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.easymeta.DisplayType;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.trigger.ActionType;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多语言提取
 * 1 .java 文件提取 `.L(xxx, [args])` `.errorl(xxx)` 注意不要换行
 * 2 .js 提取 `$L(xxx, [args])`
 * 3 .html 提取 `bundle.L(xxx, [args])`
 *
 * @author devezhao
 * @since 2021/5/14
 */
@SuppressWarnings("NewClassNamingConvention")
@Slf4j
public class I18nGettextParser extends TestSupport {

    public static final String ROOT = "D:\\GitHub\\rebuild\\rebuild";

    @Disabled
    @Test
    void runMain() throws Exception {
        main(null);
    }

    public static void main(String[] args) throws Exception {
        final File root = new File(ROOT);

        Set<String> into = new TreeSet<>();
        parse(root, into);
        // append rebuild-mob
        parse(new File("D:\\GitHub\\rebuild\\rebuild-mob"), into);

        log.info("Found {} items", into.size());
        // Bad text
        for (String text : into) {
            if (text.contains(",") || text.contains("'") || text.contains("\"")) System.err.println(text);
        }

        File target = new File(root, "lang.zh_CN.json");
        FileUtils.deleteQuietly(target);

        JSONObject contents = new JSONObject(true);
        sysDefined(contents);

        for (String text : into) {
            text = text.trim();
            if (contents.containsKey(text)) continue;
            contents.put(text, text);
        }

        FileUtils.writeStringToFile(target, JSONUtils.prettyPrint(contents), AppUtils.UTF8);
        log.info("File write : {} ({})", target.getAbsolutePath(), contents.size());
    }

    static void parse(File fileOrDir, Set<String> into) throws IOException {
        String fileName = fileOrDir.getName();
        if (fileOrDir.isFile()) {
            if (fileName.endsWith(".js")) {
                into.addAll(parseJs(fileOrDir));
            } else if (fileName.endsWith(".html")) {
                into.addAll(parseJs(fileOrDir));
                into.addAll(parseHtml(fileOrDir));
            } else if (fileName.endsWith(".java")) {
                into.addAll(parseJava(fileOrDir));
            }

        } else if (fileOrDir.isDirectory()) {
            if (fileName.equalsIgnoreCase("node_modules")
                    || fileName.equalsIgnoreCase("test")) {
                return;
            }

            for (File sub : Objects.requireNonNull(fileOrDir.listFiles())) {
                parse(sub, into);
            }
        }
    }

    static List<String> parseJs(File file) throws IOException {
        Pattern pattern = Pattern.compile("\\$L\\('(.*?)'[,)]");
        return parseWithPattern(file, pattern);
    }

    static List<String> parseHtml(File file) throws IOException {
        Pattern pattern = Pattern.compile("bundle\\.L\\('(.*?)'[,)]");
        return parseWithPattern(file, pattern);
    }

    static List<String> parseJava(File file) throws IOException {
        Pattern pattern = Pattern.compile("\\.L\\(\"(.*?)\"[,)]");
        List<String> list = new ArrayList<>(parseWithPattern(file, pattern));
        pattern = Pattern.compile("\\.errorl\\(\"(.*?)\"[,)]");
        list.addAll(parseWithPattern(file, pattern));
        return list;
    }

    static List<String> parseWithPattern(File file, Pattern pattern) throws IOException {
        String content = FileUtils.readFileToString(file, AppUtils.UTF8);
        Matcher matcher = pattern.matcher(content);

        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            String gettext = matcher.group(1);
            log.info("`{}` in {}", gettext, file.getName());
            list.add(gettext);
        }
        return list;
    }

    // 系统定义的

    static void sysDefined(JSONObject into) {
        into.put("_", "中文");

        for (DisplayType o : DisplayType.values()) into.put(o.getDisplayName(), o.getDisplayName());
        for (ActionType o : ActionType.values()) into.put(o.getDisplayName(), o.getDisplayName());
        for (ApprovalState s : ApprovalState.values()) into.put(s.getName(), s.getName());

        // 实体元数据

        for (Entity entity : Application.getPersistManagerFactory().getMetadataFactory().getEntities()) {
            if (!EasyMetaFactory.valueOf(entity).isBuiltin()) continue;
            sysDefinedMeta(entity, into);
            for (Field field : entity.getFields()) {
                if (!EasyMetaFactory.valueOf(field).isBuiltin()) continue;
                sysDefinedMeta(field, into);
            }
        }
        into.put("__", "__");
    }

    static void sysDefinedMeta(BaseMeta meta, JSONObject into) {
        String text = meta.getDescription();
        if (StringUtils.isNotBlank(text)) {
            text = text.split("\\(")[0].trim();
            if (StringUtils.isNotBlank(text)) into.put(text, text);
        }
    }
}
