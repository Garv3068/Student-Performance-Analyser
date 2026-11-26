package com.studentanalytics.servlet;

import com.studentanalytics.dao.DataAccessException;
import com.studentanalytics.dao.Database;
import com.studentanalytics.dao.JdbcStudentDao;
import com.studentanalytics.dao.StudentDao;
import com.studentanalytics.model.Student;
import com.studentanalytics.service.AnalyticsService;
import com.studentanalytics.service.RankingService;
import com.studentanalytics.util.GradeCalculator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Student-only, read-only view of the signed-in student's own details and marks.
 * AuthFilter only lets accounts with the STUDENT role reach this servlet.
 *
 * The record is looked up from the email stored in the session, never from a request
 * parameter, so a student cannot ask for anybody else's data. There is no POST handler:
 * students cannot change anything.
 */
@WebServlet(name = "PortalServlet", urlPatterns = "/portal")
public class PortalServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/views/portal.jsp";

    private final transient GradeCalculator gradeCalculator = new GradeCalculator();
    private final transient AnalyticsService analyticsService = new AnalyticsService();
    private final transient RankingService rankingService = new RankingService();
    private transient StudentDao dao;

    @Override
    public void init() throws ServletException {
        try {
            Database.initSchema();
            dao = new JdbcStudentDao();
        } catch (DataAccessException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        String email = session == null ? null : (String) session.getAttribute(AuthServlet.USER_EMAIL);
        try {
            Student me = dao.findByEmail(email);
            if (me != null) {
                // Rank against classmates (same teacher and section). Only numbers are shown,
                // never other students' names or marks.
                List<Student> peers = new ArrayList<>();
                for (Student s : dao.findAll(me.getOwnerId())) {
                    if (Objects.equals(s.getClassId(), me.getClassId())) {
                        peers.add(s);
                    }
                }
                gradeCalculator.assignGrades(peers);
                rankingService.rankStudents(peers);
                for (Student s : peers) {
                    if (s.getId().equalsIgnoreCase(me.getId())) {
                        me = s;
                        break;
                    }
                }
                req.setAttribute("groupSize", peers.size());
                req.setAttribute("groupAverage", analyticsService.calculateClassAverage(peers));
                req.setAttribute("highestSubject", analyticsService.getHighestSubject(me));
                req.setAttribute("lowestSubject", analyticsService.getLowestSubject(me));
            }
            req.setAttribute("student", me);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        } catch (DataAccessException e) {
            log("Database error during GET /portal", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error. Check that MySQL is running and see the server log.");
        }
    }
}
