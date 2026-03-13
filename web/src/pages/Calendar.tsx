import { useState, useEffect } from "react";
import { getCalendarRevisions, type CalendarEvent } from "../lib/api";
import { toast } from "sonner";
import { ChevronLeft, ChevronRight, ExternalLink, X, Clock, TrendingUp } from "lucide-react";
import { cn } from "../lib/utils";

const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function getDifficultyDotColor(difficulty: string) {
  const d = difficulty?.toUpperCase();
  if (d === "EASY") return "bg-[#22C55E]";
  if (d === "MEDIUM") return "bg-[#F59E0B]";
  if (d === "HARD") return "bg-[#EF4444]";
  return "bg-muted-foreground";
}

function getDifficultyBadge(difficulty: string) {
  const d = difficulty?.toUpperCase();
  if (d === "EASY") return "bg-[#22C55E]/10 text-[#22C55E] border-[#22C55E]/20";
  if (d === "MEDIUM") return "bg-[#F59E0B]/10 text-[#F59E0B] border-[#F59E0B]/20";
  if (d === "HARD") return "bg-[#EF4444]/10 text-[#EF4444] border-[#EF4444]/20";
  return "bg-muted text-muted-foreground border-border";
}

function getPlatformBadge(platform: string) {
  switch (platform) {
    case "LeetCode": return "bg-[#E2703A]/10 text-[#E2703A] border-[#E2703A]/20";
    case "Codeforces": return "bg-[#1F8ACB]/10 text-[#1F8ACB] border-[#1F8ACB]/20";
    case "GFG": return "bg-[#2F8D46]/10 text-[#2F8D46] border-[#2F8D46]/20";
    case "HackerRank": return "bg-[#1BA94C]/10 text-[#1BA94C] border-[#1BA94C]/20";
    case "CodeChef": return "bg-[#5B4638]/10 text-[#5B4638] border-[#5B4638]/20";
    default: return "bg-muted text-muted-foreground border-border";
  }
}

// StatusBadge removed

