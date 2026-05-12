import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  ArrowUpRight,
  Bell,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  FileText,
  LayoutDashboard,
  LogOut,
  Search,
  ShieldCheck,
  UsersRound
} from "lucide-react";

const storageKey = "scholr.auth";

const fallbackMetrics = {
  totalStudents: 0,
  totalTeachers: 0,
  totalOfferings: 0,
  activeEnrollments: 0,
  publishedResults: 0,
  studentsWithOutstandingDues: 0,
  lowAttendanceCases: 0
};

const roleWorkflows = {
  ADMIN: ["Academic setup", "User provisioning", "Reports", "Audit review"],
  TEACHER: ["Attendance", "Course offerings", "Exam marks", "Announcements"],
  STUDENT: ["Enrollment", "Fees", "Results", "Notifications"]
};

const navItems = [
  { label: "Overview", href: "#overview", icon: LayoutDashboard },
  { label: "People", href: "#people", icon: UsersRound },
  { label: "Academics", href: "#academic", icon: BookOpen },
  { label: "Calendar", href: "#calendar", icon: CalendarDays }
];

const operatingRhythm = [
  { code: "01", label: "Validate term records" },
  { code: "02", label: "Review attendance exceptions" },
  { code: "03", label: "Clear pending academic actions" }
];

function readStoredAuth() {
  try {
    return JSON.parse(localStorage.getItem(storageKey));
  } catch {
    return null;
  }
}

async function apiFetch(path, token, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    }
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || "Request failed");
  }

  return payload.data ?? payload;
}

function formatNumber(value) {
  return new Intl.NumberFormat("en-IN").format(value ?? 0);
}

function App() {
  const [auth, setAuth] = useState(readStoredAuth);
  const [loginForm, setLoginForm] = useState({ username: "", password: "" });
  const [loginError, setLoginError] = useState("");
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [dashboard, setDashboard] = useState(fallbackMetrics);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loadState, setLoadState] = useState("idle");

  const user = currentUser || auth?.user;
  const role = user?.role || "GUEST";
  const workflows = roleWorkflows[role] || roleWorkflows.STUDENT;

  const greeting = useMemo(() => {
    if (!user) {
      return "A precise academic workspace for daily operations.";
    }

    return `Signed in as ${user.username}.`;
  }, [user]);

  useEffect(() => {
    if (!auth?.accessToken) {
      return;
    }

    let isMounted = true;

    async function loadDashboard() {
      setLoadState("loading");

      try {
        const [profile, notificationCount, adminSummary] = await Promise.all([
          apiFetch("/api/v1/users/me", auth.accessToken),
          apiFetch("/api/v1/notifications/me/unread-count", auth.accessToken),
          auth.user?.role === "ADMIN"
            ? apiFetch("/api/v1/admin/reports/dashboard", auth.accessToken)
            : Promise.resolve(null)
        ]);

        if (!isMounted) {
          return;
        }

        setCurrentUser(profile);
        setUnreadCount(notificationCount.unreadCount ?? 0);
        if (adminSummary) {
          setDashboard({ ...fallbackMetrics, ...adminSummary });
        }
        setLoadState("ready");
      } catch {
        if (isMounted) {
          setLoadState("error");
        }
      }
    }

    loadDashboard();

    return () => {
      isMounted = false;
    };
  }, [auth]);

  async function handleLogin(event) {
    event.preventDefault();
    setLoginError("");
    setIsLoggingIn(true);

    try {
      const data = await apiFetch("/api/v1/auth/login", null, {
        method: "POST",
        body: JSON.stringify(loginForm)
      });

      localStorage.setItem(storageKey, JSON.stringify(data));
      setAuth(data);
      setLoginForm({ username: "", password: "" });
    } catch (error) {
      setLoginError(error.message);
    } finally {
      setIsLoggingIn(false);
    }
  }

  function handleLogout() {
    localStorage.removeItem(storageKey);
    setAuth(null);
    setCurrentUser(null);
    setDashboard(fallbackMetrics);
    setUnreadCount(0);
    setLoadState("idle");
  }

  return (
    <>
      <a className="skip-link" href="#overview">
        Skip to main content
      </a>
      <main className="app-shell">
        <Sidebar />

        <section className="workspace" id="top">
          <Topbar unreadCount={unreadCount} isSignedIn={Boolean(auth)} onLogout={handleLogout} />

          <HeroSection
            auth={auth}
            user={user}
            role={role}
            greeting={greeting}
            loginForm={loginForm}
            loginError={loginError}
            isLoggingIn={isLoggingIn}
            onLoginChange={setLoginForm}
            onLoginSubmit={handleLogin}
          />

          <MetricsStrip dashboard={dashboard} />

          <section className="content-grid" aria-label="Operational details">
            <OperationsPanel dashboard={dashboard} />
            <WorkflowPanel role={role} workflows={workflows} />
          </section>

          <p className={`status-line ${loadState}`}>
            {loadState === "loading" && "Syncing live records."}
            {loadState === "ready" && "Live records are current."}
            {loadState === "error" && "Some live records could not be loaded."}
            {loadState === "idle" && "Sign in with an existing backend account to load live records."}
          </p>
        </section>
      </main>
    </>
  );
}

