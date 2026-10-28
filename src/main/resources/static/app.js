(() => {
  'use strict';

  const API = '/api/v1';
  const session = { token: sessionStorage.getItem('studyflow-token') || '', username: sessionStorage.getItem('studyflow-user') || '' };
  const state = { mode: 'login', view: 'dashboard', projects: [], project: null, materials: [], material: null, progress: null, recommendation: null, activity: [], mastery: [], plan: null, assessment: null, expandedLesson: null };
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
  const shortDate = value => value ? new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(value)) : 'Just now';
  const initials = name => (name || 'L').trim().slice(0, 1).toUpperCase();

  async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (session.token) headers.set('Authorization', `Basic ${session.token}`);
    if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
    const response = await fetch(`${API}${path}`, { ...options, headers });
    const text = await response.text();
    let data = null;
    if (text) { try { data = JSON.parse(text); } catch { data = text; } }
    if (!response.ok) {
      const message = data?.message || data?.detail || data?.error || (typeof data === 'string' ? data : `Request failed (${response.status})`);
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }
    return data;
  }

  function setApiStatus(online) {
    const dot = $('#api-dot');
    if (!dot) return;
    dot.classList.toggle('is-offline', !online);
    $('#api-status-label').textContent = online ? 'API connected' : 'API unavailable';
    $('.api-status').classList.toggle('is-offline', !online);
  }

  function toast(message, isError = false) {
    const item = document.createElement('div');
    item.className = `toast${isError ? ' error' : ''}`;
    item.innerHTML = `<span class="toast-mark">${isError ? '!' : '✓'}</span><span>${escapeHtml(message)}</span>`;
    $('#toast-region').append(item);
    window.setTimeout(() => item.remove(), 4200);
  }

  function showAuth(message = '') {
    $('#auth-screen').hidden = false;
    $('#app').hidden = true;
    const error = $('#auth-error');
    error.textContent = message;
    error.hidden = !message;
  }

  function saveSession(username, password) {
    session.username = username;
    session.token = btoa(unescape(encodeURIComponent(`${username}:${password}`)));
    sessionStorage.setItem('studyflow-user', username);
    sessionStorage.setItem('studyflow-token', session.token);
  }

  async function handleAuth(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const username = form.elements.username.value.trim();
    const password = form.elements.password.value;
    const button = form.querySelector('button[type="submit"]');
    const errorNode = $('#auth-error');
    button.disabled = true;
    errorNode.hidden = true;
    try {
      if (state.mode === 'register') {
        await api('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) });
      }
      saveSession(username, password);
      await api('/health');
      setApiStatus(true);
      await startWorkspace();
    } catch (error) {
      sessionStorage.removeItem('studyflow-token');
      sessionStorage.removeItem('studyflow-user');
      session.token = '';
      session.username = '';
      errorNode.textContent = state.mode === 'register' && !error.status ? 'Could not reach the service. Make sure the backend is running.' : error.message;
      errorNode.hidden = false;
    } finally { button.disabled = false; }
  }

  function authMode(mode) {
    state.mode = mode;
    $$('.auth-tab').forEach(tab => {
      const active = tab.dataset.authMode === mode;
      tab.classList.toggle('is-active', active);
      tab.setAttribute('aria-selected', String(active));
    });
    $('#auth-title').textContent = mode === 'register' ? 'Your next chapter starts here.' : 'Make every study session count.';
    $('#auth-hint').textContent = mode === 'register' ? 'Use a unique username and a password of at least 12 characters.' : 'Sign in with your learner account.';
    $('#auth-submit-label').textContent = mode === 'register' ? 'Create my account' : 'Open my workspace';
    $('#auth-password').setAttribute('autocomplete', mode === 'register' ? 'new-password' : 'current-password');
    $('#auth-password').placeholder = mode === 'register' ? 'At least 12 characters' : 'Your password';
    $('#auth-password').minLength = mode === 'register' ? 12 : 1;
    $('#auth-error').hidden = true;
  }

  async function startWorkspace() {
    $('#auth-screen').hidden = true;
    $('#app').hidden = false;
    $('#profile-name').textContent = session.username || 'Learner';
    $('#profile-avatar').textContent = initials(session.username);
    await loadProjects();
  }

  async function loadProjects() {
    renderLoading();
    try {
      state.projects = await api('/projects');
      state.project = state.projects.find(project => project.id === state.project?.id) || state.projects[0] || null;
      drawProjectList();
      if (state.project) await loadProject(state.project, false);
      else render();
    } catch (error) {
      setApiStatus(false);
      if (error.status === 401) { session.token = ''; sessionStorage.removeItem('studyflow-token'); showAuth('Sign-in failed. Check your username and password, then try again.'); }
      else { renderError(error.message); }
    }
  }

  async function loadProject(project, shouldRender = true) {
    const projectChanged = state.project?.id !== project.id;
    state.project = project;
    if (projectChanged) {
      state.material = null;
      state.plan = null;
      state.assessment = null;
    }
    const savedAssessmentId = sessionStorage.getItem(`studyflow-assessment-${project.id}`);
    if (savedAssessmentId && !state.assessment) state.assessment = { id: savedAssessmentId };
    drawProjectList();
    try {
      const [materials, progress, recommendation, activity, mastery] = await Promise.all([
        api(`/projects/${project.id}/materials`),
        api(`/projects/${project.id}/progress`),
        api(`/projects/${project.id}/recommendations`),
        api(`/projects/${project.id}/activity`),
        api(`/projects/${project.id}/mastery`)
      ]);
      state.materials = materials || [];
      state.progress = progress;
      state.recommendation = recommendation;
      state.activity = activity || [];
      state.mastery = mastery || [];
      if (recommendation?.planId) {
        try { state.plan = await api(`/learning-plans/${recommendation.planId}`); }
        catch (error) { state.plan = null; toast(error.message, true); }
      } else state.plan = null;
      setApiStatus(true);
    } catch (error) {
      if (error.status === 401) { session.token = ''; sessionStorage.removeItem('studyflow-token'); showAuth('Your sign-in expired. Please sign in again.'); return; }
      toast(error.message, true);
      state.materials = [];
    }
    if (shouldRender) render();
  }

  function drawProjectList() {
    const list = $('#project-list');
    if (!list) return;
    if (!state.projects.length) { list.innerHTML = '<div class="project-empty">Your projects will show up here.</div>'; return; }
    list.innerHTML = state.projects.slice(0, 5).map((project, index) => `
      <button class="project-item${state.project?.id === project.id ? ' is-active' : ''}" data-project-id="${escapeHtml(project.id)}" title="${escapeHtml(project.name)}">
        <span class="project-glyph">${['A','B','C','D','E'][index % 5]}</span><span class="truncate">${escapeHtml(project.name)}</span>
      </button>`).join('');
  }

  function renderLoading() {
    $('#content').innerHTML = '<div class="loading-state"><div class="skeleton loading-block"></div><div class="stats-grid">' + Array.from({ length: 4 }, () => '<div class="skeleton loading-short"></div>').join('') + '</div><div class="skeleton loading-block"></div></div>';
  }

  function renderError(message) {
    $('#content').innerHTML = `<section class="panel empty-state"><div class="empty-illustration">!</div><h2>Couldn’t load your workspace</h2><p>${escapeHtml(message)} Check that the backend and database are running, then try again.</p><button class="button button-primary" data-action="retry">Try again</button></section>`;
  }

  function render() {
    const views = { dashboard: 'Overview', library: 'Study materials', learning: 'Learning plan', reports: 'Progress' };
    $('#breadcrumb-current').textContent = views[state.view] || 'Overview';
    $$('.nav-link').forEach(button => button.classList.toggle('is-active', button.dataset.view === state.view));
    if (!state.project) { renderNoProject(); return; }
    if (state.view === 'library') renderLibrary();
    else if (state.view === 'learning') renderLearning();
    else if (state.view === 'reports') renderReports();
    else renderDashboard();
  }

  function renderNoProject() {
    $('#content').innerHTML = `<section class="panel empty-state"><div class="empty-illustration">✳</div><span class="eyebrow">A FRESH START</span><h2>Create your first learning project</h2><p>Organize a course or exam, add your study material, and AI Tutor will help you make a plan that fits.</p><button class="button button-primary" data-action="new-project">＋ &nbsp; Create a project</button></section>`;
  }

  function statCard(label, value, detail, icon, positive = false) {
    return `<article class="stat-card"><div class="stat-top"><span>${escapeHtml(label)}</span><span class="stat-icon">${icon}</span></div><div class="stat-value">${escapeHtml(value)}</div><div class="stat-foot${positive ? ' positive' : ''}">${escapeHtml(detail)}</div></article>`;
  }

  function renderDashboard() {
    const p = state.progress || {};
    const rec = state.recommendation || {};
    const firstMaterial = state.materials[0];
    const planItems = state.plan?.items || [];
    const planCount = planItems.length || Number(p.totalPlanItems || 0);
    const completed = planItems.length ? planItems.filter(item => item.status === 'COMPLETED').length : Number(p.completedPlanItems || 0);
    const progressPct = planCount ? Math.round(completed * 100 / planCount) : 0;
    const nextTitle = rec.lessonTitle || rec.topic || (rec.action === 'REVIEW_PROGRESS' ? 'Your study plan is complete' : rec.action === 'CREATE_LEARNING_PLAN' ? 'Build your personalized learning plan' : state.materials.length ? 'Create a diagnostic assessment' : 'Add your first study material');
    const nextMessage = rec.message || (state.materials.length ? 'See what you already know and build a plan around your learning goals.' : 'Upload course notes or a past paper to get your workspace started.');
    const nextAction = rec.action || (state.materials.length ? 'CREATE_ASSESSMENT' : 'ADD_MATERIAL');
    const nextButton = nextAction === 'REVIEW_PROGRESS' ? 'View progress' : !state.materials.length ? 'Add material' : nextAction === 'ADD_MATERIAL' ? 'Add material' : nextAction === 'CREATE_LEARNING_PLAN' ? (state.assessment?.id ? 'Build my plan' : 'Take assessment') : nextAction.includes('LEARNING_PLAN') || nextAction === 'CONTINUE_PLAN' ? 'Continue learning' : 'Start assessment';
    const planContent = planCount ? `<div class="plan-preview"><div class="plan-progress" style="margin:0 0 9px"><strong>${progressPct}%</strong><div class="progress-bar"><span style="width:${progressPct}%"></span></div><span class="lesson-meta">${completed}/${planCount} complete</span></div><div class="learning-list">${planItems.slice(0, 3).map((item, index) => `<div class="learning-row"><span class="lesson-check${item.status === 'COMPLETED' ? ' done' : ''}">${item.status === 'COMPLETED' ? '✓' : index + 1}</span><span class="lesson-copy"><strong>${escapeHtml(item.topic || item.lesson?.title)}</strong><small>${escapeHtml(item.status || 'UP NEXT')} · ${item.estimatedMinutes || 10} min</small></span><span class="lesson-meta">${Number(item.masteryScore ?? 0)}% mastery</span></div>`).join('')}</div>${planItems.length > 3 ? '<button class="link-button" data-view="learning">View full learning plan →</button>' : ''}</div>` : `<div class="plan-preview"><div class="plan-empty"><span class="empty-icon">✧</span><div><strong>Your plan starts with a quick check-in</strong><p>Add a PDF and take a short diagnostic. We’ll shape the next steps around what you know.</p></div>${state.materials.length ? '<button class="button button-primary button-small" data-action="new-assessment">Take assessment</button>' : '<button class="button button-primary button-small" data-view="library">Add material</button>'}</div></div>`;
    $('#content').innerHTML = `
      <div class="welcome-row"><div class="welcome-copy"><span class="eyebrow">YOUR PERSONAL STUDY SPACE</span><h1>Good to see you, ${escapeHtml(session.username || 'learner')} <span aria-hidden="true">✦</span></h1><p>Let’s make a little progress today.</p></div><div class="date-pill">${new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date())}</div></div>
      <section class="hero-card"><div class="hero-content"><div class="hero-kicker"><span class="spark">✦</span> YOUR NEXT BEST STEP</div><h2>${escapeHtml(nextTitle)}</h2><p>${escapeHtml(nextMessage)}</p></div><button class="button" data-action="recommendation" data-next="${escapeHtml(nextAction)}">${escapeHtml(nextButton)} <span>→</span></button><div class="hero-art" aria-hidden="true">✳</div></section>
      <section class="stats-grid" aria-label="Learning progress">${statCard('Average mastery', `${Number(p.averageMasteryScore || 0)}%`, p.topicsTracked ? `${p.topicsTracked} topics tracked` : 'Ready when you are', '◒', Number(p.averageMasteryScore || 0) > 0)}${statCard('Lessons completed', `${completed}/${planCount || '—'}`, planCount ? 'In your current plan' : 'Create a plan to begin', '✓')}${statCard('Topics to revisit', Number(p.topicsNeedingReview || 0), 'A chance to strengthen recall', '↻')}${statCard('Practice sessions', Number(p.exerciseAttempts || 0), `${Number(p.assessmentAttempts || 0)} assessments completed`, '✎')}</section>
      <div class="dashboard-grid"><section class="panel"><div class="panel-header"><div><h2 class="panel-title">Your learning plan</h2><p class="panel-subtitle">A path that adapts as you make progress</p></div><button class="link-button" data-view="learning">View plan →</button></div>${planContent}</section>
      <div class="right-column"><section class="panel recommendation-card"><span class="recommendation-tag">✦ &nbsp;PERSONALIZED FOR YOU</span><h3>${escapeHtml(rec.topic || nextTitle)}</h3><p>${escapeHtml(rec.message || nextMessage)}</p><div class="recommendation-meta"><span>◷ &nbsp;${rec.estimatedMinutes || 8} min</span>${rec.sourcePage ? `<span>▤ &nbsp;Page ${rec.sourcePage}</span>` : ''}</div><button class="button button-primary button-small" data-action="recommendation" data-next="${escapeHtml(nextAction)}">${escapeHtml(nextButton)} &nbsp;→</button></section>
      <section class="panel section-gap"><div class="panel-header"><div><h2 class="panel-title">Recent activity</h2><p class="panel-subtitle">Your momentum, at a glance</p></div><button class="link-button" data-view="reports">See all</button></div>${activityMarkup(state.activity.slice(0, 4))}</section></div></div>
      <section class="panel section-gap"><div class="panel-header"><div><h2 class="panel-title">Study materials</h2><p class="panel-subtitle">${state.materials.length} document${state.materials.length === 1 ? '' : 's'} in ${escapeHtml(state.project.name)}</p></div><button class="link-button" data-view="library">Open library →</button></div>${state.materials.length ? `<div class="material-strip">${state.materials.slice(0, 3).map(materialCard).join('')}</div>` : '<div class="plan-preview"><div class="plan-empty"><span class="empty-icon">▤</span><div><strong>Your notes, organized</strong><p>Upload PDFs and turn them into topic guides, practice and an adaptive plan.</p></div><button class="button button-secondary button-small" data-view="library">Browse library</button></div></div>'}</section>
      ${firstMaterial && firstMaterial.status === 'PROCESSING' ? '<p class="field-hint" style="margin-top:10px">Your newest document is processing. Refresh the library in a moment to see its topics.</p>' : ''}`;
  }

  function materialCard(material) {
    const status = material.status || 'PROCESSED';
    const label = status === 'PROCESSED' ? 'Ready to study' : status === 'FAILED' ? 'Needs attention' : 'Processing';
    return `<button class="material-card" data-material-id="${escapeHtml(material.id)}" title="Open ${escapeHtml(material.originalFilename)}"><span class="file-icon">PDF</span><span class="material-info"><strong>${escapeHtml(material.originalFilename)}</strong><small>${label} · ${material.pageCount || '—'} pages</small></span></button>`;
  }

  function activityMarkup(items) {
    if (!items?.length) return '<div class="activity-list"><div class="project-empty">Your learning activity will appear here.</div></div>';
    return `<div class="activity-list">${items.map(item => `<div class="activity-item"><span class="activity-icon">${item.type?.includes('ASSESS') ? '◉' : item.type?.includes('EXERCISE') ? '✎' : '✦'}</span><span class="activity-copy"><strong>${escapeHtml(item.title || item.type || 'Learning activity')}</strong><small>${shortDate(item.occurredAt)}</small></span><span class="activity-score">${item.scorePercent != null ? `${item.scorePercent}%` : item.correct == null ? '' : item.correct ? 'Correct' : 'Review'}</span></div>`).join('')}</div>`;
  }

  function renderLibrary() {
    const project = state.project;
    $('#content').innerHTML = `<div class="page-heading"><div><span class="eyebrow">YOUR LIBRARY</span><h1>Study materials</h1><p>Bring your course content into one focused learning space.</p></div><div class="heading-actions"><button class="button button-primary" data-action="open-upload">＋ &nbsp; Upload a PDF</button></div></div>
      <section class="upload-zone"><span class="upload-mark">⇧</span><span class="upload-copy"><strong>Drop a PDF into your workspace</strong><p>We’ll extract its topics and build study tools from the pages. Up to 10 MB.</p></span><button class="button button-secondary button-small" type="button">Choose PDF</button><input class="zone-file" type="file" accept="application/pdf,.pdf" aria-label="Choose a PDF to upload"></section>
      <section class="panel section-gap"><div class="panel-header"><div><h2 class="panel-title">${escapeHtml(project.name)}</h2><p class="panel-subtitle">${state.materials.length} material${state.materials.length === 1 ? '' : 's'}</p></div><button class="link-button" data-action="refresh-project">↻ &nbsp; Refresh</button></div>
      ${state.materials.length ? `<table class="material-table"><thead><tr><th>DOCUMENT</th><th>STATUS</th><th>PAGES</th><th>ADDED</th><th></th></tr></thead><tbody>${state.materials.map(material => `<tr><td><button class="file-cell link-button" data-material-id="${escapeHtml(material.id)}"><span class="file-icon">PDF</span>${escapeHtml(material.originalFilename)}</button></td><td><span class="status-pill ${material.status === 'FAILED' ? 'failed' : material.status === 'PROCESSING' ? 'pending' : ''}"><span>●</span>${escapeHtml(material.status === 'PROCESSED' ? 'Ready' : material.status === 'FAILED' ? 'Failed' : 'Processing')}</span></td><td>${material.pageCount || '—'}</td><td>${shortDate(material.createdAt)}</td><td><button class="link-button" data-material-id="${escapeHtml(material.id)}">Open →</button></td></tr>`).join('')}</tbody></table>` : '<div class="empty-state" style="min-height:200px"><div class="empty-illustration">▤</div><h2>No materials yet</h2><p>Add a PDF to start building a personalized learning plan.</p><button class="button button-primary button-small" data-action="open-upload">Choose a PDF</button></div>'}</section>
      ${state.material ? `<section class="panel section-gap"><div class="panel-header"><div><h2 class="panel-title">Topics in ${escapeHtml(state.material.originalFilename)}</h2><p class="panel-subtitle">Select a topic to generate focused questions.</p></div><button class="link-button" data-action="suggest-topics">Refresh topics with AI</button></div>${state.material.topics?.length ? `<div class="material-topic-list">${state.material.topics.map(topic => `<button class="topic-chip" data-action="make-questions" data-topic="${escapeHtml(topic.name)}">${escapeHtml(topic.name)} <span>p. ${topic.sourcePage} · practice +</span></button>`).join('')}</div>` : '<div class="plan-preview"><p class="field-hint">No topic list found. Try AI topic suggestions.</p></div>'}</section>
      <section class="panel section-gap"><div class="panel-header"><div><h2 class="panel-title">Ask your material</h2><p class="panel-subtitle">Get an answer with relevant source passages.</p></div><span class="recommendation-tag">✦ &nbsp;AI STUDY HELPER</span></div><div class="plan-preview"><form class="ai-question-form" data-form="ai-ask"><div style="display:flex;gap:9px"><input class="text-input" name="question" maxlength="1000" placeholder="Ask a question about your notes…" required ${state.material ? '' : 'disabled'}><button class="button button-primary" type="submit" ${state.material ? '' : 'disabled'}>Ask <span>→</span></button></div><p class="field-hint">${state.material ? `Using ${escapeHtml(state.material.originalFilename)}.` : 'Open a material above to choose a source document.'}</p></form></div></section>` : ''}`;
    const zone = $('.upload-zone');
    if (zone) {
      zone.addEventListener('dragover', event => { event.preventDefault(); zone.classList.add('drag-active'); });
      zone.addEventListener('dragleave', event => { if (!zone.contains(event.relatedTarget)) zone.classList.remove('drag-active'); });
      zone.addEventListener('drop', event => {
        event.preventDefault(); zone.classList.remove('drag-active');
        const file = event.dataTransfer?.files?.[0];
        if (file) uploadModalWithFile(file);
      });
    }
  }

  function renderLearning() {
    if (!state.plan) {
      const canStart = state.materials.some(material => material.status === 'PROCESSED');
      $('#content').innerHTML = `<div class="page-heading"><div><span class="eyebrow">YOUR NEXT CHAPTER</span><h1>Learning plan</h1><p>A clear, achievable path from where you are to where you want to be.</p></div></div><section class="panel empty-state"><div class="empty-illustration">✧</div><h2>Your plan is ready to take shape</h2><p>Start with a diagnostic on one of your processed materials. Your results help us put the right topics in the right order.</p>${canStart ? '<button class="button button-primary" data-action="new-assessment">Take a quick assessment</button>' : '<button class="button button-primary" data-view="library">Add a study PDF</button>'}</section>`;
      return;
    }
    const items = state.plan.items || [];
    const completed = items.filter(item => item.status === 'COMPLETED').length;
    const percent = items.length ? Math.round(completed * 100 / items.length) : 0;
    $('#content').innerHTML = `<div class="page-heading"><div><span class="eyebrow">BUILT AROUND YOU</span><h1>Learning plan</h1><p>${escapeHtml(state.plan.learningPreference || 'A personalized path through your study material.')}</p></div><div class="heading-actions"><button class="button button-secondary" data-view="reports">View progress</button></div></div>
      <section class="plan-header-card"><div><h2>${escapeHtml(state.project.name)} · Your study path</h2><p>${items.length} focused lessons · ${Number(state.plan.availableStudyMinutes || 0)} minutes available · ${escapeHtml(state.plan.status || 'IN_PROGRESS').toLowerCase().replaceAll('_',' ')}</p></div><div class="plan-progress"><strong>${percent}%</strong><div class="progress-bar"><span style="width:${percent}%"></span></div><span class="lesson-meta">${completed}/${items.length}</span></div></section>
      <section>${items.map((item, index) => {
        const lesson = item.lesson || {};
        const open = state.expandedLesson === lesson.id;
        const done = lesson.completed || item.status === 'COMPLETED';
        return `<article class="lesson-card${done ? ' done' : ''}"><div class="lesson-card-head"><span class="lesson-sequence">${done ? '✓' : index + 1}</span><span class="lesson-main"><strong>${escapeHtml(lesson.title || item.topic)}</strong><small>${escapeHtml(item.topic)} · ${item.estimatedMinutes || 10} min · ${Number(item.masteryScore || 0)}% mastery</small></span><button class="button button-secondary button-small" data-action="toggle-lesson" data-lesson-id="${escapeHtml(lesson.id || '')}">${open ? 'Close' : done ? 'Review' : 'Open lesson'}</button></div>${open ? `<div class="lesson-details"><h4>Learning objective</h4><p>${escapeHtml(lesson.objective || 'Build confidence with this topic.')}</p><h4>Lesson</h4><p>${escapeHtml(lesson.explanation || 'Review the relevant source pages and key ideas.')}</p>${lesson.workedExample ? `<h4>Worked example</h4><p class="example">${escapeHtml(lesson.workedExample)}</p>` : ''}<div style="display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:15px"><span class="field-hint">Source page ${lesson.sourcePage || '—'}</span><button class="button button-primary button-small" data-action="exercise" data-lesson-id="${escapeHtml(lesson.id || '')}">Practice this topic →</button></div></div>` : ''}</article>`;
      }).join('')}</section>`;
  }

  function renderReports() {
    const p = state.progress || {};
    $('#content').innerHTML = `<div class="page-heading"><div><span class="eyebrow">EVERY STEP ADDS UP</span><h1>Your progress</h1><p>Notice what’s getting stronger and where another review will help.</p></div><div class="heading-actions"><button class="button button-secondary" data-action="refresh-project">↻ &nbsp; Refresh report</button></div></div>
      <section class="stats-grid">${statCard('Average mastery', `${Number(p.averageMasteryScore || 0)}%`, `${Number(p.masteredTopics || 0)} topics mastered`, '◒', Number(p.averageMasteryScore || 0) > 0)}${statCard('Topics tracked', Number(p.topicsTracked || 0), `${Number(p.topicsNeedingReview || 0)} could use another look`, '⌕')}${statCard('Plan completion', `${Number(p.completedPlanItems || 0)}/${Number(p.totalPlanItems || 0)}`, p.currentPlanStatus || 'No active plan yet', '✓')}${statCard('Practice', Number(p.exerciseAttempts || 0), `${Number(p.assessmentAttempts || 0)} assessment attempts`, '✎')}</section>
      <div class="report-grid"><section class="panel"><div class="panel-header"><div><h2 class="panel-title">Topic mastery</h2><p class="panel-subtitle">Your confidence, measured over time</p></div></div>${state.mastery.length ? `<div class="mastery-list">${state.mastery.map(topic => `<div class="mastery-row"><div><div class="mastery-topic" title="${escapeHtml(topic.topic)}">${escapeHtml(topic.topic)}</div><span class="mastery-state">${topic.mastered ? 'Mastered' : topic.needsReview ? 'Review recommended' : `${topic.attempts || 0} attempt${topic.attempts === 1 ? '' : 's'}`}</span></div><div class="progress-bar"><span style="width:${Math.max(2, Math.min(100, Number(topic.masteryScore || 0)))}%;background:${topic.needsReview ? 'linear-gradient(90deg,#efb452,#f5d18c)' : ''}"></span></div><span class="mastery-score">${Number(topic.masteryScore || 0)}%</span></div>`).join('')}</div>` : '<div class="empty-state" style="min-height:190px"><div class="empty-illustration">◒</div><h2>Your mastery map is on its way</h2><p>Take a diagnostic or practice an exercise to start tracking topics.</p></div>'}</section>
      <section class="panel"><div class="panel-header"><div><h2 class="panel-title">Learning activity</h2><p class="panel-subtitle">Your recent practice and checkpoints</p></div></div>${activityMarkup(state.activity)}</section></div>
      <section class="callout section-gap"><span class="callout-icon">✦</span><div><strong>Progress is personal</strong><p>Mastery scores reflect your attempts on this project. Revisit topics marked for review and your next learning step will adapt.</p></div></section>`;
  }

  function modal(title, subtitle, body, wide = false) {
    const root = $('#modal-root');
    root.innerHTML = `<div class="modal-backdrop" data-action="backdrop"><section class="modal${wide ? ' modal-wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-title"><header class="modal-header"><div><h2 id="modal-title">${escapeHtml(title)}</h2><p>${escapeHtml(subtitle || '')}</p></div><button class="modal-close" data-action="close-modal" aria-label="Close">×</button></header><div class="modal-body">${body}</div></section></div>`;
    $('.modal-backdrop').addEventListener('click', event => { if (event.target.classList.contains('modal-backdrop')) closeModal(); });
    $('.modal-close').focus();
  }

  function closeModal() { $('#modal-root').innerHTML = ''; }

  function createProjectModal() {
    modal('Create a learning project', 'Choose a subject, course or exam to organize your study.', `<form class="modal-form" data-form="project"><div class="form-field"><label for="project-name">Project name</label><input id="project-name" name="name" maxlength="120" placeholder="e.g. BCA final exams" required></div><div class="form-field"><label for="project-description">What are you working toward?</label><textarea id="project-description" name="description" maxlength="1000" placeholder="Add a short description or goal (optional)"></textarea><span class="field-help">A little context helps you keep your focus.</span></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancel</button><button class="button button-primary" type="submit">Create project →</button></div></form>`);
  }

  function uploadModal() {
    modal('Add study material', `Upload a PDF into ${state.project ? escapeHtml(state.project.name) : 'your project'}.`, `<form class="modal-form" data-form="upload"><div class="form-field"><label for="pdf-file">Choose a PDF</label><input id="pdf-file" name="file" type="file" accept="application/pdf,.pdf" required><span class="field-help">PDF only · maximum size 10 MB. The file is processed by your local backend.</span></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancel</button><button class="button button-primary" type="submit">Upload material ↑</button></div></form>`);
  }

  async function uploadMaterial(form) {
    const file = form.elements.file.files[0];
    if (!file || !state.project) return;
    if (file.size > 10 * 1024 * 1024) { toast('This PDF is larger than the 10 MB limit.', true); return; }
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) { toast('Choose a PDF file to continue.', true); return; }
    const button = form.querySelector('[type=submit]'); button.disabled = true; button.textContent = 'Uploading…';
    try {
      const body = new FormData(); body.append('file', file);
      const material = await api(`/projects/${state.project.id}/materials`, { method: 'POST', body });
      closeModal(); toast('Your PDF was added. We’re preparing the study material.');
      await loadProject(state.project);
      if (material?.id) { await openMaterial(material.id); state.view = 'library'; render(); }
    } catch (error) { button.disabled = false; button.textContent = 'Upload material ↑'; toast(error.message, true); }
  }

  async function openMaterial(id) {
    try {
      const [detail, topics] = await Promise.all([api(`/materials/${id}`), api(`/materials/${id}/topics`)]);
      state.material = { ...detail.material, chunks: detail.chunks || [], topics: topics || [] };
      state.view = 'library';
      render();
    } catch (error) { toast(error.message, true); }
  }

  async function askMaterial(question) {
    if (!state.material) { toast('Open a study material first.', true); return; }
    try {
      const result = await api(`/materials/${state.material.id}/ai/answer`, { method: 'POST', body: JSON.stringify({ question }) });
      const sources = (result.sources || []).map(source => `<div class="source-item"><strong>Page ${source.pageNumber ?? source.page ?? '—'}</strong> · ${escapeHtml(source.excerpt || source.text || '')}</div>`).join('');
      modal('Your answer', `${result.provider || 'Study helper'} · based on ${state.material.originalFilename}`, `<div class="answer-box">${escapeHtml(result.answer)}</div>${sources ? `<div class="source-list">${sources}</div>` : ''}`);
    } catch (error) { toast(error.message, true); }
  }

  async function suggestTopics() {
    if (!state.material) { toast('Choose a study material first.', true); return; }
    try {
      const result = await api(`/materials/${state.material.id}/ai/topics`, { method: 'POST' });
      state.material.topics = result.topics || [];
      render(); toast('Topic suggestions are ready.');
    } catch (error) { toast(error.message, true); }
  }

  async function generateQuestions(topic) {
    if (!state.material) { toast('Open a material first.', true); return; }
    try {
      const result = await api(`/materials/${state.material.id}/ai/questions`, { method: 'POST', body: JSON.stringify({ topic, count: 3 }) });
      modal(`Practice · ${topic}`, 'Use these prompts to check your understanding, then review the source pages.', (result.questions || []).map((question, index) => `<article class="assessment-question"><small>QUESTION ${index + 1} · ${escapeHtml(question.difficulty || 'Practice')} · PAGE ${question.sourcePage || '—'}</small><h3>${escapeHtml(question.question)}</h3><ul class="question-options">${(question.options || []).map(option => `<li>${escapeHtml(option)}</li>`).join('')}</ul></article>`).join('') || '<p class="field-hint">No questions were returned for this topic.</p>', true);
    } catch (error) { toast(error.message, true); }
  }

  async function newAssessment() {
    if (!state.project) return;
    const materials = state.materials.filter(item => item.status === 'PROCESSED');
    if (!materials.length) { toast('Add a processed PDF before taking an assessment.', true); state.view = 'library'; render(); uploadModal(); return; }
    modal('Start a diagnostic', 'Choose the study material you want to check in on.', `<form class="modal-form" data-form="assessment"><div class="form-field"><label for="assessment-material">Study material</label><select id="assessment-material" class="select-input" name="materialId">${materials.map(item => `<option value="${escapeHtml(item.id)}">${escapeHtml(item.originalFilename)}</option>`).join('')}</select></div><div class="form-field"><label for="question-count">Number of questions</label><select id="question-count" class="select-input" name="questionCount"><option value="5">5 questions · quick</option><option value="8">8 questions · standard</option><option value="10">10 questions · thorough</option></select></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Not now</button><button class="button button-primary" type="submit">Begin assessment →</button></div></form>`);
  }

  async function beginAssessment(form) {
    const button = form.querySelector('[type=submit]'); button.disabled = true; button.textContent = 'Preparing…';
    try {
      const assessment = await api(`/projects/${state.project.id}/assessments/diagnostic`, { method: 'POST', body: JSON.stringify({ materialId: form.elements.materialId.value, questionCount: Number(form.elements.questionCount.value) }) });
      state.assessment = assessment;
      sessionStorage.setItem(`studyflow-assessment-${state.project.id}`, assessment.id);
      renderAssessment(assessment);
    } catch (error) { button.disabled = false; button.textContent = 'Begin assessment →'; toast(error.message, true); }
  }

  function renderAssessment(assessment) {
    const questions = assessment.questions || [];
    modal(assessment.title || 'Your diagnostic', `${questions.length} questions · Take your time.`, `<form class="modal-form" data-form="submit-assessment"><div id="assessment-questions">${questions.map((question, index) => `<article class="assessment-question"><small>QUESTION ${index + 1} · ${escapeHtml(question.topic)} · PAGE ${question.sourcePage || '—'}</small><h3>${escapeHtml(question.question)}</h3>${(question.options || []).map((option, optionIndex) => `<label class="exercise-option"><input type="radio" name="answer-${escapeHtml(question.id)}" value="${optionIndex}" required><span>${escapeHtml(option)}</span></label>`).join('')}</article>`).join('')}</div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Save for later</button><button class="button button-primary" type="submit">See my results →</button></div></form>`, true);
  }

  async function submitAssessment(form) {
    const questions = state.assessment?.questions || [];
    const answers = questions.map(question => {
      const selected = form.querySelector(`input[name="answer-${CSS.escape(question.id)}"]:checked`);
      return selected ? { questionId: question.id, selectedOptionIndex: Number(selected.value) } : null;
    });
    if (answers.some(answer => !answer)) { toast('Please choose an answer for each question.', true); return; }
    const button = form.querySelector('[type=submit]'); button.disabled = true; button.textContent = 'Scoring your answers…';
    try {
      const result = await api(`/assessments/${state.assessment.id}/submit`, { method: 'POST', body: JSON.stringify({ answers }) });
      state.assessment.lastResult = result;
      renderAssessmentResult(result);
      await loadProject(state.project, false);
    } catch (error) { button.disabled = false; button.textContent = 'See my results →'; toast(error.message, true); }
  }

  function renderAssessmentResult(result) {
    const feedback = result.feedback || [];
    const notes = feedback.map(item => `<div class="feedback-item"><strong>${escapeHtml(item.topic || 'Question')} · ${item.correct ? 'Correct' : 'Review this one'}</strong><p>${escapeHtml(item.explanation || item.correctOption || '')}</p></div>`).join('');
    modal('A good step forward', 'Your results will shape a more useful study plan.', `<div class="result-summary"><div class="result-stat"><strong>${Number(result.scorePercent || 0)}%</strong><small>Score</small></div><div class="result-stat"><strong>${Number(result.correctAnswers || 0)}/${Number(result.totalQuestions || 0)}</strong><small>Correct</small></div><div class="result-stat"><strong>${(result.topicScores || []).filter(topic => topic.needsReview).length || 0}</strong><small>Review topics</small></div></div>${notes ? `<h3 class="panel-title" style="margin:12px 0 6px">Question feedback</h3>${notes}` : ''}<div class="modal-actions" style="margin-top:17px"><button class="button button-secondary" data-action="close-modal">Later</button><button class="button button-primary" data-action="make-plan">Build my learning plan →</button></div>`);
  }

  function makePlanModal() {
    if (!state.assessment?.id) { toast('Take a diagnostic assessment first.', true); return; }
    modal('Shape your study plan', 'Choose an amount of focused study time and how you like to learn.', `<form class="modal-form" data-form="learning-plan"><div class="form-field"><label for="study-minutes">Available study time</label><select class="select-input" name="availableStudyMinutes" id="study-minutes"><option value="30">30 minutes</option><option value="60" selected>1 hour</option><option value="120">2 hours</option><option value="240">4 hours</option></select><span class="field-help">You can adjust your routine later by creating a new plan.</span></div><div class="form-field"><label for="learning-preference">How do you like to learn?</label><select class="select-input" name="learningPreference" id="learning-preference"><option value="A balanced mix of clear explanations and worked examples">A balanced mix</option><option value="Start with simple explanations, then build toward practice">Start with simple explanations</option><option value="I learn best by practicing and testing myself">Practice and self-testing</option></select></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancel</button><button class="button button-primary" type="submit">Create my plan ✦</button></div></form>`);
  }

  async function createPlan(form) {
    const button = form.querySelector('[type=submit]'); button.disabled = true; button.textContent = 'Building your plan…';
    try {
      state.plan = await api(`/projects/${state.project.id}/learning-plans`, { method: 'POST', body: JSON.stringify({ assessmentId: state.assessment.id, availableStudyMinutes: Number(form.elements.availableStudyMinutes.value), learningPreference: form.elements.learningPreference.value }) });
      closeModal(); state.view = 'learning'; state.expandedLesson = null;
      await loadProject(state.project, false); render(); toast('Your personalized learning plan is ready.');
    } catch (error) { button.disabled = false; button.textContent = 'Create my plan ✦'; toast(error.message, true); }
  }

  async function openLesson(id) {
    if (!id) { toast('This lesson is still being prepared.', true); return; }
    try {
      const lesson = await api(`/lessons/${id}`);
      state.plan.items = (state.plan.items || []).map(item => item.lesson?.id === id ? { ...item, lesson } : item);
      state.expandedLesson = state.expandedLesson === id ? null : id;
      render();
    } catch (error) { toast(error.message, true); }
  }

  async function createExercise(lessonId) {
    try {
      const exercise = await api(`/lessons/${lessonId}/exercises`, { method: 'POST' });
      state.exercise = exercise;
      modal('Practice this topic', 'Choose the best answer. You’ll get feedback right away.', `<form class="modal-form" data-form="exercise"><div class="assessment-question"><small>${escapeHtml(exercise.difficulty || 'PRACTICE')} · PAGE ${exercise.sourcePage || '—'}</small><h3>${escapeHtml(exercise.question)}</h3>${(exercise.options || []).map((option, index) => `<label class="exercise-option"><input type="radio" name="selectedOptionIndex" value="${index}" required><span>${escapeHtml(option)}</span></label>`).join('')}</div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Close</button><button class="button button-primary" type="submit">Check my answer →</button></div></form>`);
    } catch (error) { toast(error.message, true); }
  }

  async function submitExercise(form) {
    const selection = form.querySelector('input[name="selectedOptionIndex"]:checked');
    if (!selection) { toast('Choose an answer first.', true); return; }
    const button = form.querySelector('[type=submit]'); button.disabled = true; button.textContent = 'Checking…';
    try {
      const result = await api(`/exercises/${state.exercise.id}/submit`, { method: 'POST', body: JSON.stringify({ selectedOptionIndex: Number(selection.value) }) });
      const next = result.nextRecommendedTopic ? `<p class="field-hint" style="margin-top:12px">Next up: ${escapeHtml(result.nextRecommendedTopic)}</p>` : '';
      modal(result.correct ? 'Nicely done!' : 'A useful chance to review', `${result.masteryBefore}% → ${result.masteryAfter}% topic mastery`, `<div class="answer-box"><strong>${result.correct ? 'That’s right.' : `The answer is ${escapeHtml(result.correctOption || 'shown in your notes')}.`}</strong><br>${escapeHtml(result.explanation || result.recommendation || '')}</div>${next}<div class="modal-actions" style="margin-top:16px"><button class="button button-primary" data-action="exercise-done">Continue learning →</button></div>`);
      await loadProject(state.project, false);
    } catch (error) { button.disabled = false; button.textContent = 'Check my answer →'; toast(error.message, true); }
  }

  async function handleProjectForm(form) {
    const button = form.querySelector('[type=submit]'); button.disabled = true;
    try {
      const project = await api('/projects', { method: 'POST', body: JSON.stringify({ name: form.elements.name.value.trim(), description: form.elements.description.value.trim() }) });
      closeModal(); state.projects.unshift(project); state.view = 'dashboard'; await loadProject(project); toast('Your learning project is ready.');
    } catch (error) { button.disabled = false; toast(error.message, true); }
  }

  async function recommendationAction(action) {
    if (action === 'ADD_MATERIAL') { state.view = 'library'; render(); uploadModal(); }
    else if (action === 'CREATE_LEARNING_PLAN') { if (state.assessment?.id) makePlanModal(); else await newAssessment(); }
    else if (action === 'CREATE_DIAGNOSTIC_ASSESSMENT' || action === 'CREATE_ASSESSMENT' || action === 'START_ASSESSMENT') await newAssessment();
    else if (action === 'REVIEW_PROGRESS') { state.view = 'reports'; render(); }
    else if (action === 'CONTINUE_LEARNING_PLAN' || action === 'CONTINUE_PLAN' || action === 'START_LESSON' || action === 'REVIEW_TOPIC') { state.view = state.plan ? 'learning' : 'dashboard'; render(); if (state.plan?.items?.length) await openLesson(state.recommendation?.lessonId || state.plan.items.find(item => item.status !== 'COMPLETED')?.lesson?.id || state.plan.items[0].lesson?.id); }
    else if (state.plan) { state.view = 'learning'; render(); }
    else if (state.materials.length) await newAssessment();
    else { state.view = 'library'; render(); uploadModal(); }
  }

  function setView(view) {
    if (!['dashboard', 'library', 'learning', 'reports'].includes(view)) return;
    state.view = view;
    closeSidebar();
    render();
  }

  function closeSidebar() { $('#sidebar').classList.remove('is-open'); }

  document.addEventListener('click', async event => {
    const viewButton = event.target.closest('[data-view]');
    if (viewButton) { setView(viewButton.dataset.view); return; }
    const projectButton = event.target.closest('[data-project-id]');
    if (projectButton) { const project = state.projects.find(item => item.id === projectButton.dataset.projectId); if (project) { await loadProject(project); state.view = 'dashboard'; render(); } return; }
    const materialButton = event.target.closest('[data-material-id]');
    if (materialButton) { await openMaterial(materialButton.dataset.materialId); return; }
    const action = event.target.closest('[data-action]')?.dataset.action;
    if (!action) return;
    if (action === 'new-project') createProjectModal();
    else if (action === 'open-upload') uploadModal();
    else if (action === 'close-modal') closeModal();
    else if (action === 'logout') { session.token = ''; session.username = ''; sessionStorage.removeItem('studyflow-token'); sessionStorage.removeItem('studyflow-user'); state.projects = []; state.project = null; showAuth(); $('#auth-password').value = ''; }
    else if (action === 'retry') await loadProjects();
    else if (action === 'refresh-project') { if (state.project) await loadProject(state.project); }
    else if (action === 'new-assessment') await newAssessment();
    else if (action === 'make-plan') makePlanModal();
    else if (action === 'recommendation') await recommendationAction(event.target.closest('[data-next]')?.dataset.next || '');
    else if (action === 'suggest-topics') await suggestTopics();
    else if (action === 'make-questions') await generateQuestions(event.target.closest('[data-topic]')?.dataset.topic || '');
    else if (action === 'toggle-lesson') await openLesson(event.target.closest('[data-lesson-id]')?.dataset.lessonId || '');
    else if (action === 'exercise') await createExercise(event.target.closest('[data-lesson-id]')?.dataset.lessonId || '');
    else if (action === 'exercise-done') { closeModal(); state.view = 'learning'; render(); }
    else if (action === 'open-sidebar') $('#sidebar').classList.add('is-open');
    else if (action === 'close-sidebar') closeSidebar();
    else if (action === 'open-api-help') modal('About your workspace', 'A friendly interface for your AI Tutor backend.', `<div class="answer-box">This web app runs from your Spring Boot server at the same address as the API. Sign in with the learner account created in this project, then create a project and add a PDF to begin. The API remains available in <a href="/swagger-ui.html" target="_blank" rel="noreferrer">Swagger UI</a>.</div>`);
  });

  document.addEventListener('submit', async event => {
    const form = event.target;
    const kind = form.dataset.form;
    if (!kind) return;
    event.preventDefault();
    if (kind === 'auth') await handleAuth(event);
    else if (kind === 'project') await handleProjectForm(form);
    else if (kind === 'upload') await uploadMaterial(form);
    else if (kind === 'ai-ask') { const question = form.elements.question.value.trim(); if (question) await askMaterial(question); }
    else if (kind === 'assessment') await beginAssessment(form);
    else if (kind === 'submit-assessment') await submitAssessment(form);
    else if (kind === 'learning-plan') await createPlan(form);
    else if (kind === 'exercise') await submitExercise(form);
  });

  document.addEventListener('change', event => {
    if (event.target.matches('.upload-zone input[type=file]') && event.target.files[0]) uploadModalWithFile(event.target.files[0]);
  });

  function uploadModalWithFile(file) {
    uploadModal();
    const input = $('#pdf-file');
    if (!input) return;
    const transfer = new DataTransfer(); transfer.items.add(file); input.files = transfer.files;
  }

  $$('.auth-tab').forEach(tab => tab.addEventListener('click', () => authMode(tab.dataset.authMode)));
  $('#auth-form').addEventListener('submit', handleAuth);
  authMode('login');

  async function resumeSession() {
    try {
      await api('/health');
      setApiStatus(true);
      if (session.token && session.username) await startWorkspace();
      else showAuth();
    } catch (error) { setApiStatus(false); showAuth('The backend is not responding yet. Start the server and try again.'); }
  }

  resumeSession();
})();
