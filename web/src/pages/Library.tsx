import { useState, useEffect } from "react";
import { getStudyPlans, type StudyPlan } from "../lib/api";
import { toast } from "sonner";
import { BookOpen, Clock, ArrowLeft, ExternalLink, ChevronRight } from "lucide-react";
import { cn } from "../lib/utils";

const PLAN_COLORS = [
  ["from-indigo-500", "to-purple-500"],
  ["from-emerald-500", "to-teal-500"],
  ["from-orange-500", "to-red-500"],
  ["from-blue-500", "to-cyan-500"],
  ["from-pink-500", "to-rose-500"],
  ["from-violet-500", "to-fuchsia-500"],
];

function getDifficultyColor(d: string) {
  const u = d.toUpperCase();
  if (u === "EASY") return "text-green-300";
  if (u === "MEDIUM") return "text-yellow-300";
  if (u === "HARD") return "text-red-300";
  return "text-white/60";
}

export default function Library() {
  const [plans, setPlans] = useState<StudyPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<StudyPlan | null>(null);

  useEffect(() => {
    getStudyPlans()
      .then(setPlans)
      .catch(() => toast.error("Failed to load plans"))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (selected) {
    const colors = PLAN_COLORS[plans.indexOf(selected) % PLAN_COLORS.length];
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        <button
          onClick={() => setSelected(null)}
          className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Plans
        </button>

        <div className={cn("bg-gradient-to-r rounded-xl p-6 text-white", colors[0], colors[1])}>
          <h1 className="text-2xl font-bold">{selected.name}</h1>
          {selected.description && <p className="text-white/80 mt-1">{selected.description}</p>}
          <div className="flex items-center gap-4 mt-4 text-sm text-white/70">
            <span className="flex items-center gap-1">
              <BookOpen className="w-4 h-4" /> {selected.problems.length} problems
            </span>
            {selected.reminderTime && (
              <span className="flex items-center gap-1">
                <Clock className="w-4 h-4" /> {selected.reminderTime}
              </span>
            )}
          </div>
          {selected.revisionIntervals && (
            <div className="flex gap-2 mt-3">
              {selected.revisionIntervals.map((d, i) => (
                <span key={i} className="text-xs bg-white/20 backdrop-blur px-2 py-0.5 rounded-full">
                  Day {d}
                </span>
              ))}
            </div>
          )}
        </div>

        <div className="space-y-3">
          {selected.problems.length === 0 ? (
            <p className="text-center text-muted-foreground py-12">No problems in this plan</p>
          ) : (
            selected.problems.map((spp, i) => (
              <div key={spp.id} className="bg-card border border-border rounded-xl p-4 flex items-center gap-4">
                <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center text-sm font-bold text-muted-foreground">
                  {i + 1}
                </div>
                <div className="flex-1">
                  <p className="font-medium text-foreground">{spp.title}</p>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-xs text-muted-foreground">{spp.platform}</span>
                    <span className={cn("text-xs font-medium", getDifficultyColor(spp.difficulty))}>
                      {spp.difficulty}
                    </span>
                    {spp.topicTags && (
                      <span className="text-xs text-muted-foreground">
                        {spp.topicTags.split(",").slice(0, 3).join(", ")}
                      </span>
                    )}
                  </div>
                </div>
                {spp.url && (
                  <a
                    href={spp.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:text-primary/80"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Study Plans</h1>
        <p className="text-muted-foreground">Your spaced repetition plans</p>
      </div>

      {plans.length === 0 ? (
        <div className="text-center py-20">
          <BookOpen className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
          <p className="text-lg font-medium text-foreground">No study plans yet</p>
          <p className="text-muted-foreground">Create your first plan to get started</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {plans.map((plan, i) => {
            const colors = PLAN_COLORS[i % PLAN_COLORS.length];
            return (
              <button
                key={plan.id}
                onClick={() => setSelected(plan)}
                className={cn(
                  "bg-gradient-to-br rounded-xl p-5 text-left text-white transition-transform hover:scale-[1.02] active:scale-100",
                  colors[0], colors[1]
                )}
              >
                <div className="flex justify-between items-start">
                  <h3 className="font-bold text-lg">{plan.name}</h3>
                  <ChevronRight className="w-5 h-5 text-white/60" />
                </div>
                {plan.description && (
                  <p className="text-sm text-white/70 mt-1 line-clamp-2">{plan.description}</p>
                )}
                <div className="flex items-center gap-3 mt-4 text-sm text-white/70">
                  <span>{plan.problems.length} problems</span>
                  {plan.reminderTime && <span>⏰ {plan.reminderTime}</span>}
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
