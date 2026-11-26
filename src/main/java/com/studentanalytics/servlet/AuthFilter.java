package com.studentanalytics.servlet;

import com.studentanalytics.model.Role;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Role-based access control, checked on the server for every request.
 *
 *   /students, /classes  -> TEACHER only (create sections, add / edit / delete students)
 *   /portal              -> STUDENT only (read-only view of the student's own record)
 *
 * Signed-out visitors go to the sign-in page. A signed-in person who opens a page that
 * belongs to the other role is sent back to their own home page instead.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = { "/students", "/classes", "/portal" })
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        Role role = session == null ? null : (Role) session.getAttribute(AuthServlet.USER_ROLE);

        if (session == null || session.getAttribute(AuthServlet.USER_ID) == null || role == null) {
            resp.sendRedirect(req.getContextPath() + "/auth.html#signin");
            return;
        }

        Role needed = "/portal".equals(req.getServletPath()) ? Role.STUDENT : Role.TEACHER;
        if (role != needed) {
            resp.sendRedirect(req.getContextPath() + "/" + role.getHome());
            return;
        }

        // Stops the browser showing cached pages through the Back button after sign-out.
        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
