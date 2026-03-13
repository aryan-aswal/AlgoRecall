"use client"

import { useState, useEffect } from "react"
import { BookOpen, ArrowLeft, ExternalLink, Clock, Target, CalendarDays } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { getStudyPlans } from "@/lib/api"

const PLAN_COLORS = [
  { bg: "from-[#6366F1] to-[#8B5CF6]", light: "bg-[#6366F1]/10", text: "text-[#6366F1]" },
  { bg: "from-[#E2703A] to-[#F59E0B]", light: "bg-[#E2703A]/10", text: "text-[#E2703A]" },
  { bg: "from-[#22C55E] to-[#10B981]", light: "bg-[#22C55E]/10", text: "text-[#22C55E]" },
  { bg: "from-[#1F8ACB] to-[#6366F1]", light: "bg-[#1F8ACB]/10", text: "text-[#1F8ACB]" },
  { bg: "from-[#EF4444] to-[#E2703A]", light: "bg-[#EF4444]/10", text: "text-[#EF4444]" },
  { bg: "from-[#F59E0B] to-[#22C55E]", light: "bg-[#F59E0B]/10", text: "text-[#F59E0B]" },
]

function getDifficultyColor(difficulty: string) {
  switch (difficulty?.toUpperCase()) {
    case "EASY": return "bg-[#22C55E]/10 text-[#22C55E] border-[#22C55E]/20"
    case "MEDIUM": return "bg-[#F59E0B]/10 text-[#F59E0B] border-[#F59E0B]/20"
    case "HARD": return "bg-[#EF4444]/10 text-[#EF4444] border-[#EF4444]/20"
    default: return "bg-muted text-muted-foreground"
  }
}

function getPlatformColor(platform: string) {
  switch (platform?.toUpperCase()) {
    case "LEETCODE": return "bg-[#E2703A]/10 text-[#E2703A]"
    case "CODEFORCES": return "bg-[#1F8ACB]/10 text-[#1F8ACB]"
    case "GFG": return "bg-[#2F8D46]/10 text-[#2F8D46]"
    case "HACKERRANK": return "bg-[#1BA94C]/10 text-[#1BA94C]"
    case "CODECHEF": return "bg-[#5B4638]/10 text-[#5B4638]"
    default: return "bg-muted text-muted-foreground"
  }
}

function formatPlatform(p: string) {
  if (!p) return "Unknown"
  switch (p.toUpperCase()) {
    case "LEETCODE": return "LeetCode"
    case "GFG": return "GFG"
    case "CODEFORCES": return "Codeforces"
    case "HACKERRANK": return "HackerRank"
    case "CODECHEF": return "CodeChef"
    default: return p
  }
}

function formatDifficulty(d: string) {
  if (!d) return "Medium"
  switch (d.toUpperCase()) {
    case "EASY": return "Easy"
    case "MEDIUM": return "Medium"
    case "HARD": return "Hard"
    default: return d
  }
}

interface StudyPlan {
  id: number
  name: string
  description: string
  problems: Array<{
    id: number
    title: string
    problemNumber: number | null
    url: string
    platform: string
    difficulty: string
    topicTags: string
    dateAdded: string | null
  }>
  revisionIntervals: number[] | null
  reminderTime: string | null
}

