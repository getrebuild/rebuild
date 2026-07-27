/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
public class SearchHelp implements Tool {

    private static final String SEARCH_URL = "https://getrebuild.com/docs/search?wd=";
    private static final String DOCS_HOME = "https://getrebuild.com/docs/";
    private static final int MAX_RESULTS = 10;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

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

        // 检测搜索服务异常（h5 标签包含错误信息）
        Element errorEl = doc.selectFirst("div.results h5");
        if (errorEl != null) {
            String errorText = errorEl.text().trim();
            // 搜索服务不可用
            if (errorText.contains("暂不可用") || errorText.contains("不可用")) {
                log.warn("Help search service unavailable : {}", keyword);
                throw new ToolException("帮助文档搜索服务暂不可用，请稍后重试或直接访问 " + DOCS_HOME);
            }
            // 无搜索结果
            if (errorText.contains("没有找到") || errorText.contains("未找到")) {
                JSONObject ret = new JSONObject();
                ret.put("status", "ok");
                ret.put("keyword", keyword);
                ret.put("message", "未找到与「" + keyword + "」匹配的帮助文档，请尝试更换关键词");
                ret.put("searchUrl", url);
                ret.put("docsHome", DOCS_HOME);
                return ret;
            }
        }

        // 定位搜索结果容器（优先通过 h6 标签，其次通过 div.results）
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
        // 回退方案：直接使用 div.results 作为容器
        if (resultsContainer == null) {
            resultsContainer = doc.selectFirst("div.results");
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
            JSONObject ret = new JSONObject();
            ret.put("status", "ok");
            ret.put("keyword", keyword);
            ret.put("message", "未找到匹配的帮助文档，请尝试更换关键词");
            ret.put("searchUrl", url);
            ret.put("docsHome", DOCS_HOME);
            return ret;
        }

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("keyword", keyword);
        ret.put("total", total);
        ret.put("results", results);
        ret.put("searchUrl", url);
        ret.put("docsHome", DOCS_HOME);
        return ret;
    }
}
