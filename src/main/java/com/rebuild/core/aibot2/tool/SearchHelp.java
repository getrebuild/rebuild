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

    private static final String SEARCH_API = "https://getrebuild.com/docs/search-api.json?wd=";
    private static final String SEARCH_PAGE = "https://getrebuild.com/docs/search?wd=";
    private static final String DOCS_HOME = "https://getrebuild.com/docs/";
    private static final int MAX_RESULTS = 10;

    @Override
    public Object tool(String arguments) throws Exception {
        final JSONObject args = JSON.parseObject(arguments);

        String keyword = args.getString("keyword");
        if (StringUtils.isBlank(keyword)) {
            throw new KnownToolException("搜索关键词不能为空");
        }

        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
        String url = SEARCH_API + encodedKeyword;
        String pageUrl = SEARCH_PAGE + encodedKeyword;

        String respBody;
        try {
            respBody = OkHttpUtils.get(url);
        } catch (Exception ex) {
            log.error("Failed to fetch help docs : {}", keyword, ex);
            throw new KnownToolException("无法访问帮助文档，请稍后重试");
        }

        JSONObject resp;
        try {
            resp = JSON.parseObject(respBody);
        } catch (Exception ex) {
            log.error("Failed to parse help search response : {}", respBody, ex);
            throw new KnownToolException("无法访问帮助文档，请稍后重试");
        }

        // error 非 0 表示服务异常（如搜索服务暂不可用）
        Object error = resp.get("error");
        if (error != null && !"0".equals(String.valueOf(error))) {
            log.warn("Help search service error : {} {}", keyword, error);
            throw new KnownToolException(error + "，或直接访问 " + DOCS_HOME);
        }

        JSONArray hits = resp.getJSONArray("results");
        if (hits == null || hits.isEmpty()) {
            JSONObject ret = new JSONObject();
            ret.put("status", "ok");
            ret.put("keyword", keyword);
            ret.put("message", "未找到与「" + keyword + "」匹配的帮助文档，请尝试更换关键词");
            ret.put("searchUrl", pageUrl);
            ret.put("docsHome", DOCS_HOME);
            return ret;
        }

        JSONArray results = new JSONArray();
        for (int i = 0; i < hits.size() && results.size() < MAX_RESULTS; i++) {
            JSONObject hit = hits.getJSONObject(i);

            JSONObject result = new JSONObject();
            result.put("title", hit.getString("title"));
            result.put("url", hit.getString("url"));
            result.put("category", hit.getString("category"));
            result.put("description", hit.getString("content"));
            String mdUrl = StringUtils.substringBefore(hit.getString("url"), "#") + ".md";
            result.put("mdUrl", mdUrl);
            results.add(result);
        }

        JSONObject ret = new JSONObject();
        ret.put("status", "ok");
        ret.put("keyword", keyword);
        ret.put("total", resp.getIntValue("total"));
        ret.put("results", results);
        ret.put("searchUrl", pageUrl);
        ret.put("docsHome", DOCS_HOME);
        return ret;
    }
}