export function LibraryContent() {
  const [plans, setPlans] = useState<StudyPlan[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedPlan, setSelectedPlan] = useState<StudyPlan | null>(null)

  useEffect(() => {
    getStudyPlans()
      .then(setPlans)
      .catch(() => setPlans([]))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="px-4 py-4 flex items-center justify-center h-full">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    )
  }

  // Detail view for a selected study plan
  if (selectedPlan) {
    const colorIdx = plans.findIndex((p) => p.id === selectedPlan.id) % PLAN_COLORS.length
    const color = PLAN_COLORS[colorIdx]
    const easyCount = selectedPlan.problems.filter((p) => p.difficulty?.toUpperCase() === "EASY").length
    const mediumCount = selectedPlan.problems.filter((p) => p.difficulty?.toUpperCase() === "MEDIUM").length
    const hardCount = selectedPlan.problems.filter((p) => p.difficulty?.toUpperCase() === "HARD").length

    return (
      <div className="px-4 py-4">
        <button onClick={() => setSelectedPlan(null)} className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground mb-4 transition-colors">
          <ArrowLeft className="w-4 h-4" />Back to Library
        </button>

        {/* Plan Header Card */}
        <div className={cn("rounded-xl bg-linear-to-br p-4 mb-4 text-white", color.bg)}>
          <h1 className="text-lg font-bold">{selectedPlan.name}</h1>
          {selectedPlan.description && (
            <p className="text-sm text-white/80 mt-1">{selectedPlan.description}</p>
          )}
          <div className="flex items-center gap-3 mt-3">
            <div className="flex items-center gap-1.5 bg-white/20 rounded-full px-2.5 py-1">
              <Target className="w-3.5 h-3.5" />
              <span className="text-xs font-medium">{selectedPlan.problems.length} Problems</span>
            </div>
            {selectedPlan.revisionIntervals && (
              <div className="flex items-center gap-1.5 bg-white/20 rounded-full px-2.5 py-1">
                <Clock className="w-3.5 h-3.5" />
                <span className="text-xs font-medium">{selectedPlan.revisionIntervals.length} Revisions</span>
              </div>
            )}
          </div>
          {/* Difficulty breakdown */}
          <div className="flex items-center gap-2 mt-3">
            {easyCount > 0 && <Badge className="bg-white/20 text-white border-0 text-[10px] hover:bg-white/20">{easyCount} Easy</Badge>}
            {mediumCount > 0 && <Badge className="bg-white/20 text-white border-0 text-[10px] hover:bg-white/20">{mediumCount} Medium</Badge>}
            {hardCount > 0 && <Badge className="bg-white/20 text-white border-0 text-[10px] hover:bg-white/20">{hardCount} Hard</Badge>}
          </div>
          {/* Revision Policy */}
          {selectedPlan.revisionIntervals && selectedPlan.revisionIntervals.length > 0 && (
            <div className="mt-3 bg-white/15 rounded-lg p-2.5">
              <div className="flex items-center gap-1.5 mb-1">
                <CalendarDays className="w-3.5 h-3.5" />
                <span className="text-xs font-semibold">Revision Policy</span>
              </div>
              <p className="text-[11px] text-white/80">
                Revisions on Day {selectedPlan.revisionIntervals.join(", Day ")} after adding each problem
              </p>
            </div>
          )}
        </div>

        {/* Problems List */}
        <h2 className="text-sm font-semibold text-foreground mb-3">Problems ({selectedPlan.problems.length})</h2>
        <div className="flex flex-col gap-2">
          {selectedPlan.problems.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">No problems in this plan yet</p>
          ) : (
            selectedPlan.problems.map((problem, idx) => {
              // Compute revision dates from dateAdded + intervals
              const revDates: string[] = []
              if (problem.dateAdded && selectedPlan.revisionIntervals) {
                const base = new Date(problem.dateAdded + "T00:00:00")
                for (const interval of selectedPlan.revisionIntervals) {
                  const d = new Date(base)
                  d.setDate(d.getDate() + interval)
                  revDates.push(d.toLocaleDateString("en-US", { month: "short", day: "numeric" }))
                }
              }
              return (
              <Card key={problem.id} className="bg-card border-border shadow-sm overflow-hidden">
                <CardContent className="p-3">
                  <div className="flex items-start gap-3">
                    <div className={cn("w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0 mt-0.5", color.light, color.text)}>
                      {idx + 1}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-sm font-medium text-foreground truncate">{problem.title}</h3>
                      <div className="flex items-center gap-2 mt-1.5">
                        <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4", getPlatformColor(problem.platform))}>
                          {formatPlatform(problem.platform)}
                        </Badge>
                        <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4 border", getDifficultyColor(problem.difficulty))}>
                          {formatDifficulty(problem.difficulty)}
                        </Badge>
                      </div>
                      {problem.topicTags && (
                        <div className="flex flex-wrap gap-1 mt-1.5">
                          {problem.topicTags.split(",").slice(0, 3).map((tag) => (
                            <Badge key={tag.trim()} variant="secondary" className="text-[9px] px-1.5 py-0 h-3.5 bg-accent text-accent-foreground">
                              {tag.trim()}
                            </Badge>
                          ))}
                        </div>
                      )}
                      {revDates.length > 0 && (
                        <div className="flex items-center gap-1 mt-1.5 text-[10px] text-muted-foreground">
                          <CalendarDays className="w-3 h-3 shrink-0" />
                          <span className="truncate">Rev: {revDates.join(" → ")}</span>
                        </div>
                      )}
                    </div>
                    {problem.url && (
                      <Button size="sm" variant="ghost" className="h-7 w-7 p-0 shrink-0"
                        onClick={() => window.open(problem.url, "_blank")}>
                        <ExternalLink className="w-3.5 h-3.5 text-muted-foreground" />
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
              )
            })
          )}
        </div>
      </div>
    )
  }

  // Library main view — study plan cards
  return (
    <div className="px-4 py-4">
      <div className="mb-5">
        <h1 className="text-xl font-semibold text-foreground">Library</h1>
        <p className="text-sm text-muted-foreground">{plans.length} study plan{plans.length !== 1 ? "s" : ""}</p>
      </div>

      {plans.length === 0 ? (
        <div className="text-center py-16">
          <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-primary/10 flex items-center justify-center">
            <BookOpen className="w-8 h-8 text-primary" />
          </div>
          <p className="font-medium text-foreground">No study plans yet</p>
          <p className="text-sm text-muted-foreground mt-1">Create one from the + tab to get started</p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {plans.map((plan, idx) => {
            const color = PLAN_COLORS[idx % PLAN_COLORS.length]
            const easyCount = plan.problems.filter((p) => p.difficulty?.toUpperCase() === "EASY").length
            const mediumCount = plan.problems.filter((p) => p.difficulty?.toUpperCase() === "MEDIUM").length
            const hardCount = plan.problems.filter((p) => p.difficulty?.toUpperCase() === "HARD").length

            return (
              <button key={plan.id} onClick={() => setSelectedPlan(plan)}
                className="text-left w-full group">
                <Card className="bg-card border-border shadow-sm overflow-hidden transition-all duration-200 group-hover:shadow-md group-hover:border-primary/30">
                  <CardContent className="p-0">
                    <div className={cn("h-2 bg-linear-to-r", color.bg)} />
                    <div className="p-3">
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <h3 className="font-semibold text-foreground text-sm">{plan.name}</h3>
                          {plan.description && (
                            <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">{plan.description}</p>
                          )}
                        </div>
                        <div className={cn("w-9 h-9 rounded-xl flex items-center justify-center shrink-0", color.light)}>
                          <BookOpen className={cn("w-4.5 h-4.5", color.text)} />
                        </div>
                      </div>
                      <div className="flex items-center gap-2 mt-3">
                        <Badge variant="secondary" className="text-[10px] px-2 py-0 h-5 font-medium">
                          {plan.problems.length} problem{plan.problems.length !== 1 ? "s" : ""}
                        </Badge>
                        {plan.revisionIntervals && (
                          <Badge variant="outline" className="text-[10px] px-2 py-0 h-5">
                            {plan.revisionIntervals.length} revisions
                          </Badge>
                        )}
                      </div>
                      {/* Difficulty pills */}
                      {plan.problems.length > 0 && (
                        <div className="flex items-center gap-1.5 mt-2">
                          {easyCount > 0 && (
                            <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-[#22C55E]/10 text-[#22C55E] font-medium">{easyCount} Easy</span>
                          )}
                          {mediumCount > 0 && (
                            <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-[#F59E0B]/10 text-[#F59E0B] font-medium">{mediumCount} Medium</span>
                          )}
                          {hardCount > 0 && (
                            <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-[#EF4444]/10 text-[#EF4444] font-medium">{hardCount} Hard</span>
                          )}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
