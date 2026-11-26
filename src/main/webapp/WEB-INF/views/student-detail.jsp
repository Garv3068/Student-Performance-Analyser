<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, com.studentanalytics.model.*, com.studentanalytics.util.ViewUtil" %>
<%
    String ctx = request.getContextPath();
    String navActive = "report";
    Student student = (Student) request.getAttribute("student");
    Subject highest = (Subject) request.getAttribute("highestSubject");
    Subject lowest = (Subject) request.getAttribute("lowestSubject");
    Integer classSize = (Integer) request.getAttribute("classSize");
    String message = (String) request.getAttribute("message");
    @SuppressWarnings("unchecked")
    List<SchoolClass> sections = (List<SchoolClass>) request.getAttribute("classes");
    if (sections == null) sections = new ArrayList<SchoolClass>();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><%= ViewUtil.esc(student.getName()) %> | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1><a href="<%= ctx %>/">Student Performance Analyser</a></h1>
      <p>Student details</p>
    </div>
    <%@ include file="nav-teacher.jspf" %>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= ViewUtil.esc(message) %></div>
    <% } %>

    <section class="sheet" aria-labelledby="studentName">
      <div class="detail-head">
        <div>
          <h2 id="studentName"><%= ViewUtil.esc(student.getName()) %></h2>
          <p class="meta">ID <%= ViewUtil.esc(student.getId()) %>
            &middot; <%= student.getClassName() == null ? "No section" : "Section " + ViewUtil.esc(student.getClassName()) %>
            <% if (student.getEmail() != null) { %> &middot; <%= ViewUtil.esc(student.getEmail()) %><% } %>
            <% if (classSize != null) { %> &middot; Rank <%= student.getRank() %> of <%= classSize %><% } %></p>
        </div>
        <% if (!student.getSubjects().isEmpty()) { %>
          <span class="grade <%= ViewUtil.gradeClass(student.getGrade()) %>" aria-label="Grade <%= ViewUtil.esc(student.getGrade()) %>"><%= ViewUtil.esc(student.getGrade()) %></span>
        <% } %>
      </div>
    </section>

    <div class="summary">
      <div class="stat">
        <div class="label">Total marks</div>
        <div class="value"><%= ViewUtil.num(student.getTotalMarks()) %> / <%= ViewUtil.num(student.getMaxTotalMarks()) %></div>
      </div>
      <div class="stat">
        <div class="label">Percentage</div>
        <div class="value"><%= ViewUtil.pct(student.getPercentage()) %></div>
      </div>
      <% if (highest != null) { %>
      <div class="stat">
        <div class="label">Strongest subject</div>
        <div class="value"><%= ViewUtil.esc(highest.getName()) %></div>
        <div class="detail"><%= ViewUtil.pct(highest.getPercentage()) %></div>
      </div>
      <% } %>
      <% if (lowest != null) { %>
      <div class="stat">
        <div class="label">Weakest subject</div>
        <div class="value"><%= ViewUtil.esc(lowest.getName()) %></div>
        <div class="detail"><%= ViewUtil.pct(lowest.getPercentage()) %></div>
      </div>
      <% } %>
    </div>

    <section class="sheet" aria-labelledby="subjTitle">
      <h2 id="subjTitle">Subject marks</h2>
      <% if (student.getSubjects().isEmpty()) { %>
        <p class="empty">No subjects yet. Add the first one below.</p>
      <% } else { %>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Subject</th><th class="num">Marks</th><th class="num">Out of</th><th class="num">Percentage</th><th>Result</th><th>Edit</th></tr>
            </thead>
            <tbody>
              <% int rowNo = 0; for (Subject sub : student.getSubjects()) { rowNo++; %>
                <tr>
                  <td>
                    <%= ViewUtil.esc(sub.getName()) %>
                    <form id="upd<%= rowNo %>" method="post" action="<%= ctx %>/students">
                      <input type="hidden" name="action" value="updateSubject">
                      <input type="hidden" name="id" value="<%= ViewUtil.esc(student.getId()) %>">
                      <input type="hidden" name="subjectName" value="<%= ViewUtil.esc(sub.getName()) %>">
                    </form>
                  </td>
                  <td class="num"><input form="upd<%= rowNo %>" name="marks" type="number" min="0" step="0.5" required
                        value="<%= ViewUtil.num(sub.getMarksObtained()) %>" aria-label="Marks for <%= ViewUtil.esc(sub.getName()) %>" style="width:6rem"></td>
                  <td class="num"><input form="upd<%= rowNo %>" name="maxMarks" type="number" min="1" step="0.5" required
                        value="<%= ViewUtil.num(sub.getMaxMarks()) %>" aria-label="Maximum marks for <%= ViewUtil.esc(sub.getName()) %>" style="width:6rem"></td>
                  <td class="num <%= sub.isPassed() ? "" : "low" %>"><%= ViewUtil.pct(sub.getPercentage()) %></td>
                  <td><span class="<%= sub.isPassed() ? "status-pass" : "status-fail" %>"><%= sub.isPassed() ? "Pass" : "Fail" %></span></td>
                  <td>
                    <div class="row-actions">
                      <button type="submit" form="upd<%= rowNo %>" class="btn small">Save</button>
                      <form class="inline-form" method="post" action="<%= ctx %>/students"
                            onsubmit="return confirm('Remove <%= ViewUtil.esc(sub.getName()).replace("'", "\\'") %>?');">
                        <input type="hidden" name="action" value="deleteSubject">
                        <input type="hidden" name="id" value="<%= ViewUtil.esc(student.getId()) %>">
                        <input type="hidden" name="subjectName" value="<%= ViewUtil.esc(sub.getName()) %>">
                        <button type="submit" class="btn small danger">Remove</button>
                      </form>
                    </div>
                  </td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
      <% } %>
    </section>

    <section class="sheet" aria-labelledby="detailsTitle">
      <h2 id="detailsTitle">Edit student details</h2>
      <form method="post" action="<%= ctx %>/students" class="field-row details">
        <input type="hidden" name="action" value="updateDetails">
        <input type="hidden" name="id" value="<%= ViewUtil.esc(student.getId()) %>">
        <div>
          <label for="studentNameInput">Name</label>
          <input id="studentNameInput" name="name" maxlength="60" required value="<%= ViewUtil.esc(student.getName()) %>">
        </div>
        <div>
          <label for="studentEmailInput">Email</label>
          <input id="studentEmailInput" name="email" type="email" maxlength="254" autocomplete="off"
                 value="<%= ViewUtil.esc(student.getEmail()) %>">
        </div>
        <div>
          <label for="studentClassInput">Section</label>
          <select id="studentClassInput" name="classId">
            <option value="">No section</option>
            <% for (SchoolClass sc : sections) { %>
              <option value="<%= sc.getId() %>"<%= student.getClassId() != null && student.getClassId() == sc.getId() ? " selected" : "" %>><%= ViewUtil.esc(sc.getName()) %></option>
            <% } %>
          </select>
        </div>
        <button type="submit" class="btn">Save details</button>
      </form>
    </section>

    <section class="sheet" aria-labelledby="addSubjTitle">
      <h2 id="addSubjTitle">Add a subject</h2>
      <form method="post" action="<%= ctx %>/students" class="add-subject">
        <input type="hidden" name="action" value="addSubject">
        <input type="hidden" name="id" value="<%= ViewUtil.esc(student.getId()) %>">
        <div>
          <label for="subjectName">Subject</label>
          <input id="subjectName" name="subjectName" maxlength="40" required>
        </div>
        <div>
          <label for="marks">Marks</label>
          <input id="marks" name="marks" type="number" min="0" step="0.5" required>
        </div>
        <div>
          <label for="maxMarks">Out of</label>
          <input id="maxMarks" name="maxMarks" type="number" min="1" step="0.5" value="100">
        </div>
        <button type="submit" class="btn">Add subject</button>
      </form>
    </section>

    <div class="actions">
      <a class="btn secondary" href="<%= ctx %>/students">Back to class report</a>
      <form class="inline-form" method="post" action="<%= ctx %>/students"
            onsubmit="return confirm('Remove <%= ViewUtil.esc(student.getName()).replace("'", "\\'") %>?');">
        <input type="hidden" name="action" value="delete">
        <input type="hidden" name="id" value="<%= ViewUtil.esc(student.getId()) %>">
        <button type="submit" class="btn danger">Remove student</button>
      </form>
    </div>
  </main>
  <script src="<%= ctx %>/js/session.js"></script>
</body>
</html>
