import { useState, useEffect, useCallback, useRef } from "react";
import { getSettings, updateSettings, authorizeCalendar, disconnectCalendar, type Settings as SettingsData } from "../lib/api";
import { useAuth } from "../lib/auth-context";
import { useTheme } from "../lib/use-theme";
import { toast } from "sonner";
import {
  User, Bell, Smartphone, Mail, Calendar, Clock, Moon,
  Phone, LogOut, Link, Unlink, Save,
} from "lucide-react";
import { cn } from "../lib/utils";

declare global {
  interface Window { google?: any; }
}

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";

export default function Settings() {
  const { logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const [settings, setSettings] = useState<SettingsData | null>(null);
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);
  const gsiLoaded = useRef(false);

  useEffect(() => {
    getSettings()
      .then((s) => { setSettings(s); setPhone(s.phoneNumber || ""); })
      .catch(() => toast.error("Failed to load settings"))
      .finally(() => setLoading(false));
  }, []);

  const update = useCallback(async (field: string, value: any) => {
    setSaving(field);
    try {
      const updated = await updateSettings({ [field]: value });
      setSettings(updated);
      toast.success("Setting updated");
    } catch {
      toast.error("Failed to update setting");
    } finally {
      setSaving(null);
    }
  }, []);

  /* ---- Google Calendar ---- */
  const loadGsi = () =>
    new Promise<void>((resolve) => {
      if (window.google?.accounts?.oauth2) { resolve(); return; }
      if (gsiLoaded.current) { resolve(); return; }
      const s = document.createElement("script");
      s.src = "https://accounts.google.com/gsi/client";
      s.async = true;
      s.onload = () => { gsiLoaded.current = true; resolve(); };
      document.head.appendChild(s);
    });

  const handleConnect = async () => {
    if (!GOOGLE_CLIENT_ID) { toast.error("Google Client ID not configured"); return; }
    setSaving("gcal");
    try {
      await loadGsi();
      const client = window.google.accounts.oauth2.initCodeClient({
        client_id: GOOGLE_CLIENT_ID,
        scope: "https://www.googleapis.com/auth/calendar.events",
        ux_mode: "popup",
        redirect_uri: "postmessage",
        callback: async (resp: any) => {
          if (resp.error) { toast.error("Google authorization failed"); setSaving(null); return; }
          try {
            const result = await authorizeCalendar(resp.code);
            toast.success(result.message || "Google Calendar connected");
            const s = await getSettings();
            setSettings(s);
          } catch { toast.error("Failed to connect Google Calendar"); }
          setSaving(null);
        },
      });
      client.requestCode();
    } catch {
      toast.error("Failed to load Google sign-in");
      setSaving(null);
    }
  };

  const handleDisconnect = async () => {
    setSaving("gcal");
    try {
      await disconnectCalendar();
      const s = await getSettings();
      setSettings(s);
      toast.success("Google Calendar disconnected");
    } catch { toast.error("Failed to disconnect"); }
    setSaving(null);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!settings) return null;

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="text-muted-foreground">Manage your account and preferences</p>
      </div>

      {/* Profile */}
      <Section icon={<User className="w-5 h-5" />} title="Profile">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-14 h-14 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xl font-bold">
            {(settings.username || "?")[0].toUpperCase()}
          </div>
          <div>
            <p className="font-semibold">{settings.username}</p>
            <p className="text-sm text-muted-foreground">{settings.email}</p>
          </div>
        </div>
        <label className="block text-sm font-medium mb-1">Phone Number</label>
        <div className="flex gap-2">
          <input
            className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="+1 234 567 8900"
          />
          <button
            onClick={() => update("phoneNumber", phone || null)}
            disabled={saving === "phoneNumber"}
            className="flex items-center gap-1.5 bg-primary text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-primary/90 disabled:opacity-50"
          >
            <Save className="w-4 h-4" /> Save
          </button>
        </div>
      </Section>

      {/* Notifications */}
      <Section icon={<Bell className="w-5 h-5" />} title="Notifications">
        <Toggle label="Email Notifications" icon={<Mail className="w-4 h-4" />}
          checked={settings.emailNotifications}
          loading={saving === "emailNotifications"}
          onChange={(v) => update("emailNotifications", v)} />
        <Toggle label="Push Notifications" icon={<Smartphone className="w-4 h-4" />}
          checked={settings.pushNotifications}
          loading={saving === "pushNotifications"}
          onChange={(v) => update("pushNotifications", v)} />
        <Toggle label="SMS Notifications" icon={<Phone className="w-4 h-4" />}
          checked={settings.smsNotifications}
          loading={saving === "smsNotifications"}
          onChange={(v) => update("smsNotifications", v)}
          description={!settings.phoneNumber ? "Add a phone number above to enable SMS" : undefined} />
      </Section>

      {/* Calendar */}
      <Section icon={<Calendar className="w-5 h-5" />} title="Google Calendar">
        {settings.googleCalendarConnected ? (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-green-500" />
              <span className="text-sm font-medium">Connected</span>
            </div>
            <button onClick={handleDisconnect} disabled={saving === "gcal"}
              className="flex items-center gap-1.5 text-sm text-destructive hover:underline disabled:opacity-50">
              <Unlink className="w-4 h-4" /> Disconnect
            </button>
          </div>
        ) : (
          <button onClick={handleConnect} disabled={saving === "gcal"}
            className="flex items-center gap-2 bg-primary text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-primary/90 disabled:opacity-50">
            <Link className="w-4 h-4" /> Connect Google Calendar
          </button>
        )}
        <Toggle label="Auto-sync Revisions" icon={<Calendar className="w-4 h-4" />}
          checked={settings.calendarSync}
          loading={saving === "calendarSync"}
          onChange={(v) => update("calendarSync", v)}
          description="Automatically create calendar events for revisions" />
      </Section>

      {/* Study Durations */}
      <Section icon={<Clock className="w-5 h-5" />} title="Study Durations">
        <Slider label="New Problem Duration" value={settings.newProblemDuration} min={10} max={120} step={5}
          suffix="min" loading={saving === "newProblemDuration"}
          onChange={(v) => update("newProblemDuration", v)} />
        <Slider label="Revision Duration" value={settings.revisionDuration} min={5} max={60} step={5}
          suffix="min" loading={saving === "revisionDuration"}
          onChange={(v) => update("revisionDuration", v)} />
        <p className="text-xs text-muted-foreground mt-2">
          These durations determine how long calendar events are blocked for new problems and revisions.
        </p>
      </Section>

      {/* Appearance */}
      <Section icon={<Moon className="w-5 h-5" />} title="Appearance">
        <Toggle label="Dark Mode" icon={<Moon className="w-4 h-4" />}
          checked={theme === "dark"}
          onChange={(v) => { setTheme(v ? "dark" : "light"); update("darkMode", v); }} />
      </Section>

      {/* Sign Out */}
      <button onClick={logout}
        className="w-full flex items-center justify-center gap-2 border border-destructive text-destructive rounded-xl py-3 text-sm font-medium hover:bg-destructive/5 transition-colors">
        <LogOut className="w-4 h-4" /> Sign Out
      </button>
    </div>
  );
}

