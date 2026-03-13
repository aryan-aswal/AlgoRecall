import { useState, useEffect } from "react";
import { browseProblems, getTopics, createStudyPlan, type BrowseProblem } from "../lib/api";
import { toast } from "sonner";
import { cn } from "../lib/utils";
import {
  ArrowLeft, ArrowRight, Check, Search, Plus, X, Eye, Clock
} from "lucide-react";

interface RevisionInterval { value: number; type: string }

const intervalTypes = ["Days", "Weeks", "Months"];

export default function AddPlan() {
  const [step, setStep] = useState(1);

  // Step 1
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  // Step 2
  const [intervals, setIntervals] = useState<RevisionInterval[]>([
    { value: 1, type: "Days" },
    { value: 3, type: "Days" },
    { value: 1, type: "Weeks" },
    { value: 2, type: "Weeks" },
    { value: 1, type: "Months" },
  ]);
  const [reminderTime, setReminderTime] = useState("09:00");

  // Step 3
  const [problems, setProblems] = useState<BrowseProblem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [platform, setPlatform] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [topic, setTopic] = useState("");
  const [topics, setTopics] = useState<string[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getTopics().then(setTopics).catch(() => {});
  }, []);

  useEffect(() => {
    const params: Record<string, string> = { page: String(page), size: "20" };
    if (search) params.search = search;
    if (platform) params.platform = platform;
    if (difficulty) params.difficulty = difficulty;
    if (topic) params.topic = topic;
    browseProblems(params).then((d) => {
      setProblems(d.content);
      setTotalPages(d.totalPages);
    }).catch(() => {});
  }, [page, search, platform, difficulty, topic]);

  const toggleProblem = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const addInterval = () => setIntervals([...intervals, { value: 1, type: "Days" }]);
  const removeInterval = (i: number) => setIntervals(intervals.filter((_, idx) => idx !== i));
  const updateInterval = (i: number, field: keyof RevisionInterval, v: number | string) => {
    const copy = [...intervals];
    copy[i] = { ...copy[i], [field]: v };
    setIntervals(copy);
  };
  const toDays = (iv: RevisionInterval) => {
    if (iv.type === "Weeks") return iv.value * 7;
    if (iv.type === "Months") return iv.value * 30;
    return iv.value;
  };
  const getSchedulePreview = () => {
    const today = new Date();
    return intervals.map((iv, idx) => {
      const d = new Date(today);
      if (iv.type === "Days") d.setDate(d.getDate() + iv.value);
      else if (iv.type === "Weeks") d.setDate(d.getDate() + iv.value * 7);
      else d.setMonth(d.getMonth() + iv.value);
      return { revision: idx + 1, date: d.toLocaleDateString("en-US", { month: "short", day: "numeric" }) };
    });
  };

  const handleSubmit = async () => {
    if (!name.trim()) return toast.error("Plan name is required");
    if (selected.size === 0) return toast.error("Select at least one problem");
    setSubmitting(true);
    try {
      await createStudyPlan({
        name: name.trim(),
        description: description.trim() || undefined,
        problemIds: Array.from(selected),
        revisionIntervals: intervals.map(toDays),
        reminderTime: reminderTime || undefined,
      });
      toast.success("Study plan created!");
      setStep(1);
      setName("");
      setDescription("");
      setSelected(new Set());
    } catch (err: any) {
      toast.error(err.message || "Failed to create plan");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Create Study Plan</h1>
        <p className="text-muted-foreground">Set up a new spaced repetition plan</p>
      </div>

      {/* Progress */}
      <div className="flex items-center gap-3">
        {[1, 2, 3].map((s) => (
          <div key={s} className="flex items-center gap-2">
            <div
              className={cn(
                "w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors",
                step >= s ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
              )}
            >
              {step > s ? <Check className="w-4 h-4" /> : s}
            </div>
            <span className={cn("text-sm font-medium", step >= s ? "text-foreground" : "text-muted-foreground")}>
              {s === 1 ? "Details" : s === 2 ? "Schedule" : "Problems"}
            </span>
            {s < 3 && <div className={cn("w-12 h-0.5", step > s ? "bg-primary" : "bg-border")} />}
          </div>
        ))}
      </div>

      {/* Step 1: Details */}
      {step === 1 && (
        <div className="bg-card border border-border rounded-xl p-6 space-y-5">
          <div>
            <label className="block text-sm font-medium mb-1.5">Plan Name *</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Binary Search Mastery"
              className="w-full h-11 px-4 rounded-lg border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1.5">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What's this plan about?"
              rows={3}
              className="w-full px-4 py-3 rounded-lg border border-input bg-background text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring resize-none"
            />
          </div>
          <div className="flex justify-end">
            <button
              onClick={() => { if (name.trim()) setStep(2); else toast.error("Plan name is required"); }}
              className="flex items-center gap-2 px-5 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/90"
            >
              Next <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 2: Schedule */}
      {step === 2 && (
        <div className="bg-card border border-border rounded-xl p-6 space-y-6">
          <div>
            <h3 className="text-base font-semibold mb-4">Revision Policy</h3>
            
            <div className="mb-6">
              <label className="block text-sm font-medium">Revision Intervals</label>
              <p className="text-xs text-muted-foreground mb-4">Applied to every problem in this plan</p>
              
              <div className="flex flex-col gap-3">
                {intervals.map((iv, idx) => (
                  <div key={idx} className="flex items-center gap-3">
                    <span className="text-xs text-muted-foreground w-6">#{idx + 1}</span>
                    <input
                      type="number"
                      min={1}
                      value={iv.value}
                      onChange={(e) => updateInterval(idx, "value", Number(e.target.value))}
                      className="w-20 h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-1 focus:ring-ring"
                    />
                    <select
                      value={iv.type}
                      onChange={(e) => updateInterval(idx, "type", e.target.value)}
                      className="w-28 h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-1 focus:ring-ring"
                    >
                      {intervalTypes.map((t) => (
                        <option key={t} value={t}>{t}</option>
                      ))}
                    </select>
                    {intervals.length > 1 && (
                      <button
                        onClick={() => removeInterval(idx)}
                        className="p-1.5 text-muted-foreground hover:text-destructive rounded-md hover:bg-muted"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                ))}
                
                <button
                  onClick={addInterval}
                  className="mt-2 flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium border border-border rounded-lg hover:bg-muted w-fit"
                >
                  <Plus className="w-4 h-4" /> Add Interval
                </button>
              </div>
            </div>

            <div className="mb-6">
              <label className="flex items-center gap-1.5 text-sm font-medium">
                <Clock className="w-4 h-4 text-muted-foreground" /> Reminder Time
              </label>
              <p className="text-xs text-muted-foreground mb-3">When should revision reminders arrive?</p>
              <input
                type="time"
                value={reminderTime}
                onChange={(e) => setReminderTime(e.target.value)}
                className="w-32 h-10 px-3 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-1 focus:ring-ring"
              />
            </div>

            <div className="pt-5 border-t border-border">
              <label className="flex items-center gap-1.5 text-sm font-medium">
                <Eye className="w-4 h-4 text-muted-foreground" /> Schedule Preview
              </label>
              <p className="text-xs text-muted-foreground mb-3">
                Each problem will be revised on these dates at {reminderTime || "09:00"}
              </p>
              <div className="flex flex-wrap gap-2">
                {getSchedulePreview().map((p) => (
                  <div key={p.revision} className="px-2.5 py-1 rounded-md text-xs font-medium bg-primary/10 text-primary">
                    R{p.revision}: {p.date}
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="flex justify-between">
            <button
              onClick={() => setStep(1)}
              className="flex items-center gap-2 px-5 py-2.5 border border-border rounded-lg text-sm font-medium hover:bg-muted"
            >
              <ArrowLeft className="w-4 h-4" /> Back
            </button>
            <button
              onClick={() => setStep(3)}
              className="flex items-center gap-2 px-5 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/90"
            >
              Next <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Step 3: Browse & Select Problems */}
      {step === 3 && (
        <div className="space-y-4">
          <div className="bg-card border border-border rounded-xl p-4">
            <div className="flex flex-wrap gap-3">
              <div className="relative flex-1 min-w-[200px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <input
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0); }}
                  placeholder="Search problems..."
                  className="w-full h-10 pl-10 pr-4 rounded-lg border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <select
                value={platform}
                onChange={(e) => { setPlatform(e.target.value); setPage(0); }}
                className="h-10 px-3 rounded-lg border border-input bg-background text-sm"
              >
                <option value="">All Platforms</option>
                <option value="LEETCODE">LeetCode</option>
                <option value="GFG">GFG</option>
                <option value="CODEFORCES">Codeforces</option>
              </select>
              <select
                value={difficulty}
                onChange={(e) => { setDifficulty(e.target.value); setPage(0); }}
                className="h-10 px-3 rounded-lg border border-input bg-background text-sm"
              >
                <option value="">All Difficulties</option>
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
              <select
                value={topic}
                onChange={(e) => { setTopic(e.target.value); setPage(0); }}
                className="h-10 px-3 rounded-lg border border-input bg-background text-sm max-w-[180px]"
              >
                <option value="">All Topics</option>
                {topics.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>

          {selected.size > 0 && (
            <div className="bg-primary/10 text-primary rounded-lg px-4 py-2.5 text-sm font-medium">
              {selected.size} problem{selected.size > 1 ? "s" : ""} selected
            </div>
          )}

          <div className="bg-card border border-border rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="py-3 px-4 text-left w-10"></th>
                  <th className="py-3 px-4 text-left font-medium">#</th>
                  <th className="py-3 px-4 text-left font-medium">Title</th>
                  <th className="py-3 px-4 text-left font-medium">Platform</th>
                  <th className="py-3 px-4 text-left font-medium">Difficulty</th>
                  <th className="py-3 px-4 text-left font-medium">Topics</th>
                </tr>
              </thead>
              <tbody>
                {problems.map((p) => (
                  <tr
                    key={p.id}
                    onClick={() => toggleProblem(p.id)}
                    className={cn(
                      "border-b border-border last:border-0 cursor-pointer transition-colors",
                      selected.has(p.id) ? "bg-primary/5" : "hover:bg-muted/50"
                    )}
                  >
                    <td className="py-3 px-4">
                      <div className={cn(
                        "w-5 h-5 rounded border-2 flex items-center justify-center transition-colors",
                        selected.has(p.id) ? "bg-primary border-primary" : "border-input"
                      )}>
                        {selected.has(p.id) && <Check className="w-3 h-3 text-primary-foreground" />}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-muted-foreground">{p.problemNumber}</td>
                    <td className="py-3 px-4 font-medium">{p.title}</td>
                    <td className="py-3 px-4 text-muted-foreground">{p.platform}</td>
                    <td className="py-3 px-4">
                      <span className={cn(
                        "text-xs font-medium px-2 py-0.5 rounded-full",
                        p.difficulty === "EASY" ? "text-green-600 bg-green-50 dark:bg-green-400/10" :
                        p.difficulty === "MEDIUM" ? "text-yellow-600 bg-yellow-50 dark:bg-yellow-400/10" :
                        "text-red-600 bg-red-50 dark:bg-red-400/10"
                      )}>
                        {p.difficulty}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-muted-foreground text-xs max-w-[200px] truncate">
                      {p.topicTags || "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm border border-border rounded-md hover:bg-muted disabled:opacity-50"
              >
                Prev
              </button>
              <span className="px-3 py-1.5 text-sm text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 text-sm border border-border rounded-md hover:bg-muted disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}

          <div className="flex justify-between">
            <button
              onClick={() => setStep(2)}
              className="flex items-center gap-2 px-5 py-2.5 border border-border rounded-lg text-sm font-medium hover:bg-muted"
            >
              <ArrowLeft className="w-4 h-4" /> Back
            </button>
            <button
              onClick={handleSubmit}
              disabled={submitting || selected.size === 0}
              className="flex items-center gap-2 px-6 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-medium hover:bg-primary/90 disabled:opacity-50"
            >
              {submitting ? (
                <div className="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
              ) : (
                <>Create Plan <Check className="w-4 h-4" /></>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
