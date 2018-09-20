/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.web.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.rebuild.server.metadata.MetadataHelper;
import cn.devezhao.rebuild.server.service.entitymanage.EasyMeta;
import cn.devezhao.rebuild.web.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/app/")
public class MetadataGet extends BaseControll {

	@RequestMapping("common/metadata/entities")
	public void entities(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Entity[] entities = MetadataHelper.getEntities();
		
		List<Map<String, String>> list = new ArrayList<>();
		for (Entity e : entities) {
			if (EasyMeta.BUILTIN_ENTITY.contains(e.getName())) {
				continue;
			}
			Map<String, String> map = new HashMap<>();
			EasyMeta easy = new EasyMeta(e);
			map.put("name", e.getName());
			map.put("label", easy.getLabel());
			map.put("icon", easy.getIcon());
			list.add(map);
		}
		writeSuccess(response, list);
	}
	
	@RequestMapping("common/metadata/fields")
	public void fields(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String entity = getParameterNotNull(request, "entity");
		Entity entityBase = MetadataHelper.getEntity(entity);
		
		Field[] fields = entityBase.getFields();
		List<Map<String, String>> list = new ArrayList<>();
		for (Field e : fields) {
			Map<String, String> map = new HashMap<>();
			map.put("name", e.getName());
			map.put("label", EasyMeta.getLabel(e));
			list.add(map);
		}
		writeSuccess(response, list);
	}
}
