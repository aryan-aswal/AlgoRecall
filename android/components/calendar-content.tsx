"use client"

import { useState, useEffect } from "react"
import { ChevronLeft, ChevronRight, ExternalLink, Clock, TrendingUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { cn } from "@/lib/utils"
import { getCalendarRevisions } from "@/lib/api"

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]

interface CalendarEvent {
  id: number; name: string; platform: string
  difficulty: string; time: string; revision: number
  totalRevisions: number; url?: string; status?: string; type?: string
}

interface EventsData { [key: string]: CalendarEvent[] }

function getDifficultyColor(difficulty: string) {
  switch (difficulty) {
    case "Easy": return "bg-[#22C55E]"
    case "Medium": return "bg-[#F59E0B]"
    case "Hard": return "bg-[#EF4444]"
    default: return "bg-muted"
  }
}

function getDifficultyBadgeColor(difficulty: string) {
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

export function CalendarContent() {
  const now = new Date()
  const [currentDate, setCurrentDate] = useState(new Date(now.getFullYear(), now.getMonth(), 1))
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [sheetOpen, setSheetOpen] = useState(false)
  const [eventsData, setEventsData] = useState<EventsData>({})
  const [loading, setLoading] = useState(true)
  const [sheetTab, setSheetTab] = useState<"problems" | "revisions">("problems")

  const year = currentDate.getFullYear()
  const month = currentDate.getMonth()

  useEffect(() => {
    setLoading(true)
    getCalendarRevisions(month + 1, year)
      .then((data) => setEventsData((data.events || {}) as EventsData))
      .catch(() => setEventsData({}))
      .finally(() => setLoading(false))
  }, [month, year])

  const firstDayOfMonth = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  const prevMonth = () => setCurrentDate(new Date(year, month - 1, 1))
  const nextMonth = () => setCurrentDate(new Date(year, month + 1, 1))

  const formatDateKey = (day: number) =>
    `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`

  const handleDayClick = (day: number) => {
    const dateKey = formatDateKey(day)
    const dayEvents = eventsData[dateKey]
    if (dayEvents && dayEvents.length > 0) {
      setSelectedDate(dateKey)
      setSheetTab("problems")
      setSheetOpen(true)
    }
  }

  const today = new Date()
  const isToday = (day: number) =>
    today.getDate() === day && today.getMonth() === month && today.getFullYear() === year

  const calendarDays = []
  for (let i = 0; i < firstDayOfMonth; i++) {
    calendarDays.push(<div key={`empty-${i}`} className="h-12" />)
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const dateKey = formatDateKey(day)
    const dayEvents = eventsData[dateKey] || []
    const hasEvents = dayEvents.length > 0
    calendarDays.push(
      <button key={day} onClick={() => handleDayClick(day)}
        className={cn("h-12 flex flex-col items-center justify-start pt-1 rounded-lg transition-all relative",
          isToday(day) && "bg-primary/10 ring-2 ring-primary",
          hasEvents && "cursor-pointer hover:bg-accent",
          !hasEvents && "cursor-default")}>
        <span className={cn("text-sm font-medium", isToday(day) ? "text-primary" : "text-foreground")}>{day}</span>
        {hasEvents && (
          <div className="flex gap-[2px] mt-0.5">
            {dayEvents.slice(0, 3).map((event, idx) => (
              <div key={idx} className={cn("w-1.5 h-1.5 rounded-full", getDifficultyColor(event.difficulty))} />
            ))}
          </div>
        )}
      </button>
    )
  }

  const selectedEvents = selectedDate ? eventsData[selectedDate] || [] : []
  const problems = selectedEvents.filter((e) => e.type === "problem")
  const revisions = selectedEvents.filter((e) => e.type === "revision")

  return (
    <div className="px-4 py-4">
      <div className="mb-4">
        <h1 className="text-xl font-semibold text-foreground">Calendar</h1>
        <p className="text-sm text-muted-foreground">View your revision schedule</p>
      </div>

      <Card className="bg-card border-border shadow-sm mb-4">
        <CardHeader className="pb-2 px-3 pt-3">
          <div className="flex items-center justify-between">
            <Button variant="ghost" size="icon" onClick={prevMonth} className="h-8 w-8">
              <ChevronLeft className="w-4 h-4" />
            </Button>
            <CardTitle className="text-base font-semibold text-foreground">{MONTHS[month]} {year}</CardTitle>
            <Button variant="ghost" size="icon" onClick={nextMonth} className="h-8 w-8">
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>
        </CardHeader>
        <CardContent className="px-2 pb-3">
          <div className="grid grid-cols-7 mb-2">
            {DAYS.map((day) => (
              <div key={day} className="text-center text-xs font-medium text-muted-foreground py-2">{day}</div>
            ))}
          </div>
          {loading ? (
            <div className="text-center py-8"><p className="text-sm text-muted-foreground">Loading...</p></div>
          ) : (
            <div className="grid grid-cols-7 gap-1">{calendarDays}</div>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center justify-center gap-4">
        <div className="flex items-center gap-1.5"><div className="w-2.5 h-2.5 rounded-full bg-[#22C55E]" /><span className="text-xs text-muted-foreground">Easy</span></div>
        <div className="flex items-center gap-1.5"><div className="w-2.5 h-2.5 rounded-full bg-[#F59E0B]" /><span className="text-xs text-muted-foreground">Medium</span></div>
        <div className="flex items-center gap-1.5"><div className="w-2.5 h-2.5 rounded-full bg-[#EF4444]" /><span className="text-xs text-muted-foreground">Hard</span></div>
      </div>

      {/* Day Detail Sheet */}
      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="bottom" className="rounded-t-2xl max-h-[70vh] flex flex-col p-0 max-w-[390px] mx-auto">
          <SheetHeader className="px-4 pt-4 pb-2 shrink-0">
            <SheetTitle className="text-base">
              {selectedDate && new Date(selectedDate + "T00:00:00").toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })}
            </SheetTitle>
          </SheetHeader>

          {/* Tabs */}
          <div className="flex rounded-lg bg-muted mx-4 p-1 mb-3 shrink-0">
            <button
              className={cn(
                "flex-1 text-xs font-medium py-1.5 rounded-md transition-all",
                sheetTab === "problems"
                  ? "bg-background text-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
              onClick={() => setSheetTab("problems")}
            >
              <Clock className="w-3 h-3 inline mr-1 -mt-0.5" />
              Problems ({problems.length})
            </button>
            <button
              className={cn(
                "flex-1 text-xs font-medium py-1.5 rounded-md transition-all",
                sheetTab === "revisions"
                  ? "bg-background text-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground"
              )}
              onClick={() => setSheetTab("revisions")}
            >
              <TrendingUp className="w-3 h-3 inline mr-1 -mt-0.5" />
              Revisions ({revisions.length})
            </button>
          </div>

          {/* Scrollable content */}
          <div className="flex-1 overflow-y-auto px-4 pb-6">
            {sheetTab === "problems" && (
              <div className="flex flex-col gap-2">
                {problems.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">No problems for this day</p>
                ) : (
                  problems.map((event, idx) => (
                    <Card key={`p-${event.id}-${idx}`} className={cn(
                      "bg-card border-border shadow-sm overflow-hidden",
                      event.status === "COMPLETED" && "border-l-4 border-l-[#22C55E]",
                      event.status === "SKIPPED" && "border-l-4 border-l-[#F59E0B]"
                    )}>
                      <CardContent className="p-3">
                        <div className="flex items-start justify-between gap-2 mb-1.5">
                          <h3 className="font-medium text-foreground text-sm truncate flex-1">{event.name}</h3>
                          {event.status === "COMPLETED" ? (
                            <Badge className="text-[9px] px-1.5 py-0 h-4 bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/20 hover:bg-[#22C55E]/15 shrink-0">Solved</Badge>
                          ) : event.status === "SKIPPED" ? (
                            <Badge className="text-[9px] px-1.5 py-0 h-4 bg-[#F59E0B]/15 text-[#F59E0B] border border-[#F59E0B]/20 hover:bg-[#F59E0B]/15 shrink-0">Skipped</Badge>
                          ) : (
                            <Badge variant="secondary" className="text-[9px] px-1.5 py-0 h-4 shrink-0">Pending</Badge>
                          )}
                        </div>
                        <div className="flex items-center gap-2 mb-2">
                          <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4", getPlatformColor(event.platform))}>{event.platform}</Badge>
                          <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4 border", getDifficultyBadgeColor(event.difficulty))}>{event.difficulty}</Badge>
                        </div>
                        {event.url && (
                          <Button size="sm" className="h-7 text-xs w-full bg-primary hover:bg-primary/90 text-primary-foreground"
                            onClick={() => window.open(event.url, "_blank")}>
                            <ExternalLink className="w-3 h-3 mr-1" />Open
                          </Button>
                        )}
                      </CardContent>
                    </Card>
                  ))
                )}
              </div>
            )}

            {sheetTab === "revisions" && (
              <div className="flex flex-col gap-2">
                {revisions.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">No revisions for this day</p>
                ) : (
                  revisions.map((event, idx) => (
                    <Card key={`r-${event.id}-${idx}`} className={cn(
                      "bg-card border-border shadow-sm overflow-hidden",
                      event.status === "COMPLETED" && "border-l-4 border-l-[#22C55E]",
                      event.status === "SKIPPED" && "border-l-4 border-l-[#F59E0B]"
                    )}>
                      <CardContent className="p-3">
                        <div className="flex items-start justify-between gap-2 mb-1.5">
                          <h3 className="font-medium text-foreground text-sm truncate flex-1">{event.name}</h3>
                          <div className="text-right shrink-0">
                            <p className="text-[10px] text-primary font-medium">Rev {event.revision} of {event.totalRevisions}</p>
                            {event.status === "COMPLETED" ? (
                              <Badge className="text-[9px] px-1.5 py-0 h-4 mt-0.5 bg-[#22C55E]/15 text-[#22C55E] border border-[#22C55E]/20 hover:bg-[#22C55E]/15">Done</Badge>
                            ) : event.status === "SKIPPED" ? (
                              <Badge className="text-[9px] px-1.5 py-0 h-4 mt-0.5 bg-[#F59E0B]/15 text-[#F59E0B] border border-[#F59E0B]/20 hover:bg-[#F59E0B]/15">Skipped</Badge>
                            ) : (
                              <Badge variant="secondary" className="text-[9px] px-1.5 py-0 h-4 mt-0.5">Pending</Badge>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center gap-2 mb-2">
                          <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4", getPlatformColor(event.platform))}>{event.platform}</Badge>
                          <Badge variant="outline" className={cn("text-[10px] px-1.5 py-0 h-4 border", getDifficultyBadgeColor(event.difficulty))}>{event.difficulty}</Badge>
                        </div>
                        {event.url && (
                          <Button size="sm" className="h-7 text-xs w-full bg-primary hover:bg-primary/90 text-primary-foreground"
                            onClick={() => window.open(event.url, "_blank")}>
                            <ExternalLink className="w-3 h-3 mr-1" />Open
                          </Button>
                        )}
                      </CardContent>
                    </Card>
                  ))
                )}
              </div>
            )}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
