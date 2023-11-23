/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.datareport;

import cn.devezhao.persist4j.Entity;

import java.io.File;

/**
 * @author devezhao
 * @since 2023/4/5
 */
public class TemplateFile {

    final public File templateFile;
    final public String templateContent;
    final public Entity entity;
    final public int type;
    final public boolean isV33;

    public TemplateFile(File templateFile, Entity entity, int type, boolean isV33) {
        this.templateFile = templateFile;
        this.entity = entity;
        this.type = type;
        this.isV33 = isV33;
        this.templateContent = null;
    }

    // HTML5
    public TemplateFile(String templateContent, Entity entity) {
        this.templateContent = templateContent;
        this.entity = entity;
        this.type = DataReportManager.TYPE_HTML5;
        this.isV33 = true;
        this.templateFile = null;
    }
}
