// "Sign out" and "Switch workspace" links in the top bar.
// Both end the current session on the server; then Sign out opens the front page and
// Switch workspace opens the sign-in page so another account can be used.
(function () {
  function endSession(link) {
    return fetch(link.getAttribute('data-auth'), {
      method: 'POST', credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=signout'
    }).then(function (r) { if (!r.ok) throw new Error(r.status); });
  }

  document.querySelectorAll('[data-signout], [data-switch]').forEach(function (link) {
    link.addEventListener('click', function (e) {
      e.preventDefault();
      endSession(link)
        .then(function () { window.location.href = link.href; })
        .catch(function () { window.alert('Could not sign out. Please try again.'); });
    });
  });
})();
