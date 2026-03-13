const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("token");
}

export function setToken(token: string) {
  localStorage.setItem("token", token);
}

export function clearToken() {
  localStorage.removeItem("token");
}

async function fetchAPI<T>(
  path: string,
  options: RequestInit = {},
  { skipRedirect = false }: { skipRedirect?: boolean } = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (res.status === 401 || res.status === 403) {
    // Don't redirect on auth endpoints — just throw the error
    const isAuthEndpoint = path === "/auth/login" || path === "/auth/register";
    if (!isAuthEndpoint && !skipRedirect) {
      clearToken();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
    const text = await res.text();
    throw new Error(text || "Unauthorized");
  }

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `API error: ${res.status}`);
  }

  if (res.status === 204) return {} as T;
  return res.json();
}

// Auth
export async function login(username: string, password: string) {
  const data = await fetchAPI<{ token: string; username: string; email: string; role: string }>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  setToken(data.token);
  return data;
}

export async function register(
  username: string,
  email: string,
  password: string,
  phoneNumber?: string
) {
  const data = await fetchAPI<{ token: string; username: string; email: string; role: string }>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password, phoneNumber }),
  });
  setToken(data.token);
  return data;
}

export async function getProfile() {
  return fetchAPI<{ username: string; email: string }>("/auth/profile");
}

// Dashboard
export async function getDashboard() {
  return fetchAPI<{
    problemsSolved: number;
    revisionStreak: number;
    completionRate: number;
    todayProblems: Array<{
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
    }>;
    revisionTasks: Array<{
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
    }>;
  }>("/dashboard/today");
}

// Problems / Library
export async function getLibrary(params?: {
  search?: string;
  platform?: string;
  difficulty?: string;
  topic?: string;
  sortBy?: string;
  page?: number;
  size?: number;
}) {
  const query = new URLSearchParams();
  if (params?.search) query.set("search", params.search);
  if (params?.platform) query.set("platform", params.platform);
  if (params?.difficulty) query.set("difficulty", params.difficulty);
  if (params?.topic) query.set("topic", params.topic);
  if (params?.sortBy) query.set("sortBy", params.sortBy);
  if (params?.page !== undefined) query.set("page", String(params.page));
  if (params?.size) query.set("size", String(params.size));
  const qs = query.toString();
  return fetchAPI<
    Array<{
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
      url: string;
    }>
  >(`/problems/library${qs ? `?${qs}` : ""}`);
}