function Sidebar() {
  return (
    <aside className="sidebar" aria-label="Main navigation">
      <a className="brand" href="#top" aria-label="Scholr home">
        <span className="brand-mark" aria-hidden="true">
          <span className="brand-circle brand-circle-red" />
          <span className="brand-circle brand-circle-yellow" />
        </span>
        <span>
          <strong>Scholr</strong>
          <small>Academic index</small>
        </span>
      </a>

      <nav className="nav-list">
        {navItems.map(({ label, href, icon: Icon }, index) => (
          <a className={`nav-item ${index === 0 ? "active" : ""}`} href={href} key={label}>
            <Icon size={18} aria-hidden="true" />
            {label}
          </a>
        ))}
      </nav>

      <div className="sidebar-note">
        <ClipboardList size={18} aria-hidden="true" />
        <span>Personal academic operations, tuned for role, rhythm, and priority.</span>
      </div>
    </aside>
  );
}

function Topbar({ unreadCount, isSignedIn, onLogout }) {
  return (
    <header className="topbar">
      <label className="search-field">
        <Search size={18} aria-hidden="true" />
        <span className="sr-only">Search</span>
        <input type="search" placeholder="Search students, courses, reports" />
      </label>

      <div className="topbar-actions">
        <button className="icon-button" type="button" aria-label="Notifications">
          <Bell size={18} aria-hidden="true" />
          <span>{unreadCount}</span>
        </button>
        {isSignedIn ? (
          <button className="soft-button" type="button" onClick={onLogout}>
            <LogOut size={17} aria-hidden="true" />
            Sign out
          </button>
        ) : null}
      </div>
    </header>
  );
}

function HeroSection({
  auth,
  user,
  role,
  greeting,
  loginForm,
  loginError,
  isLoggingIn,
  onLoginChange,
  onLoginSubmit
}) {
  return (
    <section className="hero-panel" id="overview">
      <div className="hero-glow hero-glow-primary" aria-hidden="true" />
      <div className="hero-glow hero-glow-tertiary" aria-hidden="true" />
      <div className="hero-copy-block">
        <p className="eyebrow">{role === "GUEST" ? "Scholr workspace" : role.toLowerCase()}</p>
        <h1>
          <span>Scholr</span>
          <em>{greeting}</em>
        </h1>
        <p className="hero-copy">
          Enrollment, attendance, fees, results, and notifications in a soft,
          adaptive workspace that keeps every academic role moving with less friction.
        </p>

        <div className="rhythm-list" aria-label="Operating rhythm">
          {operatingRhythm.map((item) => (
            <div className="rhythm-item" key={item.code}>
              <strong>{item.code}</strong>
              <span>{item.label}</span>
            </div>
          ))}
        </div>
      </div>

      <aside className="access-panel" aria-label={auth ? "Current profile" : "Sign in"}>
        {auth ? (
          <ProfileCard user={user} role={role} />
        ) : (
          <LoginCard
            form={loginForm}
            error={loginError}
            isLoading={isLoggingIn}
            onChange={onLoginChange}
            onSubmit={onLoginSubmit}
          />
        )}
      </aside>
    </section>
  );
}

