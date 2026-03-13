"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import {
  Plus, X, Eye, Clock, Trash2, BookOpen, Search,
  ChevronLeft, ChevronRight, Check, Filter
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Checkbox } from "@/components/ui/checkbox"
import { cn } from "@/lib/utils"
import { browseProblems, createStudyPlan, getTopics, type BrowseProblemsResponse } from "@/lib/api"

/* ───── constants ───── */
const intervalTypes = ["Days", "Weeks", "Months"]
const platforms = ["LEETCODE", "GFG"]
const difficulties = ["EASY", "MEDIUM", "HARD"]
const sortOptions = [
  { value: "number", label: "Problem #" },
  { value: "title", label: "Title A-Z" },
  { value: "difficulty", label: "Difficulty" },
  { value: "newest", label: "Newest" },
]

/* ───── types ───── */
interface RevisionInterval { value: number; type: string }

interface ProblemItem {
  id: number
  title: string
  problemNumber: number | null
  url: string
  platform: string
  difficulty: string
  topicTags: string
}

/* ───── helpers ───── */
const diffColor = (d: string | null) => {
  switch (d?.toUpperCase()) {
    case "EASY": return "text-green-600 bg-green-50 border-green-200"
    case "MEDIUM": return "text-yellow-600 bg-yellow-50 border-yellow-200"
    case "HARD": return "text-red-600 bg-red-50 border-red-200"
    default: return "text-muted-foreground bg-muted"
  }
}
const platLabel = (p: string | null) => {
  switch (p?.toUpperCase()) {
    case "LEETCODE": return "LeetCode"
    case "GFG": return "GFG"
    case "CODEFORCES": return "Codeforces"
    default: return p || ""
  }
}

const PAGE_SIZE = 20

