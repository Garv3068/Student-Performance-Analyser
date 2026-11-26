<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, com.studentanalytics.model.*, com.studentanalytics.util.ViewUtil" %>
<%
    String ctx = request.getContextPath();
    String navActive = "new";
    String error = (String) request.getAttribute("error");
    String idValue = request.getParameter("id");
    String nameValue = request.getParameter("name");
    String emailValue = request.getParameter("email");
    @SuppressWarnings("unchecked")
    List<SchoolClass> sections = (List<SchoolClass>) request.getAttribute("classes");
    String chosenClass = (String) request.getAttribute("defaultClassId");
    if (sections == null) sections = new ArrayList<SchoolClass>();
    // Values submitted before an error, so the person does not have to retype them
    String[] subjectNames = request.getParameterValues("subjectName");
    String[] marks = request.getParameterValues("marks");
    String[] maxes = request.getParameterValues("maxMarks");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Add student | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1><a href="<%= ctx %>/">Student Performance Analyser</a></h1>
      <p>Add a student</p>
    </div>
    <%@ include file="nav-teacher.jspf" %>
  </header>

  <main>
    <section class="sheet" aria-labelledby="addTitle">
      <h2 id="addTitle">Student and marks</h2>

      <% if (error != null) { %>
        <div class="alert" role="alert"><%= ViewUtil.esc(error) %></div>
      <% } %>
      <div id="formError" class="alert" role="alert" hidden></div>

      <form id="studentForm" method="post" action="<%= ctx %>/students">
        <input type="hidden" name="action" value="add">

        <div class="field-row">
          <div>
            <label for="id">Student ID</label>
            <input id="id" name="id" maxlength="20" required value="<%= ViewUtil.esc(idValue) %>">
          </div>
          <div>
            <label for="name">Student name</label>
            <input id="name" name="name" maxlength="60" required value="<%= ViewUtil.esc(nameValue) %>">
          </div>
        </div>

        <div class="field-row two">
          <div>
            <label for="email">Student email (optional)</label>
            <input id="email" name="email" type="email" maxlength="254" autocomplete="off"
                   value="<%= ViewUtil.esc(emailValue) %>" aria-describedby="emailHelp">
            <small id="emailHelp" class="meta">Lets the student sign up and see their own marks. Leave blank for no login.</small>
          </div>
          <div>
            <label for="classId">Section</label>
            <select id="classId" name="classId">
              <option value="">No section</option>
              <% for (SchoolClass sc : sections) { %>
                <option value="<%= sc.getId() %>"<%= String.valueOf(sc.getId()).equals(chosenClass) ? " selected" : "" %>><%= ViewUtil.esc(sc.getName()) %></option>
              <% } %>
            </select>
            <% if (sections.isEmpty()) { %><small class="meta"><a href="<%= ctx %>/classes">Create a section</a> first to assign one.</small><% } %>
          </div>
        </div>

        <div class="subject-head" aria-hidden="true">
          <span>Subject</span><span>Marks</span><span>Out of</span><span></span>
        </div>
        <div id="subjectRows">
          <% if (subjectNames != null) {
               for (int i = 0; i < subjectNames.length; i++) { %>
            <div class="subject-row">
              <input type="text" name="subjectName" aria-label="Subject name" maxlength="40" value="<%= ViewUtil.esc(subjectNames[i]) %>">
              <input type="number" name="marks" aria-label="Marks obtained" min="0" step="0.5" value="<%= ViewUtil.esc(marks != null && i < marks.length ? marks[i] : "") %>">
              <input type="number" name="maxMarks" aria-label="Maximum marks" min="1" step="0.5" value="<%= ViewUtil.esc(maxes != null && i < maxes.length ? maxes[i] : "100") %>">
              <button type="button" class="btn small" data-remove>Remove</button>
            </div>
          <%   }
             } %>
        </div>
        <noscript><p class="empty">Turn on JavaScript to add subject rows, or add subjects later from the student's page.</p></noscript>

        <div class="actions">
          <button type="button" id="addSubject" class="btn secondary">Add subject</button>
          <button type="submit" class="btn">Save student</button>
          <a class="btn secondary" href="<%= ctx %>/students">Cancel</a>
        </div>
      </form>
    </section>
  </main>

  <script src="<%= ctx %>/js/app.js"></script>
  <script src="<%= ctx %>/js/session.js"></script>
</body>
</html>
