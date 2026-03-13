const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

function getToken(): string | null {
  return localStorage.getItem("token");
}

export function setToken(token: string) {
  localStorage.setItem("token", token);
}

export function clearToken() {
  localStorage.removeItem("token");
}

async function parseErrorText(res: Response): Promise<string> {
  const text = await res.text();
  if (!text) return "";
  try {
    const json = JSON.parse(text);
    if (json.errors && typeof json.errors === "object") {
      return Object.values(json.errors).join("; ");
    }
    if (json.message) return json.message;
    if (json.error) return json.error;
    return text;
  } catch {
    return text;
  }
}

async function fetchAPI<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401 || res.status === 403) {
    const isAuth = path === "/auth/login" || path === "/auth/register";
    if (!isAuth) {
      clearToken();
      window.location.href = "/login";
    }
    throw new Error(await parseErrorText(res) || "Unauthorized");
  }

  if (!res.ok) {
    throw new Error(await parseErrorText(res) || `API error: ${res.status}`);
  }

  if (res.status === 204) return {} as T;
  return res.json();
}

// ─── Auth ───
export async function login(username: string, password: string) {
  const data = await fetchAPI<{ token: string; username: string; email: string }>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  setToken(data.token);
  return data;
}

export async function register(username: string, email: string, password: string, phoneNumber?: string) {
  const data = await fetchAPI<{ token: string; username: string; email: string }>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password, phoneNumber }),
  });
  setToken(data.token);
  return data;
}

export async function getProfile() {
  return fetchAPI<{ username: string; email: string }>("/auth/profile");
}

// ─── Dashboard ───
export interface DashboardProblem {
  id: number;
  revisionScheduleId: number | null;
  name: string;
  url: string | null;
  platform: string;
  difficulty: string;
  tags: string[];
  time: string;
  isNew: boolean;
  solved: boolean;
  skipped: boolean;
  studyPlanProblemId: number | null;
}

export interface DashboardRevision {
  id: number;
  revisionScheduleId: number;
  name: string;
  url: string | null;
  platform: string;
  difficulty: string;
  tags: string[];
  revision: string;
  time: string;
  overdue: boolean;
  solved: boolean;
  skipped: boolean;
}

export interface DashboardData {
  problemsSolved: number;
  revisionStreak: number;
  completionRate: number;
  todayProblems: DashboardProblem[];
  revisionTasks: DashboardRevision[];
}

export async function getDashboard() {
  return fetchAPI<DashboardData>("/dashboard/today");
}

// ─── Library / Problems ───
export interface LibraryProblem {
  id: number;
  name: string;
  platform: string;
  difficulty: string;
  topics: string[];
  dateSolved: string;
  nextRevision: string;
  status: string;
  revisionsCompleted: number;
  totalRevisions: number;
  url: string | null;
}

export async function getLibrary(params?: Record<string, string>) {
  const qs = params ? "?" + new URLSearchParams(params).toString() : "";
  return fetchAPI<LibraryProblem[]>(`/problems/library${qs}`);
}

export interface BrowseProblem {
  id: number;
  title: string;
  problemNumber: number;
  url: string | null;
  platform: string;
  difficulty: string;
  topicTags: string;
}

