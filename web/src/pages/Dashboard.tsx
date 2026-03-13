import { useState, useEffect } from "react";
import {
  getDashboard, completeRevision, skipRevision,
  completeByStudyPlanProblem, skipByStudyPlanProblem,
  type DashboardData, type DashboardProblem, type DashboardRevision,
} from "../lib/api";
import { toast } from "sonner";
import {
  SkipForward, ExternalLink, Flame, Target,
  TrendingUp, BookOpen, RotateCcw, Clock, Check,
} from "lucide-react";
import { cn } from "../lib/utils";

function getDifficultyColor(d: string) {
  const u = d.toUpperCase();
  if (u === "EASY") return "text-green-600 bg-green-50 dark:bg-green-400/10";
  if (u === "MEDIUM") return "text-yellow-600 bg-yellow-50 dark:bg-yellow-400/10";
  if (u === "HARD") return "text-red-600 bg-red-50 dark:bg-red-400/10";
  return "text-muted-foreground bg-muted";
}

function getPlatformColor(p: string) {
  const u = p.toUpperCase();
  if (u === "LEETCODE") return "text-amber-600 bg-amber-50 dark:bg-amber-400/10";
  if (u === "GFG") return "text-emerald-600 bg-emerald-50 dark:bg-emerald-400/10";
  if (u === "CODEFORCES") return "text-blue-600 bg-blue-50 dark:bg-blue-400/10";
  return "text-muted-foreground bg-muted";
}

