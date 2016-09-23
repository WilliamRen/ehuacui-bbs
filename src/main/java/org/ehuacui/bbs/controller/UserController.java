package org.ehuacui.bbs.controller;

import com.jfinal.kit.PropKit;
import org.ehuacui.bbs.common.BaseController;
import org.ehuacui.bbs.common.Constants;
import org.ehuacui.bbs.common.Page;
import org.ehuacui.bbs.common.ServiceHolder;
import org.ehuacui.bbs.interceptor.BeforeAdviceController;
import org.ehuacui.bbs.interceptor.UserInterceptor;
import org.ehuacui.bbs.interceptor.UserStatusInterceptor;
import org.ehuacui.bbs.model.Collect;
import org.ehuacui.bbs.model.Reply;
import org.ehuacui.bbs.model.Topic;
import org.ehuacui.bbs.model.User;
import org.ehuacui.bbs.template.FormatDate;
import org.ehuacui.bbs.template.GetNameByTab;
import org.ehuacui.bbs.template.Marked;
import org.ehuacui.bbs.utils.StringUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by ehuacui.
 * Copyright (c) 2016, All Rights Reserved.
 * http://www.ehuacui.org
 */
@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

    /**
     * 用户个人主页
     */
    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String index(HttpServletRequest request, @RequestParam("nickname") String nickname) {
        User currentUser = ServiceHolder.userService.findByNickname(nickname);
        if (currentUser != null) {
            Long collectCount = ServiceHolder.collectService.countByUid(currentUser.getId());
            currentUser.setCollectCount(collectCount);
            Page<Topic> topicPage = ServiceHolder.topicService.pageByAuthor(1, 7, nickname);
            Page<Reply> replyPage = ServiceHolder.replyService.pageByAuthor(1, 7, nickname);
            request.setAttribute("topicPage", topicPage);
            request.setAttribute("replyPage", replyPage);
            request.setAttribute("currentUser", currentUser);
            request.setAttribute("formatDate", new FormatDate());
            request.setAttribute("getNameByTab", new GetNameByTab());
            request.setAttribute("marked", new Marked());
            request.setAttribute("pageTitle", currentUser.getNickname() + " 个人主页");
        } else {
            request.setAttribute("pageTitle", "用户未找到");
        }
        return "user/info.ftl";
    }

    /**
     * 用户的话题列表
     */
    @RequestMapping(value = "/topics", method = RequestMethod.GET)
    public String topics(HttpServletRequest request,
                         @RequestParam("nickname") String nickname,
                         @RequestParam(value = "p", defaultValue = "1") Integer p) {
        User user = ServiceHolder.userService.findByNickname(nickname);
        request.setAttribute("currentUser", user);
        Page<Topic> page = ServiceHolder.topicService.pageByAuthor(p, PropKit.getInt("pageSize"), nickname);
        request.setAttribute("page", page);
        request.setAttribute("formatDate", new FormatDate());
        request.setAttribute("getNameByTab", new GetNameByTab());
        return "user/topics.ftl";

    }

    /**
     * 用户的回复列表
     */
    @RequestMapping(value = "/replies", method = RequestMethod.GET)
    public String replies(HttpServletRequest request,
                          @RequestParam("nickname") String nickname,
                          @RequestParam(value = "p", defaultValue = "1") Integer p) {
        User user = ServiceHolder.userService.findByNickname(nickname);
        request.setAttribute("currentUser", user);
        Page<Reply> page = ServiceHolder.replyService.pageByAuthor(p, PropKit.getInt("pageSize"), nickname);
        request.setAttribute("page", page);
        request.setAttribute("marked", new Marked());
        request.setAttribute("formatDate", new FormatDate());
        return "user/replies.ftl";
    }

    /**
     * 用户的话题列表
     */
    @RequestMapping(value = "/collects", method = RequestMethod.GET)
    public String collects(HttpServletRequest request, @RequestParam("nickname") String nickname,
                           @RequestParam(value = "p", defaultValue = "1") Integer p) {
        User user = ServiceHolder.userService.findByNickname(nickname);
        request.setAttribute("currentUser", user);
        Page<Collect> page = ServiceHolder.collectService.findByUid(p, PropKit.getInt("pageSize"), user.getId());
        request.setAttribute("page", page);
        request.setAttribute("formatDate", new FormatDate());
        request.setAttribute("getNameByTab", new GetNameByTab());
        return "user/collects.ftl";
    }

    /**
     * 用户设置
     */
    @BeforeAdviceController({UserInterceptor.class, UserStatusInterceptor.class})
    @RequestMapping(value = "/setting", method = RequestMethod.POST)
    public String setting(HttpServletRequest request,
                          @RequestParam("url") String url,
                          @RequestParam("signature") String signature,
                          @RequestParam(value = "receive_msg", defaultValue = "0") Integer receive_msg) {
        User user = getUser(request);
        user.setSignature(StringUtil.notBlank(signature) ? Jsoup.clean(signature, Whitelist.basic()) : null);
        user.setUrl(StringUtil.notBlank(url) ? Jsoup.clean(url, Whitelist.basic()) : null);
        user.setReceiveMsg(receive_msg == 1);
        ServiceHolder.userService.update(user);
        //清理缓存
        try {
            clearCache(Constants.CacheEnum.usernickname.name() + URLEncoder.encode(user.getNickname(), "utf-8"));
        } catch (UnsupportedEncodingException e) {
        }
        clearCache(Constants.CacheEnum.useraccesstoken.name() + user.getAccessToken());
        request.setAttribute("msg", "保存成功。");

        return "user/setting.ftl";
    }
}
