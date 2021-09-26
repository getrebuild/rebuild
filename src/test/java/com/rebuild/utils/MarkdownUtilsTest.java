package com.rebuild.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownUtilsTest {

    @Test
    void render() {
        System.out.println(MarkdownUtils.render("> content"));
        System.out.println(MarkdownUtils.render(
                "上海锐昉科技有限公司 [沪ICP备20020345号-3](https://beian.miit.gov.cn/)" +
                        "<script>alert(1)</script>" +
                        "<iframe url=\"http://baidu.com\"></iframe>"));
    }
}