/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.approval;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/07/06
 */
public class FlowParserTest {

    @Test
    public void testParse() throws Exception {
        FlowParser flowParser = createFlowParser(2);
        System.out.println("NODES :");
        for (FlowNode node : flowParser.getAllNodes()) {
            System.out.println(node);
        }
        System.out.println();

        flowParser.prettyPrint("ROOT", null);
    }

    @Test
    public void testFind() throws Exception {
        FlowParser flowParser = createFlowParser(1);
        System.out.println("ROOT :");
        FlowNode root = flowParser.getNode("ROOT");
        System.out.println(root);
        System.out.println();

        System.out.println("CHILDREN of ROOT :");
        List<FlowNode> children = flowParser.getNextNodes("ROOT");
        for (FlowNode c : children) {
            System.out.println(c);
        }
        System.out.println();
    }

    /**
     * @param fileNo
     * @return
     * @throws IOException
     */
    static FlowParser createFlowParser(int fileNo) throws IOException {
        File file = ResourceUtils.getFile("classpath:approval-flow" + fileNo + ".json");
        try (InputStream in = new FileInputStream(file)) {
            JSONObject flowDefinition = JSON.parseObject(in, null);
            return new FlowParser(flowDefinition);
        }
    }
}
