package com.rebuild.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DeepSeekChat {

    public static void main(String[] args) throws Exception {
        JSONArray messages = new JSONArray();
        String prompt = CommonsUtils.getStringOfRes("prompt.md");
        messages.add(buildMessage(prompt, "system"));
        print(messages, false);

        JSONObject res = completions(messages);
        JSONObject c = res.getJSONArray("choices").getJSONObject(0);
        messages.add(c.get("message"));

        Scanner scanner = new Scanner(System.in);
        while (true) {
            print(messages, true);

            String user = scanner.next();
            if ("QUIT".equalsIgnoreCase(user)) break;

            messages.add(buildMessage(user, null));
            res = completions(messages);
            c = res.getJSONArray("choices").getJSONObject(0);
            messages.add(c.get("message"));
        }

        System.out.println("bye");
        scanner.close();
        System.exit(0);
    }

    static JSONObject completions(JSONArray messages) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer sk-ef4fa5d9972745d4be4e32258a4c822d");

        JSONObject body = new JSONObject();
        body.put("model", "deepseek-chat");
        body.put("stream", false);
        body.put("max_tokens", 2048);
        body.put("temperature", 0.2);
        body.put("messages", messages);

        String res = OkHttpUtils.post("https://api.deepseek.com/chat/completions", body, headers);
        return (JSONObject) JSON.parse(res);
    }

    static JSONObject buildMessage(String content, String role) {
        if (role == null) role = "user";
        return JSONUtils.toJSONObject(new String[]{"role", "content"}, new Object[]{role, content});
    }

    static void print(JSONArray messages, boolean ask) {
        JSONObject m = messages.getJSONObject(messages.size() - 1);
        System.out.println("@" + m.getString("role").toUpperCase() + ":\n" + m.getString("content") + "\n----");
        if (ask) System.out.println("请输入:");
    }
}
