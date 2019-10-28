/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.business.approval;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

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
		FlowParser flowParser = createFlowParser(0);
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
		InputStream in = FlowParserTest.class.getClassLoader().getResourceAsStream("approval-flow" + (fileNo > 0 ? fileNo : "") + ".json");
		JSONObject flowDefinition = JSON.parseObject(in, null);
		return new FlowParser(flowDefinition);
	}
}
