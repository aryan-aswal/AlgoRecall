import { useState, useEffect } from "react";
import {
  getAnalyticsProgress, getAnalyticsTopicMastery, getAnalyticsStreak,
  type AnalyticsProgress, type TopicMastery, type StreakData,
} from "../lib/api";
import { toast } from "sonner";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";
import { Target, TrendingUp, Flame, BarChart3 } from "lucide-react";
import { cn } from "../lib/utils";

export default function Analytics() {
  const [progress, setProgress] = useState<AnalyticsProgress | null>(null);
  const [mastery, setMastery] = useState<TopicMastery | null>(null);
  const [streak, setStreak] = useState<StreakData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getAnalyticsProgress(), getAnalyticsTopicMastery(), getAnalyticsStreak()])
      .then(([p, m, s]) => { setProgress(p); setMastery(m); setStreak(s); })
      .catch(() => toast.error("Failed to load analytics"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!progress) return null;

  const REVISION_COLORS = {
    completed: "#22C55E",
    skipped: "#F59E0B",
    pending: "#8B5CF6",
  };

  const donutData = [
    { name: "Completed", value: progress.completedRevisions, color: REVISION_COLORS.completed },
    { name: "Skipped", value: progress.skippedRevisions, color: REVISION_COLORS.skipped },
    { name: "Pending", value: progress.pendingRevisions, color: REVISION_COLORS.pending },
  ].filter((d) => d.value > 0);

  const topicsList = mastery?.topics || [];
  const sortedTopics = [...topicsList].sort((a, b) => b.solvedCount - a.solvedCount);

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Analytics</h1>
        <p className="text-muted-foreground">Track your progress and growth</p>
      </div>

      {/* Hero stats */}
      <div className="bg-gradient-to-r from-primary to-primary/80 rounded-xl p-6 text-white">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div>
            <div className="flex items-center gap-2 text-white/70 text-sm mb-1">
              <Target className="w-4 h-4" /> Problems Solved
            </div>
            <p className="text-3xl font-bold">{progress.totalProblemsSolved}</p>
          </div>
          <div>
            <div className="flex items-center gap-2 text-white/70 text-sm mb-1">
              <TrendingUp className="w-4 h-4" /> Completion Rate
            </div>
            <p className="text-3xl font-bold">{progress.completionRate}%</p>
          </div>
          <div>
            <div className="flex items-center gap-2 text-white/70 text-sm mb-1">
              <Flame className="w-4 h-4" /> Current Streak
            </div>
            <p className="text-3xl font-bold">{streak?.currentStreak || 0} days</p>
          </div>
          <div>
            <div className="flex items-center gap-2 text-white/70 text-sm mb-1">
              <BarChart3 className="w-4 h-4" /> Best Streak
            </div>
            <p className="text-3xl font-bold">{streak?.longestStreak || 0} days</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Donut chart */}
        <div className="bg-card border border-border rounded-xl p-6">
          <h2 className="text-lg font-semibold mb-4">Revision Breakdown</h2>
          {donutData.length === 0 ? (
            <p className="text-center text-muted-foreground py-12">No revision data yet</p>
          ) : (
            <div className="flex flex-col items-center gap-6 mt-6 mb-2">
              {/* Donut Chart */}
              <div className="w-64 h-64 shrink-0 relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={donutData}
                      cx="50%"
                      cy="50%"
                      innerRadius={85}
                      outerRadius={120}
                      paddingAngle={4}
                      dataKey="value"
                      strokeWidth={2}
                      stroke="var(--background)"
                    >
                      {donutData.map((d, i) => (
                        <Cell key={i} fill={d.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "hsl(var(--card))",
                        border: "1px solid hsl(var(--border))",
                        borderRadius: "8px",
                        fontSize: "13px",
                        padding: "6px 10px",
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>

              {/* Legend with stats in one line */}
              <div className="w-full flex flex-row flex-wrap justify-center gap-6 mt-2">
                {donutData.map((d) => (
                  <div key={d.name} className="flex items-center gap-2">
                    <div className="w-3.5 h-3.5 rounded-full shrink-0" style={{ backgroundColor: d.color }} />
                    <span className="text-sm text-foreground">{d.name}</span>
                    <span className="text-sm font-bold text-foreground">{d.value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Topic mastery */}
        <div className="bg-card border border-border rounded-xl p-6">
          <h2 className="text-lg font-semibold mb-4">Topic Mastery</h2>
          {sortedTopics.length === 0 ? (
            <p className="text-center text-muted-foreground py-12">No topic data yet</p>
          ) : (
            <div className="space-y-4 max-h-[400px] overflow-auto pr-2">
              {sortedTopics.map((t) => {
                const pct = t.totalCount > 0 ? Math.round((t.solvedCount / t.totalCount) * 100) : 0;
                return (
                  <div key={t.topic}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-sm font-medium truncate">{t.topic}</span>
                      <span className="text-xs text-muted-foreground shrink-0 ml-2">
                        {t.solvedCount}/{t.totalCount} ({pct}%)
                      </span>
                    </div>
                    <div className="h-2 bg-muted rounded-full overflow-hidden">
                      <div
                        className={cn(
                          "h-full rounded-full transition-all",
                          pct >= 70 ? "bg-green-500" : pct >= 40 ? "bg-yellow-500" : "bg-primary"
                        )}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
