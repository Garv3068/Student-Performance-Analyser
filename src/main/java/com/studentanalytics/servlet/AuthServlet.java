package com.studentanalytics.servlet;

import com.studentanalytics.dao.DataAccessException;
import com.studentanalytics.dao.Database;
import com.studentanalytics.dao.JdbcStudentDao;
import com.studentanalytics.dao.StudentDao;
import com.studentanalytics.dao.UserDao;
import com.studentanalytics.model.Role;
import com.studentanalytics.model.User;
import com.studentanalytics.util.PasswordHasher;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Sign-in / sign-up endpoint used by auth.html. Always answers with JSON.
 *
 * GET  /auth?action=session  -> {"authenticated":true|false, "name":..., "email":..., "role":..., "redirect":...}
 * POST /auth  action=signup  (role, name, email, password, confirm)
 * POST /auth  action=signin  (role, email, password)
 * POST /auth  action=signout
 */
@WebServlet(name = "AuthServlet", urlPatterns = "/auth")
public class AuthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final String USER_ID = "authUserId";
    static final String USER_NAME = "authUserName";
    static final String USER_EMAIL = "authUserEmail";
    static final String USER_ROLE = "authUserRole";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MS = 5 * 60 * 1000L;

    /** email -> {failed attempts, locked-until millis}. Slows down password guessing. */
    private final transient ConcurrentHashMap<String, long[]> failures = new ConcurrentHashMap<>();
    private transient UserDao users;
    private transient StudentDao students;

    @Override
    public void init() throws ServletException {
        try {
            Database.initSchema();
            users = new UserDao();
            students = new JdbcStudentDao();
        } catch (DataAccessException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"session".equals(req.getParameter("action"))) {
            send(resp, 404, "{\"ok\":false,\"error\":\"Not found.\"}");
            return;
        }
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute(USER_ID) != null) {
            Role role = (Role) s.getAttribute(USER_ROLE);
            send(resp, 200, "{\"authenticated\":true,\"name\":" + q((String) s.getAttribute(USER_NAME))
                    + ",\"email\":" + q((String) s.getAttribute(USER_EMAIL))
                    + ",\"role\":" + q(role == null ? "" : role.name())
                    + ",\"redirect\":" + q(role == null ? "" : role.getHome()) + "}");
        } else {
            send(resp, 200, "{\"authenticated\":false}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if (!sameOrigin(req)) {
            send(resp, 403, error(null, "Request blocked. Please reload the page and try again."));
            return;
        }
        String action = req.getParameter("action");
        try {
            if ("signup".equals(action)) signUp(req, resp);
            else if ("signin".equals(action)) signIn(req, resp);
            else if ("signout".equals(action)) signOut(req, resp);
            else send(resp, 400, error(null, "Unknown action."));
        } catch (DataAccessException e) {
            getServletContext().log("Auth database error", e);
            send(resp, 500, error(null, "We could not reach the database. Please try again shortly."));
        }
    }

    private void signUp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = trim(req.getParameter("name"));
        String email = trim(req.getParameter("email")).toLowerCase(Locale.ROOT);
        String password = param(req, "password");
        String confirm = param(req, "confirm");
        Role role = Role.parse(req.getParameter("role"));

        if (role == null) {
            send(resp, 400, error(null, "Choose whether you are signing up as a teacher or a student."));
        } else if (name.length() < 2 || name.length() > 100) {
            send(resp, 400, error("name", "Enter your name (2 to 100 characters)."));
        } else if (!validEmail(email)) {
            send(resp, 400, error("email", "Enter a valid email address."));
        } else if (password.length() < 8 || password.length() > 128
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            send(resp, 400, error("password", "Use at least 8 characters with a letter and a number."));
        } else if (!password.equals(confirm)) {
            send(resp, 400, error("confirm", "The two passwords do not match."));
        } else if (role == Role.STUDENT && !students.emailRegistered(email)) {
            // A student can only open a record a teacher has already created for that email.
            send(resp, 403, error("email", "No student record uses this email yet. "
                    + "Ask your teacher to add you with this email, then sign up again."));
        } else {
            User user = users.create(name, email, PasswordHasher.hash(password), role);
            if (user == null) {
                send(resp, 409, error("email", "That email is already registered. Try signing in."));
            } else {
                startSession(req, user);
                send(resp, 201, ok(user));
            }
        }
    }

    private void signIn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = trim(req.getParameter("email")).toLowerCase(Locale.ROOT);
        String password = param(req, "password");
        Role role = Role.parse(req.getParameter("role"));
        if (role == null) {
            send(resp, 400, error(null, "Choose whether you are signing in as a teacher or a student."));
            return;
        }

        long[] f = failures.get(email);
        if (f != null && f[0] >= MAX_FAILURES && System.currentTimeMillis() < f[1]) {
            send(resp, 429, error(null, "Too many attempts. Please wait a few minutes and try again."));
            return;
        }
        User user = validEmail(email) ? users.findByEmail(email) : null;
        boolean good = PasswordHasher.verify(password, user == null ? null : user.getPasswordHash());
        if (!good || user == null) {
            failures.compute(email, (k, old) ->
                    new long[] { old == null ? 1 : old[0] + 1, System.currentTimeMillis() + LOCK_MS });
            if (failures.size() > 5000) failures.clear();
            send(resp, 401, error("password", "Incorrect email or password."));
            return;
        }
        failures.remove(email);
        if (user.getRole() != role) {
            // Password was right, but the account belongs to the other role. No session is started.
            send(resp, 403, error(null, "This is a " + user.getRole().name().toLowerCase(Locale.ROOT)
                    + " account. Choose \"" + capitalize(user.getRole()) + "\" above to sign in."));
            return;
        }
        startSession(req, user);
        send(resp, 200, ok(user));
    }

    private void signOut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        send(resp, 200, "{\"ok\":true}");
    }

    // ---------- helpers ----------

    /** A fresh session on every sign-in prevents session fixation. */
    private void startSession(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession s = req.getSession(true);
        s.setAttribute(USER_ID, user.getId());
        s.setAttribute(USER_NAME, user.getName());
        s.setAttribute(USER_EMAIL, user.getEmail());
        s.setAttribute(USER_ROLE, user.getRole());
    }

    private static String capitalize(Role role) {
        String n = role.name();
        return n.charAt(0) + n.substring(1).toLowerCase(Locale.ROOT);
    }

    /** Rejects cross-site posts: the Origin header, when present, must match this host. */
    private boolean sameOrigin(HttpServletRequest req) {
        String origin = req.getHeader("Origin");
        if (origin == null || origin.isEmpty()) return true;
        String host = req.getHeader("Host");
        return host != null && origin.endsWith("://" + host);
    }

    private static boolean validEmail(String email) {
        return email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    private static String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static String ok(User u) {
        return "{\"ok\":true,\"name\":" + q(u.getName()) + ",\"role\":" + q(u.getRole().name())
                + ",\"redirect\":" + q(u.getRole().getHome()) + "}";
    }

    private static String error(String field, String message) {
        return "{\"ok\":false,\"field\":" + (field == null ? "null" : q(field)) + ",\"error\":" + q(message) + "}";
    }

    /** Quotes and escapes a string as a JSON value. */
    private static String q(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : (s == null ? "" : s).toCharArray()) {
            switch (c) {
                case 34: b.append("\\\""); break;
                case 92: b.append("\\\\"); break;
                case 10: b.append("\\n"); break;
                case 13: b.append("\\r"); break;
                case 9: b.append("\\t"); break;
                default:
                    if (c < 0x20 || c == 60 || c == 62) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append((char) 34).toString();
    }

    private static void send(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(json);
    }
}
