package com.studentanalytics.servlet;

import com.studentanalytics.model.Student;
import com.studentanalytics.model.Subject;
import com.studentanalytics.model.SubjectStats;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Front controller for the Student Analytics tool.
 *
 * GET  /students                    -> ranked list + class summary   (students.jsp)
 * GET  /students?action=view&id=X   -> one student's details         (student-detail.jsp)
 * GET  /students?action=new         -> add-student form              (student-form.jsp)
 * GET  /students?action=stats       -> subject-wise statistics       (subject-stats.jsp)
 *
 * POST /students  action=add        -> add a student (with optional subjects)
 * POST /students  action=addSubject -> add a subject to an existing student
 * POST /students  action=delete     -> remove a student
 *
 * Students are kept in memory for the life of the servlet. Every access to the
 * map goes through the {@code lock}, because Student objects are mutable
 * (grade and rank are written during ranking).
 *
 * Uses the jakarta.servlet namespace (Servlet 5+, Tomcat 10+). For older
 * containers (Tomcat 9 and below) replace "jakarta.servlet" with "javax.servlet".
 */
@WebServlet(name = "StudentServlet", urlPatterns = "/students")
public class StudentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VIEW_LIST = "/WEB-INF/views/students.jsp";
    private static final String VIEW_DETAIL = "/WEB-INF/views/student-detail.jsp";
    private static final String VIEW_FORM = "/WEB-INF/views/student-form.jsp";
    private static final String VIEW_STATS = "/WEB-INF/views/subject-stats.jsp";

    private static final String FLASH = "flashMessage";

    private final transient GradeCalculator gradeCalculator = new GradeCalculator();
    private final transient AnalyticsService analyticsService = new AnalyticsService();
    private final transient RankingService rankingService = new RankingService();

    private final transient Object lock = new Object();
    private final transient Map<String, Student> students = new LinkedHashMap<>();

    // ---------- GET ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }
        moveFlashToRequest(req);

        switch (action) {
            case "list":
                showList(req, resp);
                break;
            case "view":
                showStudent(req, resp);
                break;
            case "new":
                req.getRequestDispatcher(VIEW_FORM).forward(req, resp);
                break;
            case "stats":
                showStats(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        synchronized (lock) {
            List<Student> all = new ArrayList<>(students.values());
            gradeCalculator.assignGrades(all);
            List<Student> ranked = rankingService.rankStudents(all);

            req.setAttribute("students", ranked);
            req.setAttribute("studentCount", ranked.size());
            req.setAttribute("classAverage", analyticsService.calculateClassAverage(all));
            req.setAttribute("passPercentage", analyticsService.calculatePassPercentage(all));
            req.setAttribute("toppers", rankingService.getToppers(all));
            req.setAttribute("lowestScorer", analyticsService.findLowestScorer(all));
        }
        req.getRequestDispatcher(VIEW_LIST).forward(req, resp);
    }

    private void showStudent(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String id = req.getParameter("id");
        synchronized (lock) {
            Student student = find(id);
            if (student == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
                return;
            }
            List<Student> all = new ArrayList<>(students.values());
            gradeCalculator.assignGrades(all);
            rankingService.rankStudents(all);

            req.setAttribute("student", student);
            req.setAttribute("highestSubject", analyticsService.getHighestSubject(student));
            req.setAttribute("lowestSubject", analyticsService.getLowestSubject(student));
            req.setAttribute("classSize", all.size());
        }
        req.getRequestDispatcher(VIEW_DETAIL).forward(req, resp);
    }

    private void showStats(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        synchronized (lock) {
            List<Student> all = new ArrayList<>(students.values());
            List<SubjectStats> stats = analyticsService.getSubjectStatsList(all);

            req.setAttribute("subjectStats", stats);
            req.setAttribute("bestSubject", analyticsService.findBestSubject(all));
            req.setAttribute("weakestSubject", analyticsService.findWeakestSubject(all));
        }
        req.getRequestDispatcher(VIEW_STATS).forward(req, resp);
    }

    // ---------- POST ----------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing action");
            return;
        }

        switch (action) {
            case "add":
                addStudent(req, resp);
                break;
            case "addSubject":
                addSubject(req, resp);
                break;
            case "delete":
                deleteStudent(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
        }
    }

    /**
     * Form fields: id, name, and optionally parallel arrays
     * subjectName[], marks[], maxMarks[] (maxMarks may be blank for the default of 100).
     */
    private void addStudent(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Student student = new Student(req.getParameter("id"), req.getParameter("name"));

            String[] names = req.getParameterValues("subjectName");
            String[] marks = req.getParameterValues("marks");
            String[] maxes = req.getParameterValues("maxMarks");
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    if (isBlank(names[i]) && (marks == null || i >= marks.length || isBlank(marks[i]))) {
                        continue; // skip completely empty rows in the form
                    }
                    String mark = (marks != null && i < marks.length) ? marks[i] : null;
                    String max = (maxes != null && i < maxes.length) ? maxes[i] : null;
                    student.addSubject(buildSubject(names[i], mark, max));
                }
            }

            synchronized (lock) {
                String key = keyOf(student.getId());
                if (students.containsKey(key)) {
                    throw new IllegalArgumentException(
                            "A student with ID " + student.getId() + " already exists");
                }
                students.put(key, student);
            }
            flash(req, "Added student " + student.getName());
            resp.sendRedirect(req.getContextPath() + "/students");

        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher(VIEW_FORM).forward(req, resp); // keeps submitted values
        }
    }

    private void addSubject(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String id = req.getParameter("id");
        try {
            Subject subject = buildSubject(req.getParameter("subjectName"),
                    req.getParameter("marks"), req.getParameter("maxMarks"));
            synchronized (lock) {
                Student student = find(id);
                if (student == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
                    return;
                }
                student.addSubject(subject);
            }
            flash(req, "Added " + subject.getName());
        } catch (IllegalArgumentException e) {
            flash(req, "Could not add subject: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/students?action=view&id=" + encode(id));
    }

    private void deleteStudent(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        Student removed;
        synchronized (lock) {
            removed = id == null ? null : students.remove(keyOf(id));
        }
        flash(req, removed == null ? "No student with ID " + id
                                   : "Removed student " + removed.getName());
        resp.sendRedirect(req.getContextPath() + "/students");
    }

    // ---------- Helpers ----------

    /** Must be called while holding the lock. */
    private Student find(String id) {
        return isBlank(id) ? null : students.get(keyOf(id));
    }

    private static String keyOf(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Parses form text into a Subject; max marks defaults to 100 when blank. */
    private static Subject buildSubject(String name, String marksText, String maxText) {
        double marks = parseNumber(marksText, "Marks");
        if (isBlank(maxText)) {
            return new Subject(name, marks);
        }
        return new Subject(name, marks, parseNumber(maxText, "Maximum marks"));
    }

    private static double parseNumber(String text, String label) {
        if (isBlank(text)) {
            throw new IllegalArgumentException(label + " is required");
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number, got '" + text + "'");
        }
    }

    private static String encode(String value) {
        return value == null ? "" : java.net.URLEncoder.encode(
                value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Stores a one-time message that survives the redirect. */
    private static void flash(HttpServletRequest req, String message) {
        req.getSession().setAttribute(FLASH, message);
    }

    private static void moveFlashToRequest(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute(FLASH) != null) {
            req.setAttribute("message", session.getAttribute(FLASH));
            session.removeAttribute(FLASH);
        }
    }
}