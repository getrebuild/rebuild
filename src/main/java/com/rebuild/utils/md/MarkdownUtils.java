/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils.md;

import com.rebuild.api.user.AuthTokenManager;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MD 转换工具
 *
 * @author devezhao-mbp
 * @since 2019/05/16
 */
public class MarkdownUtils {

    // markdown 中的文件下载链接，输出前重写补全 `_csrfToken`
    private static final Pattern PATTERN_FILE_URL = Pattern.compile("\\]\\(([^)]*?/filex/download/[^)]*)\\)");

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    private static final Parser PARSER2;
    private static final HtmlRenderer RENDERER2;

    static {
        MutableDataSet option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MARKDOWN).set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(), TaskListExtension.create()));
        PARSER = Parser.builder(option).build();
        RENDERER = HtmlRenderer.builder(option).build();

        option = new MutableDataSet();
        option.setFrom(ParserEmulationProfile.MARKDOWN).set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(), TaskListExtension.create(),
                        MarkdownLinkAttrProvider.MarkdownLinkAttrExtension.create(), TocExtension.create()));
        PARSER2 = Parser.builder(option).build();
        RENDERER2 = HtmlRenderer.builder(option).build();
    }

    /**
     * MD 渲染，支持表格，HTML 代码会转义
     *
     * @param md
     * @return
     * @see #render(String, boolean, boolean)
     */
    public static String render(String md) {
        return render(md, false, false);
    }

    /**
     * MD 渲染，支持表格
     *
     * @param md
     * @param targetBlank
     * @param keepHtml HTML 代码保持
     * @return
     * @see CommonsUtils#escapeHtml(Object)
     */
    public static String render(String md, boolean targetBlank, boolean keepHtml) {
        if (keepHtml) {
            md = CommonsUtils.sanitizeHtml(md);
        } else {
            md = CommonsUtils.escapeHtml(md);
            md = md.replace("&gt; ", "> ");  // for MD quote
        }

        if (targetBlank) {
            Node document = PARSER2.parse(md);
            return RENDERER2.render(document);
        } else {
            Node document = PARSER.parse(md);
            return RENDERER.render(document);
        }
    }

    /**
     * MD 渲染，仅启用指定的语法（HTML 标签白名单）
     *
     * @param md
     * @param allowedTags
     * @return
     */
    public static String renderSafe444(String md, String... allowedTags) {
        String html = render(md, true, true);
        if (allowedTags == null || allowedTags.length == 0) {
            return Jsoup.clean(html, Safelist.none());
        }

        Safelist safelist = Safelist.none().addTags(allowedTags);
        safelist.addAttributes("a", "href", "target", "title");
        safelist.addAttributes("img", "src", "alt", "title");
        safelist.addAttributes("font", "color", "size");
        safelist.addProtocols("img", "src", "http", "https");
        safelist.addProtocols("a", "href", "http", "https", "mailto", "ftp");

        return Jsoup.clean(html, safelist);
    }

    /**
     * 清理 MD（包括 HTML） 格式
     *
     * @param md
     * @return
     */
    public static String cleanMarks(String md) {
        // 保留图片名称
        Pattern p = Pattern.compile("!\\[.*?]\\(([^)]+)\\)");
        Matcher m = p.matcher(md);
        while (m.find()) {
            String url =  m.group(1);
            md = md.replace("[" + url + "]", "[" + QiniuCloud.parseFileName(url) + "]");
        }

        // 保留为 [xxx]
        md = md.replaceAll("!\\[.*?]\\((.*?)\\)", "[$1]"); // 替换图片

        String html = render(md, false, true);
        return Jsoup.parse(html).body().text();
    }

    /**
     * 重写 markdown 中的文件下载链接
     *
     * @param md
     * @return
     * @see com.rebuild.web.commons.FileDownloader#checkUser(javax.servlet.http.HttpServletRequest)
     */
    public static String rewriteFileUrls(String md) {
        if (md == null || !md.contains("/filex/download/")) return md;

        Matcher m = PATTERN_FILE_URL.matcher(md);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(1);

            url = url.replaceAll("[?&](_csrfToken|_onceToken)=[^&)]*", "");
            if (url.endsWith("?")) url = url.substring(0, url.length() - 1);

            if (!url.startsWith("http")) {
                if (!url.startsWith("/")) url = "/" + url;
                url = RebuildConfiguration.getHomeUrl(url);
            }

            // 12H
            url += (url.contains("?") ? "&" : "?")
                    + AppUtils.URL_CSRFTOKEN + "=" + AuthTokenManager.generateCsrfToken(12 * 60 * 60);

            m.appendReplacement(sb, Matcher.quoteReplacement("](" + url + ")"));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
