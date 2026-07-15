/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.aibot.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.utils.JSONUtils;
import com.rebuild.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 搜索 REBUILD 帮助文档，返回匹配的文档列表及摘要
 *
 * @author devezhao
 * @since 2026/7/15
 */
@Slf4j
public class HelpDocs implements Tool {

    private static final String SEARCH_URL = "https://getrebuild.com/docs/search?wd=";
    private static final String DOCS_HOME = "https://getrebuild.com/docs/";
    private static final int MAX_RESULTS = 10;

    @Override
    public Object tool(String arguments) throws Exception {
        JSONObject args = StringUtils.isBlank(arguments) ? new JSONObject() : JSON.parseObject(arguments);
        String keyword = args.getString("keyword");
        if (StringUtils.isBlank(keyword)) {
            throw new ToolException("搜索关键词不能为空");
        }

        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
        String url = SEARCH_URL + encodedKeyword;

        String html;
        try {
            html = OkHttpUtils.get(url);
        } catch (Exception ex) {
            log.error("Failed to fetch help docs : {}", keyword, ex);
            throw new ToolException("无法访问帮助文档，请稍后重试");
        }

        Document doc = Jsoup.parse(html, DOCS_HOME);

        // 定位搜索结果容器（h6 标签包含总条数）
        Element countEl = doc.selectFirst("h6:contains(搜索结果)");
        int total = 0;
        Element resultsContainer = null;
        if (countEl != null) {
            resultsContainer = countEl.parent();
            String countText = countEl.text();
            String numStr = countText.replaceAll("[^0-9]", "");
            if (StringUtils.isNotBlank(numStr)) {
                total = Integer.parseInt(numStr);
            }
        }

        JSONArray results = new JSONArray();
        if (resultsContainer != null) {
            for (Element child : resultsContainer.children()) {
                if (!"div".equals(child.tagName())) continue;

                Element link = child.selectFirst("a[href]");
                if (link == null) continue;

                String docUrl = link.attr("abs:href");
                Element linkClone = link.clone();
                linkClone.select("small").remove();
                String title = linkClone.text().trim();

                String category = "";
                Element small = link.selectFirst("small");
                if (small != null) {
                    category = small.text().trim();
                }

                Element descEl = child.selectFirst("p");
                String description = descEl != null ? descEl.text().trim() : "";

                JSONObject result = new JSONObject();
                result.put("title", title);
                result.put("url", docUrl);
                result.put("category", category);
                result.put("description", description);
                results.add(result);

                if (results.size() >= MAX_RESULTS) break;
            }
        }

        if (results.isEmpty()) {
            return JSONUtils.toJSONObject(
                    new String[]{"status", "keyword", "message"},
                    new Object[]{"ok", keyword, "未找到匹配的帮助文档，请尝试更换关键词或访问 " + DOCS_HOME});
        }

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("keyword", keyword);
        ret.put("total", total);
        ret.put("results", results);
        ret.put("searchUrl", url);
        return ret;
    }
}