function LoginCard({ form, error, isLoading, onChange, onSubmit }) {
  return (
    <form className="login-card" onSubmit={onSubmit}>
      <div className="access-stamp" aria-hidden="true">
        <FileText size={18} />
        Verified desk
      </div>
      <div>
        <p className="eyebrow">Secure access</p>
        <h2>Sign in</h2>
      </div>

      <label>
        Username
        <input
          autoComplete="username"
          value={form.username}
          onChange={(event) => onChange({ ...form, username: event.target.value })}
          placeholder="admin"
        />
      </label>

      <label>
        Password
        <input
          autoComplete="current-password"
          type="password"
          value={form.password}
          onChange={(event) => onChange({ ...form, password: event.target.value })}
          placeholder="Admin@123"
        />
      </label>

      {error ? <p className="form-error">{error}</p> : null}

      <button className="primary-button" type="submit" disabled={isLoading}>
        <span>{isLoading ? "Signing in" : "Enter workspace"}</span>
        <ArrowRight size={17} aria-hidden="true" />
      </button>
    </form>
  );
}

function ProfileCard({ user, role }) {
  return (
    <div className="profile-card">
      <div className="avatar">{user?.username?.slice(0, 2).toUpperCase() || "SC"}</div>
      <div>
        <span>{user?.email || "Signed in"}</span>
        <strong>{role}</strong>
      </div>
    </div>
  );
}

function MetricsStrip({ dashboard }) {
  return (
    <section className="metric-grid" aria-label="Dashboard metrics">
      <MetricCard label="Students" value={dashboard.totalStudents} icon={UsersRound} />
      <MetricCard label="Teachers" value={dashboard.totalTeachers} icon={ShieldCheck} />
      <MetricCard label="Offerings" value={dashboard.totalOfferings} icon={BookOpen} />
      <MetricCard label="Enrollments" value={dashboard.activeEnrollments} icon={CheckCircle2} />
    </section>
  );
}

function MetricCard({ label, value, icon: Icon }) {
  return (
    <article className="metric-card">
      <div className="metric-icon">
        <Icon size={20} aria-hidden="true" />
      </div>
      <span>{label}</span>
      <strong>{formatNumber(value)}</strong>
      <span className="metric-satellite" aria-hidden="true">
        <ArrowUpRight size={20} />
      </span>
    </article>
  );
}

function OperationsPanel({ dashboard }) {
  return (
    <section className="panel spacious" id="academic">
      <div className="section-heading">
        <p className="eyebrow">Ledger</p>
        <h2>Academic pulse</h2>
      </div>

      <div className="pulse-list">
        <PulseItem label="Published results" value={dashboard.publishedResults} status="Ready" />
        <PulseItem label="Outstanding dues" value={dashboard.studentsWithOutstandingDues} status="Review" />
        <PulseItem label="Low attendance cases" value={dashboard.lowAttendanceCases} status="Watch" />
      </div>
    </section>
  );
}

function PulseItem({ label, value, status }) {
  return (
    <div className="pulse-item">
      <div>
        <span>{label}</span>
        <strong>{formatNumber(value)}</strong>
      </div>
      <small>{status}</small>
    </div>
  );
}

function WorkflowPanel({ role, workflows }) {
  return (
    <section className="panel" id="people">
      <div className="section-heading">
        <p className="eyebrow">Actions</p>
        <h2>{role === "GUEST" ? "Role preview" : `${role} actions`}</h2>
      </div>

      <div className="workflow-list">
        {workflows.map((item) => (
          <button type="button" className="workflow-item" key={item}>
            <span>{item}</span>
            <CheckCircle2 size={18} aria-hidden="true" />
          </button>
        ))}
      </div>
    </section>
  );
}

export default App;
