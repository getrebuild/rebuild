/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.commons;

import cn.devezhao.commons.CodecUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.utils.OkHttpUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Zixin (RB)
 * @since 08/02/2022
 */
@RestController
@RequestMapping("/commons/map/")
public class MapController extends BaseController {

    @GetMapping("suggest")
    public RespBody suggest(HttpServletRequest req) {
        String q = getParameterNotNull(req, "q");
        String city = getParameter(req, "city", "中国");

        // https://lbsyun.baidu.com/index.php?title=webapi/place-suggestion-api
        String ak = StringUtils.defaultIfBlank(
                RebuildConfiguration.get(ConfigurationItem.PortalBaiduMapAk), "YQKHNmIcOgYccKepCkxetRDy8oTC28nD");
        String qUrl = String.format(
                "https://api.map.baidu.com/place/v2/suggestion?q=%s&region=%s&city_limit=%s&output=json&ak=%s",
                CodecUtils.urlEncode(q), CodecUtils.urlEncode(city), "false", ak);

        JSON dataJson;
        try {
            String data = OkHttpUtils.get(qUrl);
            dataJson = JSON.parseObject(data);
        } catch (IOException e) {
            return RespBody.error(e.getLocalizedMessage());
        }

        return RespBody.ok(dataJson);
    }
}