export default function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [tab, setTab] = useState<"problems" | "revisions">("problems");
  const [loading, setLoading] = useState(true);
  const [confirmAction, setConfirmAction] = useState<{
    type: "complete" | "skip";
    item: DashboardProblem | DashboardRevision;
    isRevision: boolean;
  } | null>(null);

  const fetchData = () => {
    getDashboard().then(setData).catch(() => toast.error("Failed to load dashboard")).finally(() => setLoading(false));
  };

  useEffect(fetchData, []);

  const handleAction = async () => {
    if (!confirmAction) return;
    const { type, item, isRevision } = confirmAction;
    try {
      if (isRevision) {
        const rev = item as DashboardRevision;
        if (type === "complete") await completeRevision(rev.revisionScheduleId);
        else await skipRevision(rev.revisionScheduleId);
      } else {
        const prob = item as DashboardProblem;
        if (prob.studyPlanProblemId) {
          if (type === "complete") await completeByStudyPlanProblem(prob.studyPlanProblemId);
          else await skipByStudyPlanProblem(prob.studyPlanProblemId);
        }
      }
      toast.success(type === "complete" ? "Marked as completed!" : "Skipped!");
      setConfirmAction(null);
      fetchData();
    } catch {
      toast.error("Action failed");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!data) return null;

  const stats = [
    { label: "Problems Solved", value: data.problemsSolved, icon: Target, color: "text-primary" },
    { label: "Revision Streak", value: `${data.revisionStreak} days`, icon: Flame, color: "text-orange-500" },
    { label: "Completion Rate", value: `${data.completionRate}%`, icon: TrendingUp, color: "text-green-500" },
  ];

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground">Your daily overview</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {stats.map((s) => (
          <div key={s.label} className="bg-card border border-border rounded-xl p-5 flex items-center gap-4">
            <div className={cn("w-12 h-12 rounded-xl flex items-center justify-center bg-muted", s.color)}>
              <s.icon className="w-6 h-6" />
            </div>
            <div>
              <p className="text-2xl font-bold">{s.value}</p>
              <p className="text-sm text-muted-foreground">{s.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Tab toggle */}
      <div className="flex gap-1 bg-muted p-1 rounded-lg w-fit">
        <button
          onClick={() => setTab("problems")}
          className={cn(
            "px-4 py-2 rounded-md text-sm font-medium transition-colors flex items-center gap-2",
            tab === "problems" ? "bg-card text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
          )}
        >
          <BookOpen className="w-4 h-4" />
          Today's Problems ({data.todayProblems.length})
        </button>
        <button
          onClick={() => setTab("revisions")}
          className={cn(
            "px-4 py-2 rounded-md text-sm font-medium transition-colors flex items-center gap-2",
            tab === "revisions" ? "bg-card text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
          )}
        >
          <RotateCcw className="w-4 h-4" />
          Revisions ({data.revisionTasks.length})
        </button>
      </div>

      {/* Problem / Revision Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {tab === "problems" ? (
          data.todayProblems.length === 0 ? (
            <p className="text-muted-foreground col-span-2 text-center py-12">No problems scheduled for today</p>
          ) : (
            data.todayProblems.map((p) => (
              <div key={p.id} className="bg-card border border-border rounded-xl p-5 flex flex-col gap-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1">
                    <h3 className="font-semibold text-foreground">{p.name}</h3>
                    <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                      <span className={cn("text-xs font-medium px-2 py-0.5 rounded-full", getPlatformColor(p.platform))}>
                        {p.platform}
                      </span>
                      <span className={cn("text-xs font-medium px-2 py-0.5 rounded-full", getDifficultyColor(p.difficulty))}>
                        {p.difficulty}
                      </span>
                      {p.isNew && (
                        <span className="text-xs font-medium px-2 py-0.5 rounded-full text-primary bg-primary/10">NEW</span>
                      )}
                      {p.solved && (
                        <span className="text-xs font-medium px-2 py-0.5 rounded-full text-green-600 bg-green-50 dark:bg-green-400/10">SOLVED</span>
                      )}
                      {p.skipped && (
                        <span className="text-xs font-medium px-2 py-0.5 rounded-full text-yellow-600 bg-yellow-50 dark:bg-yellow-400/10">SKIPPED</span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-xs text-muted-foreground"><Clock className="w-3 h-3" />{p.time}</div>
                </div>
                {p.tags.length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {p.tags.slice(0, 4).map((t) => (
                      <span key={t} className="text-xs bg-muted text-muted-foreground px-2 py-0.5 rounded">{t}</span>
                    ))}
                  </div>
                )}
                <div className="flex gap-2 mt-1">
                  {p.url && (
                    <a
                      href={p.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center justify-center gap-1.5 h-7 flex-1 bg-primary hover:bg-primary/90 text-primary-foreground rounded-md text-xs font-medium transition-colors"
                    >
                      <ExternalLink className="w-3.5 h-3.5" /> Open
                    </a>
                  )}
                  {p.studyPlanProblemId && !p.solved && !p.skipped && (
                    <>
                      <button
                        onClick={() => setConfirmAction({ type: "complete", item: p, isRevision: false })}
                        className="flex items-center justify-center gap-1.5 h-7 px-2.5 border border-border rounded-md text-xs font-medium text-foreground hover:bg-muted ml-auto bg-background transition-colors"
                      >
                        <Check className="w-3.5 h-3.5" /> Complete
                      </button>
                      <button
                        onClick={() => setConfirmAction({ type: "skip", item: p, isRevision: false })}
                        className="flex items-center justify-center gap-1.5 h-7 px-2.5 border border-border rounded-md text-xs font-medium text-foreground hover:bg-muted bg-background transition-colors"
                      >
                        <SkipForward className="w-3.5 h-3.5" /> Skip
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))
          )
        ) : data.revisionTasks.length === 0 ? (
          <p className="text-muted-foreground col-span-2 text-center py-12">No revisions due</p>
        ) : (
          data.revisionTasks.map((r) => (
            <div key={r.id} className={cn("bg-card border rounded-xl p-5 flex flex-col gap-3", r.overdue ? "border-destructive/50" : "border-border")}>
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1">
                  <h3 className="font-semibold text-foreground">{r.name}</h3>
                  <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                    <span className={cn("text-xs font-medium px-2 py-0.5 rounded-full", getPlatformColor(r.platform))}>
                      {r.platform}
                    </span>
                    <span className={cn("text-xs font-medium px-2 py-0.5 rounded-full", getDifficultyColor(r.difficulty))}>
                      {r.difficulty}
                    </span>
                    <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-primary/10 text-primary">
                      Rev {r.revision}
                    </span>
                    {r.overdue && (
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-destructive/10 text-destructive">OVERDUE</span>
                    )}
                    {r.solved && (
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full text-green-600 bg-green-50 dark:bg-green-400/10">COMPLETED</span>
                    )}
                    {r.skipped && (
                      <span className="text-xs font-medium px-2 py-0.5 rounded-full text-yellow-600 bg-yellow-50 dark:bg-yellow-400/10">SKIPPED</span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1 text-xs text-muted-foreground"><Clock className="w-3 h-3" />{r.time}</div>
              </div>
              <div className="flex gap-2 mt-1">
                {r.url && (
                  <a
                    href={r.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center justify-center gap-1.5 h-7 flex-1 bg-primary hover:bg-primary/90 text-primary-foreground rounded-md text-xs font-medium transition-colors"
                  >
                    <ExternalLink className="w-3.5 h-3.5" /> Open
                  </a>
                )}
                {!r.solved && !r.skipped && (
                  <>
                    <button
                      onClick={() => setConfirmAction({ type: "complete", item: r, isRevision: true })}
                      className="flex items-center justify-center gap-1.5 h-7 px-2.5 border border-border rounded-md text-xs font-medium text-foreground hover:bg-muted ml-auto bg-background transition-colors"
                    >
                      <Check className="w-3.5 h-3.5" /> Complete
                    </button>
                    <button
                      onClick={() => setConfirmAction({ type: "skip", item: r, isRevision: true })}
                      className="flex items-center justify-center gap-1.5 h-7 px-2.5 border border-border rounded-md text-xs font-medium text-foreground hover:bg-muted bg-background transition-colors"
                    >
                      <SkipForward className="w-3.5 h-3.5" /> Skip
                    </button>
                  </>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Confirmation dialog */}
      {confirmAction && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={() => setConfirmAction(null)}>
          <div className="bg-card border border-border rounded-xl p-6 max-w-sm w-full mx-4 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold">
              {confirmAction.type === "complete" ? "Complete" : "Skip"} this{" "}
              {confirmAction.isRevision ? "revision" : "problem"}?
            </h3>
            <p className="text-sm text-muted-foreground mt-2">
              {confirmAction.type === "complete"
                ? "This will mark it as completed."
                : "This will skip it for now."}
            </p>
            <div className="flex justify-end gap-3 mt-6">
              <button
                onClick={() => setConfirmAction(null)}
                className="px-4 py-2 text-sm font-medium rounded-lg border border-border hover:bg-muted"
              >
                Cancel
              </button>
              <button
                onClick={handleAction}
                className={cn(
                  "px-4 py-2 text-sm font-medium rounded-lg text-white",
                  confirmAction.type === "complete" ? "bg-green-600 hover:bg-green-700" : "bg-muted-foreground hover:bg-muted-foreground/80"
                )}
              >
                {confirmAction.type === "complete" ? "Complete" : "Skip"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
