/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin;

import cn.devezhao.commons.web.ServletUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.support.CommandArgs;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.SysbaseHeartbeat;
import com.rebuild.utils.RbAssert;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.FileDownloader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 日志查看器
 *
 * @author rebuild
 * @since 2026/8/3
 */
@Slf4j
@RestController
@RequestMapping("/admin/")
public class AdminAssistController extends BaseController {

    @GetMapping("admin-cli")
    public ModelAndView adminCliConsole(HttpServletRequest request) {
        RbAssert.isSuperAdmin(getRequestUser(request));
        return createModelAndView("/admin/admin-cli");
    }

    @RequestMapping("admin-cli/exec")
    public RespBody adminCliExec(HttpServletRequest request) {
        RbAssert.isSuperAdmin(getRequestUser(request));

        String command = ServletUtils.getRequestString(request);
        if (org.apache.commons.lang.StringUtils.isBlank(command)) return RespBody.error();

        String res = new AdminCli4(command).exec();
        return RespBody.ok(res);
    }

    @RequestMapping("admin-download")
    public void adminDownloadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!CommandArgs.getBoolean(CommandArgs._AdminDownload)) {
            response.sendError(404);
            return;
        }

        String type = getParameter(request, "type", "error");
        String file = getParameter(request, "file");

        // 日志
        if ("log".equalsIgnoreCase(type) || "error".equalsIgnoreCase(type)) {
            File logFile;
            if (StringUtils.isNotBlank(file)) {
                logFile = new File(RebuildConfiguration.getFileOfData("_log"), file);
            } else {
                logFile = SysbaseHeartbeat.getLastLogbackFile("error".equalsIgnoreCase(type));
            }

            FileDownloader.setDownloadHeaders(response, logFile.getName(), false);
            FileDownloader.writeLocalFile(logFile, response);
        }
        // 数据库
        else if ("database".equalsIgnoreCase(type) || "db".equalsIgnoreCase(type)) {
            File path = RebuildConfiguration.getFileOfData("");
            path = new File(path, "_backups");

            File dbFile = null;
            if (StringUtils.isNotBlank(file)) {
                dbFile = new File(path, file);
            } else {
                try (Stream<Path> s = Files.list(path.toPath())) {
                    Optional<Path> max = s.filter(Files::isRegularFile)
                            .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
                    if (max.isPresent()) dbFile = max.get().toFile();
                }
            }

            if (dbFile != null) FileDownloader.setDownloadHeaders(response, dbFile.getName(), false);
            FileDownloader.writeLocalFile(dbFile, response);
        }
    }

    // -- LogView

    private static final int MAX_SCAN_LINES = 200000;

    private static final Pattern ENTRY_START = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[.,]\\d+\\s+(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)");

    private static final int MAX_TAIL_READ = 2 * 1024 * 1024;

    @GetMapping("admin-logview")
    public ModelAndView page(HttpServletRequest request) {
        RbAssert.isSuperAdmin(getRequestUser(request));
        return createModelAndView("/admin/admin-logview");
    }

    @GetMapping("admin-logview/files")
    public RespBody files() {
        File logd = RebuildConfiguration.getFileOfData("_log");

        JSONArray res = new JSONArray();
        File[] files = logd.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : files) {
                if (!f.isFile() || !f.getName().endsWith(".log")) continue;

                JSONObject item = new JSONObject();
                item.put("name", f.getName());
                item.put("size", f.length());
                item.put("modifiedOn", f.lastModified());
                res.add(item);
            }
        }
        return RespBody.ok(res);
    }

    @GetMapping("admin-logview/content")
    public RespBody content(HttpServletRequest request) throws IOException {
        String fileName = getParameterNotNull(request, "file");
        int lines = getIntParameter(request, "lines", 500);
        lines = Math.min(Math.max(lines, 50), 5000);
        String keyword = getParameter(request, "q");
        String level = getParameter(request, "level");

        File logFile = checkLogFile(fileName);
        if (logFile == null || !logFile.exists()) {
            return RespBody.error("Log file not found");
        }

        List<String> tailLines = readTailEntries(logFile, lines, keyword, level);
        return RespBody.ok(tailLines);
    }

    @GetMapping("admin-logview/tail")
    public RespBody tail(HttpServletRequest request) throws IOException {
        String fileName = getParameterNotNull(request, "file");
        String keyword = getParameter(request, "q");
        String level = getParameter(request, "level");
        String posParam = getParameter(request, "pos");

        File logFile = checkLogFile(fileName);
        if (logFile == null || !logFile.exists()) {
            return RespBody.error("Log file not found");
        }

        final long len = logFile.length();
        long pos = -1;
        if (StringUtils.isNotBlank(posParam)) {
            try {
                pos = Long.parseLong(posParam);
            } catch (NumberFormatException ignored) {
            }
        }
        if (pos < 0 || pos > len) pos = len;

        JSONObject res = new JSONObject();
        res.put("pos", pos);
        JSONArray lines = new JSONArray();
        res.put("lines", lines);
        if (pos == len) return RespBody.ok(res);

        int size = (int) Math.min(len - pos, MAX_TAIL_READ);
        byte[] buf = new byte[size];
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            raf.seek(pos);
            raf.readFully(buf);
        }

        String chunk = new String(buf, StandardCharsets.UTF_8);
        int nl = chunk.lastIndexOf('\n');
        if (nl < 0) return RespBody.ok(res);  // 暂无完整行
        res.put("pos", pos + nl + 1);

        final int minWeight = levelWeight(level);
        final String kw = StringUtils.isBlank(keyword) ? null : keyword.trim().toLowerCase();

        List<List<String>> entries = new ArrayList<>();
        List<String> current = null;
        for (String line : chunk.substring(0, nl).split("\n", -1)) {
            if (current == null || ENTRY_START.matcher(line).lookingAt()) {
                current = new ArrayList<>();
                entries.add(current);
            }
            current.add(line);
        }

        for (List<String> entry : entries) {
            if (minWeight > 0 && levelWeight(parseLevel(entry.get(0))) < minWeight) continue;

            if (kw != null) {
                boolean hit = false;
                for (String l : entry) {
                    if (l.toLowerCase().contains(kw)) {
                        hit = true;
                        break;
                    }
                }
                if (!hit) continue;
            }
            lines.addAll(entry);
        }

        return RespBody.ok(res);
    }

    private File checkLogFile(String fileName) throws IOException {
        if (!fileName.matches("[a-zA-Z0-9_.\\-]+\\.log")) {
            return null;
        }

        File logDir = RebuildConfiguration.getFileOfData("_log");
        File logFile = new File(logDir, fileName);
        if (!logFile.getCanonicalPath().startsWith(logDir.getCanonicalPath() + File.separator)) {
            return null;
        }
        return logFile;
    }

    private List<String> readTailEntries(File file, int maxEntries, String keyword, String minLevel) throws IOException {
        final int minWeight = levelWeight(minLevel);
        final String kw = StringUtils.isBlank(keyword) ? null : keyword.trim().toLowerCase();
        final boolean noFilter = minWeight == 0 && kw == null;

        // 1. 从尾部读取行（倒序）
        List<String> reversed = new ArrayList<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
            int entryStarts = 0;
            String line;
            while (reversed.size() < MAX_SCAN_LINES && (line = reader.readLine()) != null) {
                if (ENTRY_START.matcher(line).lookingAt()) {
                    if (noFilter && entryStarts >= maxEntries) break;
                    entryStarts++;
                }
                reversed.add(line);
            }
        }
        Collections.reverse(reversed);

        // 2. 按时间戳前缀分组为日志条目
        List<List<String>> entries = new ArrayList<>();
        List<String> current = null;
        for (String line : reversed) {
            if (current == null || ENTRY_START.matcher(line).lookingAt()) {
                current = new ArrayList<>();
                entries.add(current);
            }
            current.add(line);
        }

        // 3. 级别 + 关键字过滤
        final int maxOutLines = maxEntries * 20;

        List<String> res = new ArrayList<>();
        int matched = 0;
        for (int i = entries.size() - 1; i >= 0 && matched < maxEntries; i--) {
            List<String> entry = entries.get(i);

            if (minWeight > 0 && levelWeight(parseLevel(entry.get(0))) < minWeight) continue;

            if (kw != null) {
                boolean hit = false;
                for (String l : entry) {
                    if (l.toLowerCase().contains(kw)) {
                        hit = true;
                        break;
                    }
                }
                if (!hit) continue;
            }

            // 超出输出上限时保留更新的条目
            if (res.size() + entry.size() > maxOutLines) break;

            matched++;
            Collections.reverse(entry);
            res.addAll(entry);
        }

        Collections.reverse(res);
        return res;
    }

    private static String parseLevel(String firstLine) {
        Matcher m = ENTRY_START.matcher(firstLine);
        return m.lookingAt() ? m.group(1) : null;
    }

    private static int levelWeight(String level) {
        if (level == null) return 0;
        switch (level.toUpperCase()) {
            case "TRACE": return 1;
            case "DEBUG": return 2;
            case "INFO": return 3;
            case "WARN": return 4;
            case "ERROR": return 5;
            case "FATAL": return 6;
            default: return 0;
        }
    }


}