/* ═══════════════════════ COMPONENT ═══════════════════════ */
export function AddProblemContent() {
  const [step, setStep] = useState(1) // 1 → details  2 → revision policy  3 → pick problems

  /* ── Step 1 state ── */
  const [planName, setPlanName] = useState("")
  const [planDescription, setPlanDescription] = useState("")

  /* ── Step 2 state ── */
  const [intervals, setIntervals] = useState<RevisionInterval[]>([
    { value: 1, type: "Days" }, { value: 3, type: "Days" },
    { value: 1, type: "Weeks" }, { value: 2, type: "Weeks" },
    { value: 1, type: "Months" },
  ])
  const [reminderTime, setReminderTime] = useState("09:00")

  /* ── Step 3 state ── */
  const [selectedProblems, setSelectedProblems] = useState<ProblemItem[]>([])
  // filters
  const [searchQuery, setSearchQuery] = useState("")
  const [filterPlatform, setFilterPlatform] = useState("")
  const [filterDifficulty, setFilterDifficulty] = useState("")
  const [filterTopic, setFilterTopic] = useState("")
  const [sortBy, setSortBy] = useState("number")
  // browse results
  const [browseData, setBrowseData] = useState<BrowseProblemsResponse | null>(null)
  const [browsePage, setBrowsePage] = useState(0)
  const [browseLoading, setBrowseLoading] = useState(false)
  const [filterOpen, setFilterOpen] = useState(false)
  const [availableTopics, setAvailableTopics] = useState<string[]>([])
  const debounceRef = useRef<ReturnType<typeof setTimeout>>()

  /* ── Submission state ── */
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState("")

  /* ── Fetch available topics on mount ── */
  useEffect(() => {
    getTopics().then(setAvailableTopics).catch(() => {})
  }, [])

  /* ── Fetch problems when filters/page/sort change ── */
  const fetchProblems = useCallback(async (page: number) => {
    setBrowseLoading(true)
    try {
      const data = await browseProblems({
        search: searchQuery.trim() || undefined,
        platform: filterPlatform || undefined,
        difficulty: filterDifficulty || undefined,
        topic: filterTopic.trim() || undefined,
        sortBy,
        page,
        size: PAGE_SIZE,
      })
      setBrowseData(data)
      setBrowsePage(page)
    } catch {
      setBrowseData(null)
    } finally {
      setBrowseLoading(false)
    }
  }, [searchQuery, filterPlatform, filterDifficulty, filterTopic, sortBy])

  // Auto-fetch when entering step 3 or when filters change
  useEffect(() => {
    if (step !== 3) return
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => fetchProblems(0), 300)
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current) }
  }, [step, fetchProblems])

  /* ── Toggle problem selection ── */
  const toggleProblem = (problem: ProblemItem) => {
    setSelectedProblems(prev =>
      prev.some(p => p.id === problem.id)
        ? prev.filter(p => p.id !== problem.id)
        : [...prev, problem]
    )
  }
  const isSelected = (id: number) => selectedProblems.some(p => p.id === id)

  /* ── Interval helpers ── */
  const addInterval = () => setIntervals([...intervals, { value: 1, type: "Days" }])
  const removeInterval = (i: number) => setIntervals(intervals.filter((_, idx) => idx !== i))
  const updateInterval = (i: number, field: keyof RevisionInterval, v: number | string) => {
    const copy = [...intervals]
    copy[i] = { ...copy[i], [field]: v }
    setIntervals(copy)
  }
  const toDays = (iv: RevisionInterval) => {
    if (iv.type === "Weeks") return iv.value * 7
    if (iv.type === "Months") return iv.value * 30
    return iv.value
  }
  const getSchedulePreview = () => {
    const today = new Date()
    return intervals.map((iv, idx) => {
      const d = new Date(today)
      if (iv.type === "Days") d.setDate(d.getDate() + iv.value)
      else if (iv.type === "Weeks") d.setDate(d.getDate() + iv.value * 7)
      else d.setMonth(d.getMonth() + iv.value)
      return { revision: idx + 1, date: d.toLocaleDateString("en-US", { month: "short", day: "numeric" }) }
    })
  }

  /* ── Submit ── */
  const handleSubmit = async () => {
    if (selectedProblems.length === 0) { setError("Add at least one problem"); return }
    setSubmitting(true); setError(""); setSuccess(false)
    try {
      await createStudyPlan({
        name: planName.trim(),
        description: planDescription.trim() || undefined,
        problemIds: selectedProblems.map(p => p.id),
        revisionIntervals: intervals.map(toDays),
        reminderTime,
      })
      setSuccess(true)
      setPlanName(""); setPlanDescription(""); setSelectedProblems([])
      setIntervals([
        { value: 1, type: "Days" }, { value: 3, type: "Days" },
        { value: 1, type: "Weeks" }, { value: 2, type: "Weeks" },
        { value: 1, type: "Months" },
      ])
      setReminderTime("09:00"); setStep(1)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create study plan")
    } finally {
      setSubmitting(false)
    }
  }

  /* ── Step indicator ── */
  const stepLabels = ["Details", "Revision", "Problems"]

  /* ═══════════════════════ RENDER ═══════════════════════ */
  return (
    <div className="px-4 py-4 flex flex-col w-full">
      {/* Header */}
      <div className="mb-4">
        <h1 className="text-xl font-semibold text-foreground">Create Study Plan</h1>
        <p className="text-sm text-muted-foreground">
          Step {step} of 3 — {stepLabels[step - 1]}
        </p>
      </div>

      {/* Step indicator */}
      <div className="flex items-center gap-2 mb-5 px-2">
        {stepLabels.map((label, idx) => {
          const s = idx + 1
          const done = s < step
          const active = s === step
          return (
            <div key={label} className="flex items-center gap-2 flex-1">
              <div className={cn(
                "w-7 h-7 rounded-full flex items-center justify-center text-xs font-medium shrink-0 transition-colors",
                done ? "bg-primary text-primary-foreground" :
                active ? "bg-primary text-primary-foreground" :
                "bg-muted text-muted-foreground"
              )}>
                {done ? <Check className="w-3.5 h-3.5" /> : s}
              </div>
              <span className={cn("text-xs hidden xs:inline", active ? "text-foreground font-medium" : "text-muted-foreground")}>{label}</span>
              {idx < 2 && <div className={cn("flex-1 h-0.5 rounded", done ? "bg-primary" : "bg-muted")} />}
            </div>
          )
        })}
      </div>

      {success && (
        <div className="mb-4 p-3 bg-[#22C55E]/10 border border-[#22C55E]/20 rounded-lg">
          <p className="text-sm text-[#22C55E] font-medium">Study plan created successfully!</p>
        </div>
      )}

      {/* ═══════ STEP 1: Plan Details ═══════ */}
      {step === 1 && (
        <Card className="bg-card border-border shadow-sm mb-4 flex-1">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold text-foreground flex items-center gap-2">
              <BookOpen className="w-4 h-4" />Plan Details
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="planName" className="text-sm text-foreground">Plan Name</Label>
              <Input id="planName" placeholder="e.g., Dynamic Programming Mastery" value={planName}
                onChange={e => setPlanName(e.target.value)} className="h-10 bg-background border-input" />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="planDesc" className="text-sm text-foreground">Description (optional)</Label>
              <Input id="planDesc" placeholder="e.g., Focus on DP patterns and optimization" value={planDescription}
                onChange={e => setPlanDescription(e.target.value)} className="h-10 bg-background border-input" />
            </div>
          </CardContent>
        </Card>
      )}

      {/* ═══════ STEP 2: Revision Policy ═══════ */}
      {step === 2 && (
        <Card className="bg-card border-border shadow-sm mb-4 flex-1">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold text-foreground">Revision Policy</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {/* Intervals */}
            <div className="flex flex-col gap-2">
              <Label className="text-sm text-foreground">Revision Intervals</Label>
              <p className="text-xs text-muted-foreground">Applied to every problem in this plan</p>
              <div className="flex flex-col gap-2">
                {intervals.map((iv, idx) => (
                  <div key={idx} className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground w-6">#{idx + 1}</span>
                    <Input type="number" min={1} value={iv.value}
                      onChange={e => updateInterval(idx, "value", Number(e.target.value))}
                      className="h-9 w-16 bg-background border-input text-sm" />
                    <Select value={iv.type} onValueChange={v => updateInterval(idx, "type", v)}>
                      <SelectTrigger className="h-9 w-24 bg-background border-input text-sm"><SelectValue /></SelectTrigger>
                      <SelectContent>{intervalTypes.map(t => <SelectItem key={t} value={t}>{t}</SelectItem>)}</SelectContent>
                    </Select>
                    {intervals.length > 1 && (
                      <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => removeInterval(idx)}>
                        <X className="w-4 h-4 text-muted-foreground" />
                      </Button>
                    )}
                  </div>
                ))}
                <Button variant="outline" size="sm" className="w-fit mt-1" onClick={addInterval}>
                  <Plus className="w-3.5 h-3.5 mr-1" />Add Interval
                </Button>
              </div>
            </div>

            {/* Reminder Time */}
            <div className="flex flex-col gap-1.5">
              <Label className="text-sm text-foreground flex items-center gap-1.5">
                <Clock className="w-4 h-4" />Reminder Time
              </Label>
              <p className="text-xs text-muted-foreground">When should revision reminders arrive?</p>
              <Input type="time" value={reminderTime} onChange={e => setReminderTime(e.target.value)}
                className="h-10 bg-background border-input w-32" />
            </div>

            {/* Preview */}
            <div className="flex flex-col gap-2 pt-2 border-t border-border">
              <Label className="text-sm text-foreground flex items-center gap-1.5">
                <Eye className="w-4 h-4" />Schedule Preview
              </Label>
              <p className="text-xs text-muted-foreground">
                Each problem will be revised on these dates at {reminderTime || "09:00"}
              </p>
              <div className="flex flex-wrap gap-2">
                {getSchedulePreview().map(p => (
                  <Badge key={p.revision} variant="secondary" className="text-xs bg-accent text-accent-foreground">
                    R{p.revision}: {p.date}
                  </Badge>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ═══════ STEP 3: Browse & Select Problems ═══════ */}
      {step === 3 && (
        <div className="flex flex-col gap-3">
          {/* Selected count */}
          {selectedProblems.length > 0 && (
            <div className="flex items-center justify-between px-1">
              <Badge variant="default" className="text-xs">
                {selectedProblems.length} selected
              </Badge>
              <Button variant="ghost" size="sm" className="text-xs h-7 text-muted-foreground"
                onClick={() => setSelectedProblems([])}>
                Clear all
              </Button>
            </div>
          )}

          {/* Search bar */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input placeholder="Search by name or number..." value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="h-10 pl-9 pr-10 bg-background border-input" />
            <Button variant="ghost" size="icon"
              className={cn("absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8", (filterPlatform || filterDifficulty || filterTopic) && "text-primary")}
              onClick={() => setFilterOpen(true)}>
              <Filter className="w-4 h-4" />
            </Button>
          </div>

          {/* Active filter badges */}
          {(filterPlatform || filterDifficulty || filterTopic) && (
            <div className="flex flex-wrap gap-1.5">
              {filterPlatform && (
                <Badge variant="secondary" className="text-xs flex items-center gap-1 pr-1">
                  {platLabel(filterPlatform)}
                  <button onClick={() => setFilterPlatform("")} className="hover:text-destructive"><X className="w-3 h-3" /></button>
                </Badge>
              )}
              {filterDifficulty && (
                <Badge variant="secondary" className="text-xs flex items-center gap-1 pr-1">
                  {filterDifficulty.charAt(0) + filterDifficulty.slice(1).toLowerCase()}
                  <button onClick={() => setFilterDifficulty("")} className="hover:text-destructive"><X className="w-3 h-3" /></button>
                </Badge>
              )}
              {filterTopic && (
                <Badge variant="secondary" className="text-xs flex items-center gap-1 pr-1">
                  {filterTopic}
                  <button onClick={() => setFilterTopic("")} className="hover:text-destructive"><X className="w-3 h-3" /></button>
                </Badge>
              )}
            </div>
          )}

          {/* Filter overlay (inline, stays inside phone frame) */}
          {filterOpen && (
            <div className="absolute inset-0 z-40 flex items-center justify-center bg-black/50 p-4"
              onClick={e => { if (e.target === e.currentTarget) setFilterOpen(false) }}>
              <div className="bg-background border border-border rounded-lg shadow-lg w-full max-w-xs p-4 flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-semibold text-foreground">Filters & Sort</h3>
                  <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setFilterOpen(false)}>
                    <X className="w-4 h-4" />
                  </Button>
                </div>
                <div className="flex flex-col gap-3">
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs text-muted-foreground">Platform</Label>
                    <Select value={filterPlatform || "ALL"} onValueChange={v => setFilterPlatform(v === "ALL" ? "" : v)}>
                      <SelectTrigger className="h-9 bg-background border-input text-sm"><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Platforms</SelectItem>
                        {platforms.map(p => <SelectItem key={p} value={p}>{platLabel(p)}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs text-muted-foreground">Difficulty</Label>
                    <Select value={filterDifficulty || "ALL"} onValueChange={v => setFilterDifficulty(v === "ALL" ? "" : v)}>
                      <SelectTrigger className="h-9 bg-background border-input text-sm"><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ALL">All Difficulties</SelectItem>
                        {difficulties.map(d => <SelectItem key={d} value={d}>{d.charAt(0) + d.slice(1).toLowerCase()}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs text-muted-foreground">Topic</Label>
                    <Select value={filterTopic || "ALL"} onValueChange={v => setFilterTopic(v === "ALL" ? "" : v)}>
                      <SelectTrigger className="h-9 bg-background border-input text-sm"><SelectValue /></SelectTrigger>
                      <SelectContent className="max-h-48">
                        <SelectItem value="ALL">All Topics</SelectItem>
                        {availableTopics.map(t => <SelectItem key={t} value={t}>{t}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label className="text-xs text-muted-foreground">Sort By</Label>
                    <Select value={sortBy} onValueChange={setSortBy}>
                      <SelectTrigger className="h-9 bg-background border-input text-sm"><SelectValue /></SelectTrigger>
                      <SelectContent>
                        {sortOptions.map(s => <SelectItem key={s.value} value={s.value}>{s.label}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="flex gap-2 justify-end pt-1">
                  <Button variant="outline" size="sm" onClick={() => {
                    setFilterPlatform(""); setFilterDifficulty(""); setFilterTopic(""); setSortBy("number")
                  }}>Reset</Button>
                  <Button size="sm" onClick={() => setFilterOpen(false)}>Apply</Button>
                </div>
              </div>
            </div>
          )}

          {/* Problem list */}
          <div className="overflow-y-auto max-h-85 -mx-4 px-4">
            {browseLoading && (
              <div className="flex items-center justify-center py-10">
                <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
              </div>
            )}

            {!browseLoading && browseData && browseData.content.length === 0 && (
              <div className="py-10 text-center">
                <p className="text-sm text-muted-foreground">No problems found. Try adjusting your filters.</p>
              </div>
            )}

            {!browseLoading && browseData && browseData.content.length > 0 && (
              <div className="flex flex-col gap-1.5">
                {browseData.content.map(problem => {
                  const checked = isSelected(problem.id)
                  return (
                    <div
                      key={problem.id}
                      role="button"
                      tabIndex={0}
                      className={cn(
                        "flex items-start gap-2.5 px-3 py-2.5 rounded-lg border text-left transition-colors cursor-pointer",
                        checked ? "bg-primary/5 border-primary/30" : "bg-card border-border hover:bg-accent/50"
                      )}
                      onClick={() => toggleProblem(problem)}
                      onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggleProblem(problem) } }}
                    >
                      <Checkbox checked={checked} className="mt-0.5 shrink-0" tabIndex={-1} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="text-xs font-medium text-muted-foreground">
                            {platLabel(problem.platform)}
                            {problem.problemNumber ? ` #${problem.problemNumber}` : ""}
                          </span>
                          {problem.difficulty && (
                            <Badge variant="outline" className={cn("text-[10px] px-1 py-0 h-4", diffColor(problem.difficulty))}>
                              {problem.difficulty}
                            </Badge>
                          )}
                        </div>
                        <p className="text-sm font-medium text-foreground truncate">{problem.title}</p>
                        {problem.topicTags && (
                          <div className="flex flex-wrap gap-1 mt-0.5">
                            {problem.topicTags.split(",").slice(0, 3).map(t => (
                              <span key={t} className="text-[10px] text-muted-foreground">{t.trim()}</span>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* Pagination */}
          {browseData && browseData.totalPages > 1 && (
            <div className="flex items-center justify-between pt-2 border-t border-border">
              <Button variant="outline" size="sm" disabled={browseData.first}
                onClick={() => fetchProblems(browsePage - 1)}>
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <span className="text-xs text-muted-foreground">
                Page {browsePage + 1} of {browseData.totalPages} ({browseData.totalElements} problems)
              </span>
              <Button variant="outline" size="sm" disabled={browseData.last}
                onClick={() => fetchProblems(browsePage + 1)}>
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          )}

          {/* Selected problems tray */}
          {selectedProblems.length > 0 && (
            <div className="border-t border-border pt-2">
              <Label className="text-xs text-foreground mb-1.5 block">
                Selected ({selectedProblems.length})
              </Label>
              <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto">
                {selectedProblems.map(p => (
                  <Badge key={p.id} variant="secondary"
                    className="text-xs flex items-center gap-1 bg-accent pr-1">
                    <span className="truncate max-w-30">
                      {platLabel(p.platform)} {p.problemNumber ? `#${p.problemNumber}` : ""} {p.title}
                    </span>
                    <button onClick={e => { e.stopPropagation(); toggleProblem(p) }}
                      className="ml-0.5 hover:text-destructive">
                      <X className="w-3 h-3" />
                    </button>
                  </Badge>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ═══════ Error ═══════ */}
      {error && <p className="text-sm text-destructive my-2 text-center">{error}</p>}

      {/* ═══════ Navigation Buttons ═══════ */}
      <div className="flex gap-3 mt-4">
        {step > 1 && (
          <Button variant="outline" className="flex-1 h-11" onClick={() => { setError(""); setStep(step - 1) }}>
            <ChevronLeft className="w-4 h-4 mr-1" /> Back
          </Button>
        )}

        {step < 3 && (
          <Button className="flex-1 h-11 bg-primary text-primary-foreground hover:bg-primary/90"
            disabled={step === 1 && !planName.trim()}
            onClick={() => {
              setError("")
              if (step === 1 && !planName.trim()) { setError("Plan name is required"); return }
              if (step === 2 && intervals.length === 0) { setError("Add at least one interval"); return }
              setStep(step + 1)
            }}>
            Next <ChevronRight className="w-4 h-4 ml-1" />
          </Button>
        )}

        {step === 3 && (
          <Button className="flex-1 h-11 bg-primary text-primary-foreground hover:bg-primary/90"
            disabled={submitting || selectedProblems.length === 0}
            onClick={handleSubmit}>
            <Plus className="w-4 h-4 mr-1" />
            {submitting ? "Creating..." : `Create Plan (${selectedProblems.length})`}
          </Button>
        )}
      </div>
    </div>
  )
}
