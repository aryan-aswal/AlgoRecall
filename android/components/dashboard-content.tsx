"use client"

import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import { Bell, Flame, TrendingUp, CheckCircle2, Clock, Check, SkipForward, ExternalLink, Mail, Smartphone, BellRing, Loader2 } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog"
import { cn } from "@/lib/utils"
import { getDashboard, completeRevision, skipRevision, getNotifications, completeByStudyPlanProblem, skipByStudyPlanProblem } from "@/lib/api"
import { useAuth } from "@/lib/auth-context"
import { toast } from "sonner"

function getDifficultyColor(difficulty: string) {
  switch (difficulty) {
    case "Easy": return "bg-[#22C55E]/10 text-[#22C55E] border-[#22C55E]/20"
    case "Medium": return "bg-[#F59E0B]/10 text-[#F59E0B] border-[#F59E0B]/20"
    case "Hard": return "bg-[#EF4444]/10 text-[#EF4444] border-[#EF4444]/20"
    default: return "bg-muted text-muted-foreground"
  }
}

function getPlatformColor(platform: string) {
  switch (platform) {
    case "LeetCode": return "bg-[#E2703A]/10 text-[#E2703A]"
    case "Codeforces": return "bg-[#1F8ACB]/10 text-[#1F8ACB]"
    case "GFG": return "bg-[#2F8D46]/10 text-[#2F8D46]"
    case "HackerRank": return "bg-[#1BA94C]/10 text-[#1BA94C]"
    case "CodeChef": return "bg-[#5B4638]/10 text-[#5B4638]"
    default: return "bg-muted text-muted-foreground"
  }
}

interface DashboardData {
  problemsSolved: number
  revisionStreak: number
  completionRate: number
  todayProblems: Array<{
    id: number; revisionScheduleId: number | null; name: string; url: string | null
    platform: string; difficulty: string; tags: string[]; time: string; isNew: boolean; solved: boolean; skipped: boolean
    studyPlanProblemId: number | null
  }>
  revisionTasks: Array<{
    id: number; revisionScheduleId: number; name: string; url: string | null
    platform: string; difficulty: string; tags: string[]; revision: string
    time: string; overdue: boolean; solved?: boolean; skipped?: boolean
  }>
}

interface NotificationItem {
  id: number
  message: string
  type: string
  sent: boolean
  scheduledTime: string
  createdAt: string
}

function getNotificationIcon(type: string) {
  switch (type) {
    case "EMAIL": return <Mail className="w-4 h-4 text-[#1F8ACB]" />
    case "SMS": return <Smartphone className="w-4 h-4 text-[#22C55E]" />
    case "PUSH": return <BellRing className="w-4 h-4 text-[#E2703A]" />
    default: return <Bell className="w-4 h-4 text-muted-foreground" />
  }
}

function formatTimeAgo(dateStr: string) {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return "Just now"
  if (diffMins < 60) return `${diffMins}m ago`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours}h ago`
  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays}d ago`
}

