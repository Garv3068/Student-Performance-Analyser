package com.studentanalytics.servlet;

import com.studentanalytics.dao.ClassDao;
import com.studentanalytics.dao.DataAccessException;
import com.studentanalytics.dao.Database;
import com.studentanalytics.dao.JdbcStudentDao;
import com.studentanalytics.dao.StudentDao;
import com.studentanalytics.dao.UserDao;
import com.studentanalytics.model.SchoolClass;
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
import java.util.List;
import java.util.Objects;

/**
 * Front controller for the Student Analytics tool.
 *
 * Teacher-only: AuthFilter lets only accounts with the TEACHER role reach this servlet.
 * Every query is scoped to the signed-in teacher's own sections and students.
 *
 * GET  /students                    -> ranked list + class summary   (students.jsp)
 * GET  /students?classId=N|none     -> the same, for one section / unassigned students
 * GET  /students?action=view&id=X   -> one student's details         (student-detail.jsp)
 * GET  /students?action=new         -> add-student form              (student-form.jsp)
 * GET  /students?action=stats       -> subject-wise statistics       (subject-stats.jsp)
 *
 * POST /students  action=add           -> add a student (with optional subjects)
 * POST /students  action=addSubject    -> add a subject to an existing student
 * POST /students  action=delete        -> remove a student
 * POST /students  action=rename        -> change a student's name
 * POST /students  action=updateDetails -> change a student's name, email and section
 * POST /students  action=updateSubject -> change marks / max marks of a subject
 * POST /students  action=deleteSubject -> remove one subject from a student
 *
 * Data is stored in MySQL through {@link StudentDao}. Every request loads fresh Student
 * objects, so grades and ranks written during ranking are never shared between requests.
 * Rules such as "no duplicate subject" live in the model; the database also enforces them
 * with unique keys, which covers two requests arriving at the same moment.
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

    private transient StudentDao dao;
    private transient ClassDao classDao;
    private transient UserDao userDao;

    @Override
    public void init() throws ServletException {
        try {
            Database.initSchema();
            dao = new JdbcStudentDao();
            classDao = new ClassDao();
            userDao = new UserDao();
        } catch (DataAccessException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    // ---------- GET ----------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }
        moveFlashToRequest(req);

        try {
            switch (action) {
                case "list":
                    showList(req, resp);
                    break;
                case "view":
                    showStudent(req, resp);
                    break;
                case "new":
                    req.setAttribute("classes", classDao.findAll(uid(req)));
                    req.setAttribute("defaultClassId", req.getParameter("classId"));
                    req.getRequestDispatcher(VIEW_FORM).forward(req, resp);
                    break;
                case "stats":
                    showStats(req, resp);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
            }
        } catch (DataAccessException e) {
            log("Database error during GET " + action, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error. Check that MySQL is running and see the server log.");
        }
    }

    private void showList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long uid = uid(req);
        List<SchoolClass> sections = classDao.findAll(uid);
        List<Student> all = applyClassFilter(dao.findAll(uid), req.getParameter("classId"), sections, req);
        gradeCalculator.assignGrades(all);
        List<Student> ranked = rankingService.rankStudents(all);

        req.setAttribute("classes", sections);
        req.setAttribute("students", ranked);
        req.setAttribute("studentCount", ranked.size());
        req.setAttribute("classAverage", analyticsService.calculateClassAverage(all));
        req.setAttribute("passPercentage", analyticsService.calculatePassPercentage(all));
        req.setAttribute("toppers", rankingService.getToppers(all));
        req.setAttribute("lowestScorer", analyticsService.findLowestScorer(all));
        req.getRequestDispatcher(VIEW_LIST).forward(req, resp);
    }

    private void showStudent(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String id = req.getParameter("id");
        List<Student> all = dao.findAll(uid(req));
        gradeCalculator.assignGrades(all);
        rankingService.rankStudents(all);

        Student student = findIn(all, id);
        if (student == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
            return;
        }

        // Rank against the student's own section (or against the unassigned students).
        List<Student> group = new ArrayList<>();
        for (Student s : all) {
            if (Objects.equals(s.getClassId(), student.getClassId())) {
                group.add(s);
            }
        }
        rankingService.rankStudents(group);
        all = group;

        req.setAttribute("student", student);
        req.setAttribute("highestSubject", analyticsService.getHighestSubject(student));
        req.setAttribute("lowestSubject", analyticsService.getLowestSubject(student));
        req.setAttribute("classSize", all.size());
        req.setAttribute("classes", classDao.findAll(uid(req)));
        req.getRequestDispatcher(VIEW_DETAIL).forward(req, resp);
    }

    private void showStats(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long uid = uid(req);
        List<SchoolClass> sections = classDao.findAll(uid);
        List<Student> all = applyClassFilter(dao.findAll(uid), req.getParameter("classId"), sections, req);
        List<SubjectStats> stats = analyticsService.getSubjectStatsList(all);

        req.setAttribute("classes", sections);
        req.setAttribute("subjectStats", stats);
        req.setAttribute("bestSubject", analyticsService.findBestSubject(all));
        req.setAttribute("weakestSubject", analyticsService.findWeakestSubject(all));
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

        try {
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
                case "rename":
                    renameStudent(req, resp);
                    break;
                case "updateDetails":
                    updateDetails(req, resp);
                    break;
                case "updateSubject":
                    updateSubject(req, resp);
                    break;
                case "deleteSubject":
                    deleteSubject(req, resp);
                    break;
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
            }
        } catch (DataAccessException e) {
            log("Database error during POST " + action, e);
            flash(req, "Database error. Nothing was saved. Check that MySQL is running.");
            resp.sendRedirect(req.getContextPath() + "/students");
        }
    }

    /**
     * Form fields: id, name, and optionally parallel arrays
     * subjectName[], marks[], maxMarks[] (maxMarks may be blank for the default of 100).
     */
    private void addStudent(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            long uid = uid(req);
            Student student = new Student(req.getParameter("id"), req.getParameter("name"));
            student.setEmail(req.getParameter("email"));
            SchoolClass section = resolveSection(uid, req.getParameter("classId"));
            if (section != null) {
                student.setSection(section.getId(), section.getName());
            }

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

            dao.insert(uid, student); // throws IllegalArgumentException if the ID or email already exists
            flash(req, "Added student " + student.getName());
            resp.sendRedirect(req.getContextPath() + "/students"
                    + (section == null ? "" : "?classId=" + section.getId()));

        } catch (IllegalArgumentException e) {
            req.setAttribute("classes", classDao.findAll(uid(req)));
            req.setAttribute("defaultClassId", req.getParameter("classId"));
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher(VIEW_FORM).forward(req, resp); // keeps submitted values
        }
    }

    private void addSubject(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        try {
            Subject subject = buildSubject(req.getParameter("subjectName"),
                    req.getParameter("marks"), req.getParameter("maxMarks"));
            Student student = dao.findById(uid(req), id);
            if (student == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
                return;
            }
            student.addSubject(subject);                  // rejects duplicates
            dao.addSubject(uid(req), student.getId(), subject);
            flash(req, "Added " + subject.getName());
        } catch (IllegalArgumentException e) {
            flash(req, "Could not add subject: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/students?action=view&id=" + encode(id));
    }

    private void deleteStudent(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        Student existing = dao.findById(uid(req), id);
        if (existing != null) {
            dao.delete(uid(req), existing.getId());
        }
        flash(req, existing == null ? "No student with ID " + id
                                    : "Removed student " + existing.getName());
        resp.sendRedirect(req.getContextPath() + "/students");
    }

    private void renameStudent(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        try {
            Student student = dao.findById(uid(req), id);
            if (student == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
                return;
            }
            student.setName(req.getParameter("name"));    // validates
            dao.updateName(uid(req), student.getId(), student.getName());
            flash(req, "Updated name to " + student.getName());
        } catch (IllegalArgumentException e) {
            flash(req, "Could not update name: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/students?action=view&id=" + encode(id));
    }

    /** Changes a student's name, email and section. */
    private void updateDetails(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        try {
            long uid = uid(req);
            Student student = dao.findById(uid, id);
            if (student == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
                return;
            }
            String oldEmail = student.getEmail();
            student.setName(req.getParameter("name"));    // validates
            student.setEmail(req.getParameter("email"));  // validates
            SchoolClass section = resolveSection(uid, req.getParameter("classId"));
            if (oldEmail != null && !oldEmail.equals(student.getEmail())
                    && userDao.findByEmail(oldEmail) != null) {
                throw new IllegalArgumentException(
                        "This student already has a login with " + oldEmail + ", so the email cannot be changed");
            }
            dao.updateDetails(uid, student.getId(), student.getName(), student.getEmail(),
                    section == null ? null : section.getId());
            flash(req, "Updated details for " + student.getName());
        } catch (IllegalArgumentException e) {
            flash(req, "Could not update details: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/students?action=view&id=" + encode(id));
    }

    /** Changes marks / max marks of an existing subject (found by its name). */
    private void updateSubject(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        try {
            Subject updated = buildSubject(req.getParameter("subjectName"),
                    req.getParameter("marks"), req.getParameter("maxMarks"));
            Student student = dao.findById(uid(req), id);
            if (student == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
                return;
            }
            student.replaceSubject(updated);              // rejects unknown subjects
            dao.updateSubject(uid(req), student.getId(), updated);
            flash(req, "Updated " + updated.getName());
        } catch (IllegalArgumentException e) {
            flash(req, "Could not update subject: " + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/students?action=view&id=" + encode(id));
    }

    private void deleteSubject(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String id = req.getParameter("id");
        String subjectName = req.getParameter("subjectName");
        Student student = dao.findById(uid(req), id);
        if (student == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No student with ID " + id);
            return;
        }
        boolean removed = student.removeSubject(subjectName);
        if (removed) {
            dao.deleteSubject(uid(req), student.getId(), subjectName);
        }
        flash(req, removed ? "Removed " + subjectName.trim()
                           : "No subject named " + subjectName);
        resp.sendRedirect(req.getContextPath() + "/students?action=view&id=" + encode(id));
    }

    // ---------- Helpers ----------

    /** ID of the signed-in user. AuthFilter guarantees one is present for every /students request. */
    private static long uid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute(AuthServlet.USER_ID);
        if (!(id instanceof Number)) {
            throw new IllegalStateException("No signed-in user on this request");
        }
        return ((Number) id).longValue();
    }

    /**
     * Looks up the section named by a form field. Blank means "no section". A section that
     * does not belong to this teacher is rejected, so a forged classId cannot reach another teacher's data.
     */
    private SchoolClass resolveSection(long uid, String classIdText) {
        if (isBlank(classIdText) || "none".equals(classIdText.trim())) {
            return null;
        }
        try {
            SchoolClass sc = classDao.findById(uid, Long.parseLong(classIdText.trim()));
            if (sc == null) {
                throw new IllegalArgumentException("That section does not exist");
            }
            return sc;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("That section does not exist");
        }
    }

    /**
     * Keeps only the students in one section. filter is null/blank for everyone, "none" for
     * students without a section, or a section ID. Sets request attributes the views use.
     */
    private static List<Student> applyClassFilter(List<Student> all, String filter,
                                                  List<SchoolClass> sections, HttpServletRequest req) {
        if (isBlank(filter)) {
            return all;
        }
        List<Student> kept = new ArrayList<>();
        if ("none".equals(filter.trim())) {
            for (Student s : all) {
                if (s.getClassId() == null) kept.add(s);
            }
            req.setAttribute("filterLabel", "Unassigned students");
            req.setAttribute("filterId", "none");
            return kept;
        }
        try {
            long wanted = Long.parseLong(filter.trim());
            for (SchoolClass sc : sections) {
                if (sc.getId() == wanted) {
                    for (Student s : all) {
                        if (s.getClassId() != null && s.getClassId() == wanted) kept.add(s);
                    }
                    req.setAttribute("filterLabel", "Section " + sc.getName());
                    req.setAttribute("filterId", String.valueOf(wanted));
                    return kept;
                }
            }
        } catch (NumberFormatException ignored) {
            // fall through: show everyone
        }
        return all;
    }

    private static Student findIn(List<Student> students, String id) {
        if (isBlank(id)) {
            return null;
        }
        String wanted = id.trim();
        for (Student s : students) {
            if (s.getId().equalsIgnoreCase(wanted)) {
                return s;
            }
        }
        return null;
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