/* ---- Reusable pieces ---- */

function Section({ icon, title, children }: { icon: React.ReactNode; title: string; children: React.ReactNode }) {
  return (
    <div className="bg-card border border-border rounded-xl p-6">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-primary">{icon}</span>
        <h2 className="text-lg font-semibold">{title}</h2>
      </div>
      <div className="space-y-4">{children}</div>
    </div>
  );
}

function Toggle({
  label, icon, checked, loading, description, onChange,
}: {
  label: string; icon?: React.ReactNode; checked: boolean;
  loading?: boolean; description?: string;
  onChange: (v: boolean) => void;
}) {
  return (
    <div>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm">
          {icon && <span className="text-muted-foreground">{icon}</span>}
          <span>{label}</span>
        </div>
        <button
          onClick={() => onChange(!checked)}
          disabled={!!loading}
          className={cn(
            "relative w-11 h-6 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary/50",
            checked ? "bg-primary" : "bg-muted-foreground/30"
          )}
        >
          <span className={cn(
            "absolute top-0.5 left-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform",
            checked && "translate-x-5"
          )} />
        </button>
      </div>
      {description && <p className="text-xs text-muted-foreground mt-1 ml-6">{description}</p>}
    </div>
  );
}

function Slider({
  label, value, min, max, step, suffix, loading, onChange,
}: {
  label: string; value: number; min: number; max: number; step: number;
  suffix: string; loading?: boolean; onChange: (v: number) => void;
}) {
  const [local, setLocal] = useState(value);
  useEffect(() => setLocal(value), [value]);

  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <span className="text-sm">{label}</span>
        <span className="text-sm font-bold tabular-nums">{local} {suffix}</span>
      </div>
      <input
        type="range"
        min={min} max={max} step={step}
        value={local}
        onChange={(e) => setLocal(Number(e.target.value))}
        onMouseUp={() => { if (local !== value) onChange(local); }}
        onTouchEnd={() => { if (local !== value) onChange(local); }}
        disabled={!!loading}
        className="w-full accent-primary"
      />
    </div>
  );
}
