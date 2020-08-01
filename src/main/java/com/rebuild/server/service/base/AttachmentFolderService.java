/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.base;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.BaseService;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.BaseServiceImpl;
import com.rebuild.server.service.bizz.UserHelper;

/**
 * 文件目录
 *
 * @author devezhao
 * @since 2019/11/14
 */
public class AttachmentFolderService extends BaseService {

    protected AttachmentFolderService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.AttachmentFolder;
    }

    @Override
    public int delete(ID recordId) {
        Object inFolder = Application.createQueryNoFilter(
                "select inFolder from Attachment where inFolder = ?")
                .setParameter(1, recordId)
                .unique();
        if (inFolder != null) {
            throw new DataSpecificationException("目录内有文件不能删除");
        }

        Object parent = Application.createQueryNoFilter(
                "select parent from AttachmentFolder where parent = ?")
                .setParameter(1, recordId)
                .unique();
        if (parent != null) {
            throw new DataSpecificationException("目录内有子目录不能删除");
        }

        ID user = Application.getCurrentUser();
        if (!UserHelper.isAdmin(user)) {
            Object[] createdBy = Application.createQueryNoFilter(
                    "select createdBy from AttachmentFolder where folderId = ?")
                    .setParameter(1, recordId)
                    .unique();
            if (!user.equals(createdBy[0])) {
                throw new DataSpecificationException("无权删除他人目录");
            }
        }

        return super.delete(recordId);
    }
}