export function DashboardContent() {
  const { user } = useAuth()
  const router = useRouter()
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"problems" | "revisions">("problems")
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState<{ type: "complete" | "skip"; name: string; revisionScheduleId: number | null; studyPlanProblemId: number | null } | null>(null)
  const [actionLoading, setActionLoading] = useState(false)
  // Keep stable dialog content during exit animation
  const lastActionRef = useRef(confirmAction)
  if (confirmAction) lastActionRef.current = confirmAction
  const dialogContent = lastActionRef.current
  // Track latest fetchData call to ignore stale responses
  const fetchIdRef = useRef(0)

  const fetchData = () => {
    const id = ++fetchIdRef.current
    getDashboard()
      .then((d) => { if (fetchIdRef.current === id) setData(d) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  const fetchNotifications = () => {
    getNotifications()
      .then((all) => setNotifications(all.filter((n) => n.type === "PUSH")))
      .catch(() => {})
  }

  useEffect(() => { fetchData() }, [])

  useEffect(() => {
    if (notificationsOpen) fetchNotifications()
  }, [notificationsOpen])

  const handleConfirm = async () => {
    if (!confirmAction) return
    const { type, revisionScheduleId, studyPlanProblemId } = confirmAction
    setActionLoading(true)
    try {
      if (type === "complete") {
        if (revisionScheduleId) await completeRevision(revisionScheduleId)
        else if (studyPlanProblemId) await completeByStudyPlanProblem(studyPlanProblemId)
      } else {
        if (revisionScheduleId) await skipRevision(revisionScheduleId)
        else if (studyPlanProblemId) await skipByStudyPlanProblem(studyPlanProblemId)
      }
      // Optimistic local update so UI reflects change immediately
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          todayProblems: prev.todayProblems.map((p) =>
            (revisionScheduleId && p.revisionScheduleId === revisionScheduleId) ||
            (studyPlanProblemId && p.studyPlanProblemId === studyPlanProblemId)
              ? { ...p, solved: type === "complete", skipped: type === "skip", isNew: false }
              : p
          ),
          revisionTasks: prev.revisionTasks.map((t) =>
            revisionScheduleId && t.revisionScheduleId === revisionScheduleId
              ? { ...t, solved: type === "complete", skipped: type === "skip" }
              : t
          ),
        }
      })
    } catch {
      toast.error("Failed to update problem")
    } finally {
      setActionLoading(false)
      setConfirmAction(null)
      fetchData()
    }
  }

  const statsData = [
    { label: "Problems Solved", value: data ? String(data.problemsSolved) : "–", subtext: "total", icon: CheckCircle2, color: "text-primary" },
    { label: "Revision Streak", value: data ? String(data.revisionStreak) : "–", subtext: "days", icon: Flame, color: "text-[#F59E0B]" },
    { label: "Completion Rate", value: data ? `${data.completionRate}%` : "–", subtext: "revisions", icon: TrendingUp, color: "text-[#22C55E]" },
  ]

  if (loading) {
    return (
      <div className="px-4 py-4 flex items-center justify-center h-full">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    )
  }

  return (
    <div className="px-4 py-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-semibold text-foreground">
            {user ? `Hey, ${user.username}!` : "Good Morning!"}
          </h1>
          <p className="text-sm text-muted-foreground">Let&apos;s crush some problems today</p>
        </div>
        <div className="flex items-center gap-3">
          <Sheet open={notificationsOpen} onOpenChange={setNotificationsOpen}>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="relative">
                <Bell className="w-5 h-5 text-muted-foreground" />
                <span className="absolute top-1 right-1 w-2 h-2 bg-primary rounded-full" />
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-[320px] p-0">
              <SheetHeader className="px-4 pt-4 pb-3 border-b">
                <SheetTitle className="text-base">Notifications</SheetTitle>
              </SheetHeader>
              <div className="overflow-y-auto max-h-[calc(100vh-80px)]">
                {notifications.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-12 px-4">
                    <Bell className="w-10 h-10 text-muted-foreground/40 mb-3" />
                    <p className="text-sm text-muted-foreground">No notifications yet</p>
                  </div>
                ) : (
                  <div className="flex flex-col">
                    {notifications.map((n) => (
                      <div key={n.id} className="flex items-start gap-3 px-4 py-3 border-b last:border-b-0 hover:bg-muted/50 transition-colors">
                        <div className="mt-0.5 shrink-0">
                          <BellRing className="w-4 h-4 text-primary" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-foreground leading-snug">{n.message}</p>
                          <span className="text-[10px] text-muted-foreground">{formatTimeAgo(n.createdAt)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </SheetContent>
          </Sheet>
          <button onClick={() => router.push("/settings")} className="cursor-pointer">
            <Avatar className="w-9 h-9 border-2 border-primary">
              <AvatarFallback className="bg-primary text-primary-foreground text-sm font-medium">
                {user ? user.username.substring(0, 2).toUpperCase() : "U"}
              </AvatarFallback>
            </Avatar>
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        {statsData.map((stat) => (
          <Card key={stat.label} className="bg-card border-border shadow-sm">
            <CardContent className="p-3 text-center">
              <stat.icon className={cn("w-5 h-5 mx-auto mb-1.5", stat.color)} />
              <p className="text-lg font-bold text-foreground">{stat.value}</p>
              <p className="text-[10px] text-muted-foreground leading-tight">{stat.subtext}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Toggle Tabs */}
      <div className="flex rounded-lg bg-muted p-1 mb-4">
        <button
          className={cn(
            "flex-1 text-sm font-medium py-2 rounded-md transition-all",
            activeTab === "problems"
              ? "bg-background text-foreground shadow-sm"
              : "text-muted-foreground hover:text-foreground"
          )}
          onClick={() => setActiveTab("problems")}
        >
          <Clock className="w-3.5 h-3.5 inline mr-1.5 -mt-0.5" />
          Today&apos;s Problems
        </button>
        <button
          className={cn(
            "flex-1 text-sm font-medium py-2 rounded-md transition-all",
            activeTab === "revisions"
              ? "bg-background text-foreground shadow-sm"
              : "text-muted-foreground hover:text-foreground"
          )}
          onClick={() => setActiveTab("revisions")}
        >
          <TrendingUp className="w-3.5 h-3.5 inline mr-1.5 -mt-0.5" />
          Revisions
        </button>
      </div>

      {/* Today's Problems */}
      {activeTab === "problems" && (
        <div className="flex flex-col gap-3 mb-6">
          {(data?.todayProblems || []).length === 0 && (
            <p className="text-sm text-muted-foreground text-center py-4">No new problems today</p>
          )}
          {(data?.todayProblems || []).map((problem) => (
            <Card key={problem.studyPlanProblemId ?? problem.id} className={cn(
              "bg-card border-border shadow-sm overflow-hidden",
              problem.solved && "border-l-4 border-l-[#22C55E]",
              problem.skipped && "border-l-4 border-l-[#F59E0B]"
            )}>
              <CardContent className="p-3">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-foreground text-sm truncate">{problem.name}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4", getPlatformColor(problem.platform))}>{problem.platform}</Badge>
                      <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4 border", getDifficultyColor(problem.difficulty))}>{problem.difficulty}</Badge>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-xs text-muted-foreground">{problem.time}</p>
                    {problem.solved ? (
                      <Badge className="text-[9px] px-1.5 py-0 h-4 mt-1 bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/20 hover:bg-[#22C55E]/15">
                        <CheckCircle2 className="w-2.5 h-2.5 mr-0.5" />Solved
                      </Badge>
                    ) : problem.skipped ? (
                      <Badge className="text-[9px] px-1.5 py-0 h-4 mt-1 bg-[#F59E0B]/15 text-[#F59E0B] border border-[#F59E0B]/20 hover:bg-[#F59E0B]/15">
                        <SkipForward className="w-2.5 h-2.5 mr-0.5" />Skipped
                      </Badge>
                    ) : (
                      <Badge variant="secondary" className="text-[9px] px-1.5 py-0 h-4 mt-1">New</Badge>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1.5 mb-3">
                  {(problem.tags || []).slice(0, 3).map((tag) => (
                    <Badge key={tag} variant="secondary" className="text-[10px] px-1.5 py-0 h-4 bg-accent text-accent-foreground">{tag}</Badge>
                  ))}
                </div>
                <div className="flex items-center gap-2">
                  <Button size="sm" className="h-7 text-xs flex-1 bg-primary hover:bg-primary/90 text-primary-foreground"
                    disabled={!problem.url}
                    onClick={() => problem.url && window.open(problem.url, "_blank")}>
                    <ExternalLink className="w-3 h-3 mr-1" />Open
                  </Button>
                  {!problem.solved && !problem.skipped && (
                    <>
                      <Button size="sm" variant="outline" className="h-7 w-7 p-0" title="Complete"
                        onClick={() => setConfirmAction({ type: "complete", name: problem.name, revisionScheduleId: problem.revisionScheduleId, studyPlanProblemId: problem.studyPlanProblemId })}>
                        <Check className="w-3.5 h-3.5" />
                      </Button>
                      <Button size="sm" variant="outline" className="h-7 w-7 p-0" title="Skip"
                        onClick={() => setConfirmAction({ type: "skip", name: problem.name, revisionScheduleId: problem.revisionScheduleId, studyPlanProblemId: problem.studyPlanProblemId })}>
                        <SkipForward className="w-3.5 h-3.5" />
                      </Button>
                    </>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Revision Tasks */}
      {activeTab === "revisions" && (
        <div className="flex flex-col gap-3 mb-6">
          {(data?.revisionTasks || []).length === 0 && (
            <p className="text-sm text-muted-foreground text-center py-4">No revision tasks today</p>
          )}
          {(data?.revisionTasks || []).map((task) => (
            <Card key={task.revisionScheduleId} className={cn(
              "bg-card border-border shadow-sm overflow-hidden transition-all duration-200",
              task.overdue && !task.solved && !task.skipped && "border-l-4 border-l-[#EF4444]",
              task.solved && "border-l-4 border-l-[#22C55E]",
              task.skipped && "border-l-4 border-l-[#F59E0B]"
            )}>
              <CardContent className="p-3">
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-foreground text-sm truncate">{task.name}</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4", getPlatformColor(task.platform))}>{task.platform}</Badge>
                      <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4 border", getDifficultyColor(task.difficulty))}>{task.difficulty}</Badge>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-xs text-muted-foreground">{task.time}</p>
                    <p className="text-[10px] text-primary font-medium mt-0.5">Rev {task.revision}</p>
                    {task.solved ? (
                      <Badge className="text-[9px] px-1.5 py-0 h-4 mt-1 bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/20 hover:bg-[#22C55E]/15">
                        <CheckCircle2 className="w-2.5 h-2.5 mr-0.5" />Done
                      </Badge>
                    ) : task.skipped ? (
                      <Badge className="text-[9px] px-1.5 py-0 h-4 mt-1 bg-[#F59E0B]/15 text-[#F59E0B] border border-[#F59E0B]/20 hover:bg-[#F59E0B]/15">
                        <SkipForward className="w-2.5 h-2.5 mr-0.5" />Skipped
                      </Badge>
                    ) : task.overdue ? (
                      <Badge variant="destructive" className="text-[9px] px-1.5 py-0 h-4 mt-1">Overdue</Badge>
                    ) : null}
                  </div>
                </div>
                <div className="flex items-center gap-1.5 mb-3">
                  {(task.tags || []).slice(0, 3).map((tag) => (
                    <Badge key={tag} variant="secondary" className="text-[10px] px-1.5 py-0 h-4 bg-accent text-accent-foreground">{tag}</Badge>
                  ))}
                </div>
                <div className="flex items-center gap-2">
                  <Button size="sm" className="h-7 text-xs flex-1 bg-primary hover:bg-primary/90 text-primary-foreground"
                    disabled={!task.url}
                    onClick={() => task.url && window.open(task.url, "_blank")}>
                    <ExternalLink className="w-3 h-3 mr-1" />Open
                  </Button>
                  {!task.solved && !task.skipped && (
                    <>
                      <Button size="sm" variant="outline" className="h-7 w-7 p-0" title="Complete"
                        onClick={() => setConfirmAction({ type: "complete", name: task.name, revisionScheduleId: task.revisionScheduleId, studyPlanProblemId: null })}>
                        <Check className="w-3.5 h-3.5" />
                      </Button>
                      <Button size="sm" variant="outline" className="h-7 w-7 p-0" title="Skip"
                        onClick={() => setConfirmAction({ type: "skip", name: task.name, revisionScheduleId: task.revisionScheduleId, studyPlanProblemId: null })}>
                        <SkipForward className="w-3.5 h-3.5" />
                      </Button>
                    </>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
      {/* Confirmation Dialog */}
      <AlertDialog open={!!confirmAction} onOpenChange={(open) => { if (!open && !actionLoading) setConfirmAction(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {dialogContent?.type === "complete" ? "Mark as Completed?" : "Skip this Problem?"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {dialogContent?.type === "complete"
                ? `Are you sure you want to mark "${dialogContent?.name}" as completed? This action is irreversible.`
                : `Are you sure you want to skip "${dialogContent?.name}"? This action is irreversible and cannot be undone.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={(e) => { e.preventDefault(); handleConfirm(); }} disabled={actionLoading}>
              {actionLoading ? <Loader2 className="w-4 h-4 animate-spin mr-1" /> : null}
              {dialogContent?.type === "complete" ? "Yes, Complete" : "Yes, Skip"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
