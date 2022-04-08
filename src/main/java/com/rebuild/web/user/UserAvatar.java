/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.user;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.UserService;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.AppUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.commons.FileDownloader;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 用户头像
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/05/08
 */
@Slf4j
@Controller
@RequestMapping("/account")
public class UserAvatar extends BaseController {

    @GetMapping("/user-avatar")
    public void renderAvatat(HttpServletRequest request, HttpServletResponse response) throws IOException {
        renderUserAvatar(getRequestUser(request), request, response);
    }

    @GetMapping("/user-avatar/{user}")
    public void renderAvatat(@PathVariable String user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        renderUserAvatar(user, request, response);
    }

    /**
     * @param user
     * @param response
     * @throws IOException
     */
    private void renderUserAvatar(Object user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (user == null) {
            response.sendRedirect(AppUtils.getContextPath(UserHelper.DEFAULT_AVATAR));
            return;
        }

        if (user.equals(UserService.ALLUSERS.toString())) {
            response.sendRedirect(AppUtils.getContextPath("/assets/img/avatar-users.png"));
            return;
        }

        User realUser = null;
        if (ID.isId(user) && Application.getUserStore().existsUser(ID.valueOf(user.toString()))) {
            realUser = Application.getUserStore().getUser(ID.valueOf(user.toString()));
        } else if (Application.getUserStore().existsName(user.toString())) {
            realUser = Application.getUserStore().getUserByName(user.toString());
        } else if (Application.getUserStore().existsEmail(user.toString())) {
            realUser = Application.getUserStore().getUserByEmail(user.toString());
        }

        if (realUser == null) {
            response.sendRedirect(AppUtils.getContextPath(UserHelper.DEFAULT_AVATAR));
            return;
        }

        ServletUtils.addCacheHead(response, 30);

        String avatarUrl = realUser.getAvatarUrl();

        // 外部地址
        if (avatarUrl != null && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))) {
            response.sendRedirect(avatarUrl);
            return;
        }

        avatarUrl = QiniuCloud.encodeUrl(avatarUrl);
        if (avatarUrl != null) {
            int w = getIntParameter(request, "w", 100);
            avatarUrl = avatarUrl + "?imageView2/2/w/" + w + "/interlace/1/q/100";

            if (QiniuCloud.instance().available()) {
                avatarUrl = QiniuCloud.instance().makeUrl(avatarUrl, 30 * 60);
            } else {
                avatarUrl = AppUtils.getContextPath("/filex/img/" + avatarUrl);
                String authToken = request.getParameter(AppUtils.URL_AUTHTOKEN);
                if (authToken != null) {
                    avatarUrl += String.format("&%s=%s", AppUtils.URL_AUTHTOKEN, authToken);
                }
            }
            response.sendRedirect(avatarUrl);

        } else {

            String fullName = realUser.getFullName();
            if (realUser.getId().equals(UserService.SYSTEM_USER) || realUser.getId().equals(UserService.ADMIN_USER)) {
                fullName = "RB";
            }

            File avatarFile = UserHelper.generateAvatar(fullName, false);
            FileDownloader.writeLocalFile(avatarFile, response);
        }
    }

    @RequestMapping("/user-avatar-update")
    @ResponseBody
    public RespBody avatarUpdate(HttpServletRequest request) throws IOException {
        final ID user = getRequestUser(request);
        String avatarRaw = getParameterNotNull(request, "avatar");
        String xywh = getParameterNotNull(request, "xywh");

        File avatarFile = RebuildConfiguration.getFileOfTemp(avatarRaw);
        String uploadName = avatarCrop(avatarFile, xywh);

        Record record = EntityHelper.forUpdate(user, user);
        record.setString("avatarUrl", uploadName);
        Application.getBean(UserService.class).update(record);

        ServletUtils.setSessionAttribute(request, "davatarTime", System.currentTimeMillis());
        return RespBody.ok(uploadName);
    }

    /**
     * 头像裁剪
     *
     * @param avatar
     * @param params x,y,width,height
     * @return
     * @throws IOException
     */
    private String avatarCrop(File avatar, String params) throws IOException {
        String[] xywh = params.split(",");
        int x = Integer.parseInt(xywh[0]);
        int y = Integer.parseInt(xywh[1]);
        int width = Integer.parseInt(xywh[2]);
        int height = Integer.parseInt(xywh[3]);

        Thumbnails.Builder<File> builder = Thumbnails.of(avatar)
                .sourceRegion(x, y, width, height);

        String destName = "avatar-" + (System.currentTimeMillis() / 1000) + avatar.getName();
        File dest;
        if (QiniuCloud.instance().available()) {
            dest = RebuildConfiguration.getFileOfTemp(destName);
        } else {
            dest = RebuildConfiguration.getFileOfData(destName);
        }
        builder.scale(1.0).toFile(dest);

        if (QiniuCloud.instance().available()) {
            destName = QiniuCloud.instance().upload(dest);
        }
        return destName;
    }
}
