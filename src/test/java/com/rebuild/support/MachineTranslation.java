/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.support;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.HttpUtils;

/**
 * 机器翻译
 *
 * @author devezhao
 * @since 2020/9/22
 */
public class MachineTranslation {

    public static void main(String[] args) {
        JSONObject source = JSON.parseObject(CommonsUtils.getStringOfRes("i18n/language.zh_CN.json"), Feature.OrderedField);
        for (String key : source.keySet()) {
            String dst = dst((String) source.get(key));
            dst = dst.replace("] (", "](");
            dst = dst.replace("(Http", "(http");
            dst = dst.substring(0, 1).toUpperCase() + dst.substring(1);

            System.out.println(String.format("\"%s\": \"%s\",", key, dst));
        }
    }

    public static String dst(String text) {
        String appid = "20200922000571400";
        String appsecure = "";
        String salt = CodecUtils.randomCode(20);
        String domain = "electronics";

        String sign = EncryptUtils.toMD5Hex(appid + text + salt + domain + appsecure).toLowerCase();
        String url = String.format(
                "http://api.fanyi.baidu.com/api/trans/vip/fieldtranslate?from=zh&to=en&q=%s&appid=%s&salt=%s&domain=%s&sign=%s",
                CodecUtils.urlEncode(text), appid, salt, domain, sign);

        try {
            String res = HttpUtils.get(url);
            JSONObject resJson = JSON.parseObject(res);
            return ((JSONObject) resJson.getJSONArray("trans_result").get(0)).getString("dst");
        } catch (Exception ex) {
            return text;
        }
    }
}