export interface BrowsePage {
  content: BrowseProblem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export async function browseProblems(params?: Record<string, string>) {
  const qs = params ? "?" + new URLSearchParams(params).toString() : "";
  return fetchAPI<BrowsePage>(`/problems/browse${qs}`);
}

export async function getTopics() {
  return fetchAPI<string[]>("/problems/topics");
}

export async function createProblem(data: {
  title: string;
  platform: string;
  difficulty: string;
  url?: string;
  topicTags?: string;
}) {
  return fetchAPI<{ id: number }>("/problems", { method: "POST", body: JSON.stringify(data) });
}

// ─── Study Plans ───
export interface StudyPlan {
  id: number;
  name: string;
  description: string;
  problems: StudyPlanProblemResponse[];
  revisionIntervals: number[] | null;
  reminderTime: string | null;
}

export interface StudyPlanProblemResponse {
  id: number;
  title: string;
  problemNumber: number | null;
  url: string | null;
  platform: string;
  difficulty: string;
  topicTags: string | null;
  dateAdded: string | null;
}

export async function getStudyPlans() {
  return fetchAPI<StudyPlan[]>("/study-plans");
}

export async function createStudyPlan(data: {
  name: string;
  description?: string;
  problemIds: number[];
  revisionIntervals: number[];
  reminderTime?: string;
}) {
  return fetchAPI<StudyPlan>("/study-plans", { method: "POST", body: JSON.stringify(data) });
}

export async function addProblemToStudyPlan(planId: number, data: {
  platform: string;
  problemNumber: number;
  revisionIntervals: number[];
}) {
  return fetchAPI<StudyPlan>(`/study-plans/${planId}/problems`, { method: "POST", body: JSON.stringify(data) });
}

// ─── Revisions ───
export async function completeRevision(id: number) {
  return fetchAPI(`/revisions/${id}/complete`, { method: "PATCH" });
}

export async function skipRevision(id: number) {
  return fetchAPI(`/revisions/${id}/skip`, { method: "PATCH" });
}

export async function completeByStudyPlanProblem(sppId: number) {
  return fetchAPI(`/revisions/study-plan-problem/${sppId}/complete`, { method: "PATCH" });
}

export async function skipByStudyPlanProblem(sppId: number) {
  return fetchAPI(`/revisions/study-plan-problem/${sppId}/skip`, { method: "PATCH" });
}

// ─── Calendar ───
export interface CalendarEvent {
  id: number;
  name: string;
  platform: string;
  difficulty: string;
  time: string;
  revision: number;
  totalRevisions: number;
  url: string | null;
  status: string;
  type: string;
}

export interface CalendarData {
  month: number;
  year: number;
  events: Record<string, CalendarEvent[]>;
}

export async function getCalendarRevisions(month: number, year: number) {
  return fetchAPI<CalendarData>(`/calendar/revisions?month=${month}&year=${year}`);
}

export async function authorizeCalendar(code: string) {
  return fetchAPI<{ message: string }>("/calendar/authorize", {
    method: "POST",
    body: JSON.stringify({ code, redirectUri: "postmessage" }),
  });
}

export async function disconnectCalendar() {
  return fetchAPI<{ message: string }>("/calendar/disconnect", { method: "POST" });
}

// ─── Analytics ───
export interface AnalyticsProgress {
  totalProblemsSolved: number;
  totalRevisions: number;
  completedRevisions: number;
  skippedRevisions: number;
  pendingRevisions: number;
  completionRate: number;
}

export interface TopicMastery {
  topics: { topic: string; solvedCount: number; totalCount: number }[];
}

export interface StreakData {
  currentStreak: number;
  longestStreak: number;
  lastActiveDate: string;
}

export async function getAnalyticsProgress() {
  return fetchAPI<AnalyticsProgress>("/analytics/progress");
}

export async function getAnalyticsTopicMastery() {
  return fetchAPI<TopicMastery>("/analytics/topic-mastery");
}

export async function getAnalyticsStreak() {
  return fetchAPI<StreakData>("/analytics/streak");
}

// ─── Notifications ───
export interface Notification {
  id: number;
  message: string;
  type: string;
  sent: boolean;
  scheduledTime: string;
  createdAt: string;
}

export async function getNotifications() {
  return fetchAPI<Notification[]>("/notifications");
}

// ─── Settings ───
export interface Settings {
  username: string;
  email: string;
  phoneNumber: string | null;
  emailNotifications: boolean;
  pushNotifications: boolean;
  smsNotifications: boolean;
  calendarSync: boolean;
  darkMode: boolean;
  newProblemDuration: number;
  revisionDuration: number;
  googleCalendarConnected: boolean;
}

export async function getSettings() {
  return fetchAPI<Settings>("/users/settings");
}

export async function updateSettings(data: Partial<Settings>) {
  return fetchAPI<Settings>("/users/settings", { method: "PUT", body: JSON.stringify(data) });
}
