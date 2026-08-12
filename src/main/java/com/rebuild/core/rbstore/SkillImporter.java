/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Record;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.AibotConfigManager;
import com.rebuild.core.aibot2.AibotConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.HeavyTask;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * @author devezhao
 * @since 2026/7/26
 */
@Slf4j
public class SkillImporter extends HeavyTask<Integer> {

    @Setter
    private String[] skillNames;

    @Override
    protected Integer exec() throws Exception {
        Assert.notNull(skillNames, "[skillNames] cannot be null");

        JSONArray schemas = (JSONArray) RBStore.fetchSkills(null);
        if (schemas == null || schemas.isEmpty()) {
            this.errorMessage = Language.L("暂无可用技能");
            return 0;
        }

        this.setTotal(skillNames.length);

        for (String name : skillNames) {
            try {
                JSONObject skillIndex = findSkill(schemas, name);
                if (skillIndex == null) {
                    log.warn("Skill not found in rbstore : {}", name);
                    this.addCompleted();
                    continue;
                }

                String skillName = skillIndex.getString("name");

                JSONObject skill = (JSONObject) RBStore.fetchSkills(skillIndex.getString("file"));

                Record record = EntityHelper.forNew(EntityHelper.AibotConfig);
                record.setString("type", AibotConfigManager.TYPE_SKILL);
                record.setString("name", skillName);

                JSONObject config = new JSONObject();
                config.put("name", skillName);
                config.put("description", skill.getString("description"));
                config.put("prompt", skill.getString("prompt"));
                record.setString("config", config.toJSONString());

                Application.getBean(AibotConfigService.class).create(record);
                log.info("Skill imported : {}", skillName);
                this.addSucceeded();

            } catch (Exception ex) {
                log.error("Cannot import skill : {}", name, ex);
                this.errorMessage = ex.getLocalizedMessage();
            }
            this.addCompleted();
        }

        return getSucceeded();
    }

    private JSONObject findSkill(JSONArray schemas, String name) {
        for (Object o : schemas) {
            JSONObject item = (JSONObject) o;
            if (name.equalsIgnoreCase(item.getString("name"))) {
                return item;
            }
        }
        return null;
    }
}
