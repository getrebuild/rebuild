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
    final public Entity entity;
    final public boolean isList;
    final public boolean isV33;

    public TemplateFile(File templateFile, Entity entity, boolean isList, boolean isV33) {
        this.templateFile = templateFile;
        this.entity = entity;
        this.isList = isList;
        this.isV33 = isV33;
    }
}
