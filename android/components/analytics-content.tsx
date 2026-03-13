"use client"

import { useState, useEffect } from "react"
import { Flame, CheckCircle2, Target, RotateCcw, SkipForward, Clock, Trophy, Zap, BookOpen } from "lucide-react"
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts"
import { getAnalyticsProgress, getAnalyticsTopicMastery, getAnalyticsStreak } from "@/lib/api"

const REVISION_COLORS = {
  completed: "#22C55E",
  skipped: "#F59E0B",
  pending: "#8B5CF6",
}

const TOPIC_COLORS = [
  "#6366F1", "#EC4899", "#F59E0B", "#22C55E", "#3B82F6",
  "#EF4444", "#14B8A6", "#E2703A", "#8B5CF6", "#1F8ACB",
]

export function AnalyticsContent() {
  const [progress, setProgress] = useState<any>(null)
  const [topicMastery, setTopicMastery] = useState<any>(null)
  const [streak, setStreak] = useState<any>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([getAnalyticsProgress(), getAnalyticsTopicMastery(), getAnalyticsStreak()])
      .then(([p, t, s]) => { setProgress(p); setTopicMastery(t); setStreak(s) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const revisionData = progress ? [
    { name: "Completed", value: progress.completedRevisions || 0, color: REVISION_COLORS.completed },
    { name: "Skipped", value: progress.skippedRevisions || 0, color: REVISION_COLORS.skipped },
    { name: "Pending", value: progress.pendingRevisions || 0, color: REVISION_COLORS.pending },
  ].filter(d => d.value > 0) : []

  const totalRevisions = revisionData.reduce((s, d) => s + d.value, 0)

  const allTopics: Array<{ topic: string; solvedCount: number; totalCount: number }> = topicMastery?.topics || []

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-muted-foreground">Loading analytics...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full overflow-y-auto overflow-x-hidden">
      {/* Gradient Header */}
      <div className="px-4 pt-4 pb-3">
        <div className="rounded-2xl bg-linear-to-br from-[#6366F1] to-[#8B5CF6] p-4 text-white">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h1 className="text-lg font-bold">Analytics</h1>
              <p className="text-xs text-white/70">Your coding journey</p>
            </div>
            <div className="flex items-center gap-1.5 bg-white/20 rounded-full px-3 py-1.5">
              <Flame className="w-4 h-4 text-orange-300" />
              <span className="text-sm font-bold">{streak?.currentStreak || 0}</span>
              <span className="text-xs text-white/80">day streak</span>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div className="bg-white/15 rounded-xl p-2.5 text-center">
              <p className="text-xl font-bold">{progress?.totalProblemsSolved ?? 0}</p>
              <p className="text-[10px] text-white/70 mt-0.5">Problems</p>
            </div>
            <div className="bg-white/15 rounded-xl p-2.5 text-center">
              <p className="text-xl font-bold">{progress ? `${Math.round(progress.completionRate)}%` : "0%"}</p>
              <p className="text-[10px] text-white/70 mt-0.5">Completion</p>
            </div>
            <div className="bg-white/15 rounded-xl p-2.5 text-center">
              <p className="text-xl font-bold">{streak?.longestStreak ?? 0}</p>
              <p className="text-[10px] text-white/70 mt-0.5">Best Streak</p>
            </div>
          </div>
        </div>
      </div>

      <div className="px-4 pb-4 flex flex-col gap-3">
        {/* Revision Breakdown */}
        <div className="rounded-2xl border border-border bg-card p-3">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-7 h-7 rounded-lg bg-[#8B5CF6]/10 flex items-center justify-center">
              <RotateCcw className="w-3.5 h-3.5 text-[#8B5CF6]" />
            </div>
            <h2 className="text-sm font-semibold text-foreground">Revision Breakdown</h2>
          </div>

          {revisionData.length > 0 ? (
            <div className="flex items-center gap-3">
              {/* Donut Chart */}
              <div className="w-25 h-25 shrink-0 relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={revisionData}
                      cx="50%"
                      cy="50%"
                      innerRadius={28}
                      outerRadius={44}
                      paddingAngle={3}
                      dataKey="value"
                      strokeWidth={0}
                    >
                      {revisionData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        backgroundColor: "hsl(var(--card))",
                        border: "1px solid hsl(var(--border))",
                        borderRadius: "8px",
                        fontSize: "11px",
                        padding: "4px 8px",
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-xs font-bold text-foreground">{totalRevisions}</span>
                </div>
              </div>

              {/* Legend with stats */}
              <div className="flex-1 flex flex-col gap-2">
                {[
                  { label: "Completed", value: progress?.completedRevisions || 0, color: REVISION_COLORS.completed, icon: CheckCircle2 },
                  { label: "Skipped", value: progress?.skippedRevisions || 0, color: REVISION_COLORS.skipped, icon: SkipForward },
                  { label: "Pending", value: progress?.pendingRevisions || 0, color: REVISION_COLORS.pending, icon: Clock },
                ].map((item) => (
                  <div key={item.label} className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-md flex items-center justify-center" style={{ backgroundColor: `${item.color}15` }}>
                      <item.icon className="w-3 h-3" style={{ color: item.color }} />
                    </div>
                    <div className="flex-1 flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">{item.label}</span>
                      <span className="text-xs font-semibold text-foreground">{item.value}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-xs text-muted-foreground text-center py-4">No revision data yet</p>
          )}
        </div>

        {/* Streak Card */}
        <div className="rounded-2xl border border-border bg-card p-3">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-7 h-7 rounded-lg bg-orange-500/10 flex items-center justify-center">
              <Zap className="w-3.5 h-3.5 text-orange-500" />
            </div>
            <h2 className="text-sm font-semibold text-foreground">Streak Stats</h2>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <div className="rounded-xl bg-linear-to-br from-orange-500/10 to-orange-500/5 p-3 text-center">
              <Flame className="w-5 h-5 text-orange-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-foreground">{streak?.currentStreak || 0}</p>
              <p className="text-[10px] text-muted-foreground">Current</p>
            </div>
            <div className="rounded-xl bg-linear-to-br from-amber-500/10 to-amber-500/5 p-3 text-center">
              <Trophy className="w-5 h-5 text-amber-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-foreground">{streak?.longestStreak || 0}</p>
              <p className="text-[10px] text-muted-foreground">Best</p>
            </div>
            <div className="rounded-xl bg-linear-to-br from-emerald-500/10 to-emerald-500/5 p-3 text-center">
              <Target className="w-5 h-5 text-emerald-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-foreground">{progress ? `${Math.round(progress.completionRate)}%` : "0%"}</p>
              <p className="text-[10px] text-muted-foreground">Rate</p>
            </div>
          </div>
        </div>

        {/* Topic Mastery */}
        {allTopics.length > 0 && (
          <div className="rounded-2xl border border-border bg-card p-3">
            <div className="flex items-center gap-2 mb-3">
              <div className="w-7 h-7 rounded-lg bg-[#3B82F6]/10 flex items-center justify-center">
                <BookOpen className="w-3.5 h-3.5 text-[#3B82F6]" />
              </div>
              <h2 className="text-sm font-semibold text-foreground">Topic Mastery</h2>
            </div>
            <div className="flex flex-col gap-2.5">
              {allTopics.map((topic, idx) => {
                const color = TOPIC_COLORS[idx % TOPIC_COLORS.length]
                const pct = topic.totalCount > 0 ? Math.min(Math.round((topic.solvedCount / topic.totalCount) * 100), 100) : 0
                return (
                  <div key={topic.topic}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-medium text-foreground truncate mr-2">{topic.topic}</span>
                      <div className="flex items-center gap-1 shrink-0">
                        <span className="text-[10px] font-bold" style={{ color }}>
                          {topic.solvedCount}
                        </span>
                        <span className="text-[10px] text-muted-foreground">
                          / {topic.totalCount} problems
                        </span>
                      </div>
                    </div>
                    <div className="h-1.5 rounded-full bg-muted overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all duration-500"
                        style={{ width: `${Math.max(pct, 2)}%`, backgroundColor: color }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
