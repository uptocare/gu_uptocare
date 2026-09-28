package com.uptocare.gu.web;

import java.nio.file.Paths;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.uptocare.gu.data.PostRepository;

@WebListener
public class AppInit implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent e) {
        PostRepository.init(Paths.get(e.getServletContext().getRealPath("/WEB-INF/posts.txt")));
    }
}