export default function Calendar() {
  const today = new Date();
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [year, setYear] = useState(today.getFullYear());
  const [events, setEvents] = useState<Record<string, CalendarEvent[]>>({});
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const [tab, setTab] = useState<"problems" | "revisions">("problems");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getCalendarRevisions(month, year)
      .then((d) => setEvents(d.events || {}))
      .catch(() => toast.error("Failed to load calendar"))
      .finally(() => setLoading(false));
  }, [month, year]);

  const prevMonth = () => {
    if (month === 1) { setMonth(12); setYear(year - 1); }
    else setMonth(month - 1);
  };
  const nextMonth = () => {
    if (month === 12) { setMonth(1); setYear(year + 1); }
    else setMonth(month + 1);
  };

  const firstDay = new Date(year, month - 1, 1).getDay();
  const daysInMonth = new Date(year, month, 0).getDate();
  const cells: (number | null)[] = [];
  for (let i = 0; i < firstDay; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  const dayKey = (d: number) => `${year}-${String(month).padStart(2, "0")}-${String(d).padStart(2, "0")}`;

  const selectedEvents = selectedDay ? events[selectedDay] || [] : [];
  const problems = selectedEvents.filter((e) => e.type === "problem");
  const revisions = selectedEvents.filter((e) => e.type === "revision");

  const handleDayClick = (key: string) => {
    setSelectedDay(key);
    setTab("problems");
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Calendar</h1>
          <p className="text-muted-foreground">Your revision schedule</p>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={prevMonth} className="p-2 rounded-lg hover:bg-muted transition-colors">
            <ChevronLeft className="w-5 h-5" />
          </button>
          <span className="text-lg font-semibold min-w-[180px] text-center">{MONTHS[month - 1]} {year}</span>
          <button onClick={nextMonth} className="p-2 rounded-lg hover:bg-muted transition-colors">
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="flex gap-6">
          {/* Calendar grid */}
          <div className="flex-1 bg-card border border-border rounded-xl overflow-hidden">
            {/* Day headers */}
            <div className="grid grid-cols-7 border-b border-border">
              {DAYS.map((d) => (
                <div key={d} className="py-3 text-center text-xs font-medium text-muted-foreground">
                  {d}
                </div>
              ))}
            </div>
            {/* Day cells */}
            <div className="grid grid-cols-7">
              {cells.map((day, i) => {
                if (day === null) return (
                  <div key={i} className="h-20 border-b border-r border-border bg-muted/10 last:border-r-0" />
                );
                const key = dayKey(day);
                const dayEvents = events[key] || [];
                const isToday = key === todayStr;
                const isSelected = key === selectedDay;
                const hasEvents = dayEvents.length > 0;
                return (
                  <div
                    key={i}
                    onClick={() => handleDayClick(key)}
                    className={cn(
                      "h-20 border-b border-r border-border p-1.5 transition-colors last:border-r-0",
                      hasEvents ? "cursor-pointer hover:bg-accent/50" : "cursor-default",
                      isSelected && "bg-primary/5 ring-1 ring-inset ring-primary/30",
                    )}
                  >
                    <span className={cn(
                      "inline-flex items-center justify-center w-7 h-7 text-sm rounded-full font-medium",
                      isToday
                        ? "bg-primary text-primary-foreground"
                        : isSelected
                          ? "text-primary"
                          : "text-foreground"
                    )}>
                      {day}
                    </span>
                    {hasEvents && (
                      <div className="flex gap-[3px] mt-1 flex-wrap px-0.5">
                        {dayEvents.slice(0, 4).map((e, j) => (
                          <div key={j} className={cn("w-1.5 h-1.5 rounded-full", getDifficultyDotColor(e.difficulty))} />
                        ))}
                        {dayEvents.length > 4 && (
                          <span className="text-[9px] text-muted-foreground leading-none self-center">+{dayEvents.length - 4}</span>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Side panel */}
          <div className="w-80 shrink-0">
            <div className="bg-card border border-border rounded-xl overflow-hidden sticky top-8">
              {/* Header */}
              <div className="p-4 border-b border-border flex items-center justify-between">
                <h3 className="font-semibold text-sm">
                  {selectedDay
                    ? new Date(selectedDay + "T00:00:00").toLocaleDateString("en-US", {
                        weekday: "long", month: "long", day: "numeric",
                      })
                    : "Select a day"}
                </h3>
                {selectedDay && (
                  <button onClick={() => setSelectedDay(null)} className="text-muted-foreground hover:text-foreground">
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>

              {selectedDay && (
                /* Tabs */
                <div className="flex rounded-lg bg-muted mx-3 my-2 p-1">
                  <button
                    onClick={() => setTab("problems")}
                    className={cn(
                      "flex-1 text-xs font-medium py-1.5 rounded-md transition-all flex items-center justify-center gap-1",
                      tab === "problems"
                        ? "bg-background text-foreground shadow-sm"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    <Clock className="w-3 h-3" /> Problems ({problems.length})
                  </button>
                  <button
                    onClick={() => setTab("revisions")}
                    className={cn(
                      "flex-1 text-xs font-medium py-1.5 rounded-md transition-all flex items-center justify-center gap-1",
                      tab === "revisions"
                        ? "bg-background text-foreground shadow-sm"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                  >
                    <TrendingUp className="w-3 h-3" /> Revisions ({revisions.length})
                  </button>
                </div>
              )}

              {/* Content */}
              <div className="p-3 max-h-[480px] overflow-auto space-y-2">
                {!selectedDay ? (
                  <p className="text-sm text-muted-foreground text-center py-8">Click on a day to see events</p>
                ) : (() => {
                  const list = tab === "problems" ? problems : revisions;
                  if (list.length === 0)
                    return <p className="text-sm text-muted-foreground text-center py-8">No {tab} on this day</p>;
                  return list.map((e, i) => (
                    <div
                      key={i}
                      className={cn(
                        "bg-muted/40 border border-border rounded-lg p-3 overflow-hidden"
                      )}
                    >
                      <div className="flex items-start justify-between gap-2 mb-1.5">
                        <p className="text-sm font-medium leading-snug flex-1">{e.name}</p>
                        <div className="flex flex-col items-end gap-1 shrink-0">
                          {tab === "revisions" && (
                            <span className="text-[10px] text-primary font-medium">
                              Rev {e.revision}/{e.totalRevisions}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5 flex-wrap mb-2">
                        <span className={cn(
                          "text-[10px] px-1.5 py-0.5 rounded border font-medium",
                          getPlatformBadge(e.platform)
                        )}>
                          {e.platform}
                        </span>
                        <span className={cn(
                          "text-[10px] px-1.5 py-0.5 rounded border font-medium",
                          getDifficultyBadge(e.difficulty)
                        )}>
                          {e.difficulty}
                        </span>
                      </div>
                      {e.url && (
                        <a
                          href={e.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex items-center justify-center gap-1 w-full text-xs bg-primary text-primary-foreground hover:bg-primary/90 transition-colors rounded-md py-1.5 font-medium"
                        >
                          <ExternalLink className="w-3 h-3" /> Open
                        </a>
                      )}
                    </div>
                  ));
                })()}
              </div>

              {/* Legend */}
              <div className="px-4 py-3 border-t border-border flex items-center justify-center gap-4">
                <div className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full bg-[#22C55E]" />
                  <span className="text-[11px] text-muted-foreground">Easy</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full bg-[#F59E0B]" />
                  <span className="text-[11px] text-muted-foreground">Medium</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full bg-[#EF4444]" />
                  <span className="text-[11px] text-muted-foreground">Hard</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

