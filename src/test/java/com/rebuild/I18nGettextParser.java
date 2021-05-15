/*
Copyright (c) Ruifang Tech <http://ruifang-tech.com/> and/or its owners. All rights reserved.
*/

package com.rebuild;

import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多语言提取
 * 1. .java 文件提取 `$L(xxx)` `errorl(xxx)` 注意不要换行
 *
 * @author devezhao
 * @since 2021/5/14
 */
@Slf4j
public class I18nGettextParser {

    public static void main(String[] args) throws IOException {
//        final File root = new File("D:\\GitHub\\rebuild\\rebuild-mob");
        final File root = new File("D:\\GitHub\\rebuild\\rebuild");

        Set<String> into = new TreeSet<>();
        parse(root, into);

        log.info("Found {} items", into.size());

        File target = new File(root, "lang.zh_CN.json");
        if (target.exists()) target.delete();

        JSONObject content = new JSONObject(true);
        content.put("_", "[中文]");
        for (String gettext : into) {
            content.put(gettext, String.format("[%s]", gettext));
        }

        FileUtils.writeStringToFile(target, JSONUtils.prettyPrint(content));
        log.info("File write : {}", target.getAbsolutePath());
    }

    static void parse(File fileOrDir, Set<String> into) throws IOException {
        String fileName = fileOrDir.getName();
        if (fileOrDir.isFile()) {
            if (fileName.endsWith(".js")) {
                into.addAll(parseJs(fileOrDir));
            } else if (fileName.endsWith(".html")) {
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
        Pattern pattern = Pattern.compile("\\$L\\('(.*?)'\\)");
        return parseWithPattern(file, pattern);
    }

    static List<String> parseHtml(File file) throws IOException {
        Pattern pattern = Pattern.compile("bundle\\.L\\('(.*?)'\\)");
        return parseWithPattern(file, pattern);
    }

    static List<String> parseJava(File file) throws IOException {
        Pattern pattern = Pattern.compile("\\$L\\(\"(.*?)\"[,)]");
        List<String> list = new ArrayList<>(parseWithPattern(file, pattern));

        pattern = Pattern.compile("errorl\\(\"(.*?)\"[,)]");
        list.addAll(parseWithPattern(file, pattern));

        return list;
    }

    static List<String> parseWithPattern(File file, Pattern pattern) throws IOException {
        String content = FileUtils.readFileToString(file, "utf-8");
        Matcher matcher = pattern.matcher(content);

        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            String gettext = matcher.group(1);
            log.info("`{}` in `{}`", gettext, file.getName());
            list.add(gettext);
        }
        return list;
    }
}
