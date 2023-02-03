/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qiniu.util.Auth;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 */
public class Tests {

    @Test
    void qn() {

        String bucketName = "lusi";
        Auth auth = Auth.create("mfDsPUcC5hrVQTS9AEt-y1W3z2S3HDSd2BiWnJUG", "hdBJFYKR2MwsGYQtSegUPtrw75W3GPiFphztaCe8");

        String url = "https://sources.egrettrip.com/rb/20230128/134059781__mmexport1674884437538.jpg?imageView2/2/w/100/interlace/1/q/100";
        long deadline = 1675431193;

        System.out.println(auth.privateDownloadUrlWithDeadline(url, deadline));
    }

    @Disabled
    @Test
    void test() throws IOException {

        JSONObject TRANS = JSON.parseObject("{TRANS:{ SOURCE_ID:null, TRANSDATA:{ HEADER:{} } }}");
        TRANS.getJSONObject("TRANS").put("SOURCE_ID", "test1");
        JSONObject HEADER = TRANS.getJSONObject("TRANS").getJSONObject("TRANSDATA").getJSONObject("HEADER");

        String post = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://WS_PORTAL\" xmlns:xsd=\"http://WS_SAP/xsd\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <ws:importDatas>\n" +
                "         <ws:Request>\n" +
                "            <xsd:requestData>" + TRANS + "</xsd:requestData>\n" +
                "            <xsd:wsCode>PORT_SUPERVISED_WAREHOUSE</xsd:wsCode>\n" +
                "         </ws:Request>\n" +
                "      </ws:importDatas>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BasicAuthInterceptor("pact", "Ceaiec!Erp001"))
                .build();
        Request request = new Request.Builder()
//                .url("http://172.28.20.203:6443/axis2/services/WS_PORTAL")
                .url("http://172.28.20.203:6443/axis2/services/WS_PORTAL.WS_PORTALHttpSoap11Endpoint/")
                .post(RequestBody.create(post, MediaType.parse("text/plain")))
                .build();
        Response response = client.newCall(request).execute();

        byte[] b = Objects.requireNonNull(response.body()).bytes();
        String res = new String(b, StandardCharsets.UTF_8);
        System.out.println(res);
    }

    static class BasicAuthInterceptor implements Interceptor {
        private String credentials;
        public BasicAuthInterceptor(String user, String password) {
            this.credentials = Credentials.basic(user, password);
        }
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Request authenticatedRequest = request.newBuilder()
                    .header("Authorization", credentials).build();
            return chain.proceed(authenticatedRequest);
        }
    }
}