export async function createProblem(data: {
  title: string;
  url?: string;
  platform?: string;
  difficulty?: string;
  topicTags?: string;
  notes?: string;
}) {
  return fetchAPI<{ id: number }>("/problems", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

// Study plans
export async function getStudyPlans() {
  return fetchAPI<Array<{
    id: number;
    name: string;
    description: string;
    problems: Array<{
      id: number;
      title: string;
      problemNumber: number | null;
      url: string;
      platform: string;
      difficulty: string;
      topicTags: string;
      dateAdded: string | null;
    }>;
    revisionIntervals: number[] | null;
    reminderTime: string | null;
  }>>("/study-plans");
}

export async function createStudyPlan(data: {
  name: string;
  description?: string;
  problemIds: number[];
  revisionIntervals: number[];
  reminderTime: string;
}) {
  return fetchAPI<{ id: number; name: string }>("/study-plans", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function searchProblems(title: string, limit = 20) {
  return fetchAPI<
    Array<{
      id: number;
      title: string;
      problemNumber: number | null;
      url: string;
      platform: string;
      difficulty: string;
      topicTags: string;
    }>
  >(`/problems/search?title=${encodeURIComponent(title)}&limit=${limit}`);
}

export interface BrowseProblemsParams {
  search?: string;
  platform?: string;
  difficulty?: string;
  topic?: string;
  sortBy?: string;
  page?: number;
  size?: number;
}

export interface BrowseProblemsResponse {
  content: Array<{
    id: number;
    title: string;
    problemNumber: number | null;
    url: string;
    platform: string;
    difficulty: string;
    topicTags: string;
  }>;
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export async function browseProblems(params?: BrowseProblemsParams) {
  const query = new URLSearchParams();
  if (params?.search) query.set("search", params.search);
  if (params?.platform) query.set("platform", params.platform);
  if (params?.difficulty) query.set("difficulty", params.difficulty);
  if (params?.topic) query.set("topic", params.topic);
  if (params?.sortBy) query.set("sortBy", params.sortBy);
  if (params?.page !== undefined) query.set("page", String(params.page));
  if (params?.size) query.set("size", String(params.size));
  const qs = query.toString();
  return fetchAPI<BrowseProblemsResponse>(`/problems/browse${qs ? `?${qs}` : ""}`);
}

export async function getTopics() {
  return fetchAPI<string[]>("/problems/topics");
}

export async function addProblemToStudyPlan(
  studyPlanId: number,
  data: {
    platform: string;
    problemNumber: number;
    revisionIntervals?: number[];
  }
) {
  return fetchAPI(`/study-plans/${studyPlanId}/problems`, {
    method: "POST",
    body: JSON.stringify(data),
  });
}

// Revisions
export async function completeRevision(id: number) {
  return fetchAPI<{ status: string }>(`/revisions/${id}/complete`, {
    method: "PATCH",
  });
}

export async function skipRevision(id: number) {
  return fetchAPI<{ status: string }>(`/revisions/${id}/skip`, {
    method: "PATCH",
  });
}

export async function completeByStudyPlanProblem(sppId: number) {
  return fetchAPI<{ status: string }>(`/revisions/study-plan-problem/${sppId}/complete`, {
    method: "PATCH",
  });
}

export async function skipByStudyPlanProblem(sppId: number) {
  return fetchAPI<{ status: string }>(`/revisions/study-plan-problem/${sppId}/skip`, {
    method: "PATCH",
  });
}

// Calendar
export async function getCalendarRevisions(month: number, year: number) {
  return fetchAPI<{
    month: number;
    year: number;
    events: Record<
      string,
      Array<{
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
      }>
    >;
  }>(`/calendar/revisions?month=${month}&year=${year}`);
}

// Analytics
export async function getAnalyticsProgress() {
  return fetchAPI<{
    totalProblemsSolved: number;
    totalRevisions: number;
    completedRevisions: number;
    skippedRevisions: number;
    pendingRevisions: number;
    completionRate: number;
  }>("/analytics/progress");
}

export async function getAnalyticsTopics() {
  return fetchAPI<{
    strengths: Array<{
      topic: string;
      totalProblems: number;
      completedRevisions: number;
      totalRevisions: number;
      completionRate: number;
    }>;
    weaknesses: Array<{
      topic: string;
      totalProblems: number;
      completedRevisions: number;
      totalRevisions: number;
      completionRate: number;
    }>;
  }>("/analytics/topics");
}

export async function getAnalyticsStreak() {
  return fetchAPI<{
    currentStreak: number;
    longestStreak: number;
    lastActiveDate: string | null;
  }>("/analytics/streak");
}

export async function getAnalyticsTopicMastery() {
  return fetchAPI<{
    topics: Array<{
      topic: string;
      solvedCount: number;
      totalCount: number;
    }>;
  }>("/analytics/topic-mastery");
}

// Notifications
export async function getNotifications() {
  return fetchAPI<
    Array<{
      id: number;
      message: string;
      type: string;
      sent: boolean;
      scheduledTime: string;
      createdAt: string;
    }>
  >("/notifications", {}, { skipRedirect: true });
}

// Settings
export async function getSettings() {
  return fetchAPI<{
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
  }>("/users/settings");
}

export async function updateSettings(data: {
  emailNotifications?: boolean;
  pushNotifications?: boolean;
  smsNotifications?: boolean;
  calendarSync?: boolean;
  darkMode?: boolean;
  newProblemDuration?: number;
  revisionDuration?: number;
  phoneNumber?: string;
  fcmDeviceToken?: string;
}) {
  return fetchAPI<{
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
  }>("/users/settings", {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export async function authorizeCalendar(code: string) {
  return fetchAPI<{ message: string; synced: string }>("/calendar/authorize", {
    method: "POST",
    body: JSON.stringify({ code, redirectUri: "postmessage" }),
  });
}

export async function disconnectCalendar() {
  return fetchAPI<{ message: string }>("/calendar/disconnect", {
    method: "POST",
  });
}
