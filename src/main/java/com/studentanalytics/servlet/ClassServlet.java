package com.studentanalytics.servlet;

import com.studentanalytics.dao.ClassDao;
import com.studentanalytics.dao.DataAccessException;
import com.studentanalytics.dao.Database;
import com.studentanalytics.dao.JdbcStudentDao;
import com.studentanalytics.dao.StudentDao;
import com.studentanalytics.model.SchoolClass;
import com.studentanalytics.model.Student;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Teacher-only: create, rename and delete sections (classes).
 * AuthFilter only lets accounts with the TEACHER role reach this servlet.
 *
 * GET  /classes                       -> list of sections        (classes.jsp)
 * POST /classes  action=create        (name)
 * POST /classes  action=rename        (classId, name)
 * POST /classes  action=delete        (classId)   students stay, as "unassigned"
 */
@WebServlet(name = "ClassServlet", urlPatterns = "/classes")
public class ClassServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/classes.jsp";
    private static final String FLASH = "flashMessage";
    private static final int MAX_NAME = 40;

    private transient ClassDao classes;
    private transient StudentDao students;

    @Override
    public void init() throws ServletException {
        try {
            Database.initSchema();
            classes = new ClassDao();
            students = new JdbcStudentDao();
        } catch (DataAccessException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(FLASH) != null) {
            req.setAttribute("message", session.getAttribute(FLASH));
            session.removeAttribute(FLASH);
        }
        try {
            long uid = uid(req);
            List<SchoolClass> list = classes.findAll(uid);
            List<Student> all = students.findAll(uid);
            int unassigned = 0;
            for (Student s : all) {
                if (s.getClassId() == null) {
                    unassigned++;
                }
            }
            req.setAttribute("classes", list);
            req.setAttribute("totalStudents", all.size());
            req.setAttribute("unassignedCount", unassigned);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        } catch (DataAccessException e) {
            log("Database error during GET /classes", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error. Check that MySQL is running and see the server log.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing action");
            return;
        }
        try {
            long uid = uid(req);
            switch (action) {
                case "create": {
                    String name = cleanName(req.getParameter("name"));
                    classes.create(uid, name);
                    flash(req, "Created section " + name);
                    break;
                }
                case "rename": {
                    String name = cleanName(req.getParameter("name"));
                    long id = parseId(req.getParameter("classId"));
                    flash(req, classes.rename(uid, id, name)
                            ? "Renamed section to " + name : "That section no longer exists");
                    break;
                }
                case "delete": {
                    long id = parseId(req.getParameter("classId"));
                    SchoolClass sc = classes.findById(uid, id);
                    if (sc != null && classes.delete(uid, id)) {
                        flash(req, "Deleted section " + sc.getName()
                                + (sc.getStudentCount() > 0
                                        ? ". Its " + sc.getStudentCount() + " student(s) are now unassigned." : ""));
                    } else {
                        flash(req, "That section no longer exists");
                    }
                    break;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                    return;
            }
        } catch (IllegalArgumentException e) {
            flash(req, e.getMessage());
        } catch (DataAccessException e) {
            log("Database error during POST /classes " + action, e);
            flash(req, "Database error. Nothing was saved. Check that MySQL is running.");
        }
        resp.sendRedirect(req.getContextPath() + "/classes");
    }

    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Enter a section name, for example 10-A");
        }
        if (name.length() > MAX_NAME) {
            throw new IllegalArgumentException("Section names can have at most " + MAX_NAME + " characters");
        }
        return name;
    }

    private static long parseId(String text) {
        try {
            return Long.parseLong(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown section");
        }
    }

    private static long uid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute(AuthServlet.USER_ID);
        if (!(id instanceof Number)) {
            throw new IllegalStateException("No signed-in user on this request");
        }
        return ((Number) id).longValue();
    }

    private static void flash(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH, message);
    }
}
