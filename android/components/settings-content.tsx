"use client"

import { useState, useEffect } from "react"
import {
  Mail, Bell, Calendar as CalendarIcon, LogOut, ChevronRight,
  Moon, Shield, HelpCircle, Info, Phone, Clock, Timer, MessageSquare
} from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Slider } from "@/components/ui/slider"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import { getSettings, updateSettings, authorizeCalendar, disconnectCalendar } from "@/lib/api"
import { useAuth } from "@/lib/auth-context"
import { useTheme } from "next-themes"
import { toast } from "sonner"

export function SettingsContent() {
  const { user, logout } = useAuth()
  const { setTheme } = useTheme()
  const [emailNotifications, setEmailNotifications] = useState(true)
  const [pushNotifications, setPushNotifications] = useState(true)
  const [smsNotifications, setSmsNotifications] = useState(false)
  const [calendarSync, setCalendarSync] = useState(false)
  const [darkMode, setDarkMode] = useState(false)
  const [newProblemDuration, setNewProblemDuration] = useState(25)
  const [revisionDuration, setRevisionDuration] = useState(15)
  const [phoneNumber, setPhoneNumber] = useState("")
  const [phoneSaved, setPhoneSaved] = useState(false)
  const [loading, setLoading] = useState(true)
  const [calendarSyncing, setCalendarSyncing] = useState(false)
  const [googleCalendarConnected, setGoogleCalendarConnected] = useState(false)

  useEffect(() => {
    getSettings()
      .then((data) => {
        setEmailNotifications(data.emailNotifications)
        setPushNotifications(data.pushNotifications)
        setSmsNotifications(data.smsNotifications)
        setCalendarSync(data.calendarSync)
        setDarkMode(data.darkMode)
        setNewProblemDuration(data.newProblemDuration)
        setRevisionDuration(data.revisionDuration)
        setPhoneNumber(data.phoneNumber || "")
        setGoogleCalendarConnected(data.googleCalendarConnected)
        // Sync theme with backend preference
        setTheme(data.darkMode ? "dark" : "light")
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const handleToggle = async (setting: string, value: boolean) => {
    const updates: Record<string, boolean> = {}
    switch (setting) {
      case "Email Notifications":
        setEmailNotifications(value)
        updates.emailNotifications = value
        break
      case "Push Notifications":
        setPushNotifications(value)
        updates.pushNotifications = value
        break
      case "SMS Notifications":
        setSmsNotifications(value)
        updates.smsNotifications = value
        break
      case "Calendar Sync":
        setCalendarSync(value)
        updates.calendarSync = value
        break
      case "Dark Mode":
        setDarkMode(value)
        updates.darkMode = value
        setTheme(value ? "dark" : "light")
        break
    }
    try {
      await updateSettings(updates)
    } catch { /* silent */ }
  }

  const handleDurationChange = async (field: "newProblemDuration" | "revisionDuration", value: number) => {
    if (field === "newProblemDuration") setNewProblemDuration(value)
    else setRevisionDuration(value)
    try {
      await updateSettings({ [field]: value })
    } catch { /* silent */ }
  }

  const handlePhoneSave = async () => {
    try {
      await updateSettings({ phoneNumber })
      setPhoneSaved(true)
      setTimeout(() => setPhoneSaved(false), 2000)
    } catch { /* silent */ }
  }

  interface SettingItem {
    icon: React.ElementType; label: string
    description?: string; type: "toggle" | "link"
    value?: boolean
  }

  const notificationSettings: SettingItem[] = [
    { icon: Mail, label: "Email Notifications", description: "Receive revision reminders via email", type: "toggle", value: emailNotifications },
    { icon: Bell, label: "Push Notifications", description: "Get notified on your device", type: "toggle", value: pushNotifications },
    { icon: MessageSquare, label: "SMS Notifications", description: "Receive reminders via SMS", type: "toggle", value: smsNotifications },
  ]

  const appearanceSettings: SettingItem[] = [
    { icon: Moon, label: "Dark Mode", description: "Switch to dark theme", type: "toggle", value: darkMode },
  ]

  const supportSettings: SettingItem[] = [
    { icon: HelpCircle, label: "Help & Support", type: "link" },
    { icon: Shield, label: "Privacy Policy", type: "link" },
    { icon: Info, label: "About", type: "link" },
  ]

  const initials = user ? user.username.substring(0, 2).toUpperCase() : "U"

  return (
    <div className="px-4 py-4">
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-foreground">Settings</h1>
        <p className="text-sm text-muted-foreground">Manage your preferences</p>
      </div>

      {/* Profile Section */}
      <Card className="bg-card border-border shadow-sm mb-4">
        <CardContent className="p-4">
          <div className="flex items-center gap-4 mb-4">
            <Avatar className="w-16 h-16 border-2 border-primary">
              <AvatarFallback className="bg-primary text-primary-foreground text-xl font-semibold">{initials}</AvatarFallback>
            </Avatar>
            <div className="flex-1">
              <h2 className="text-lg font-semibold text-foreground">{user?.username || "User"}</h2>
              <p className="text-sm text-muted-foreground">{user?.email || ""}</p>
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label className="text-sm text-foreground flex items-center gap-1.5">
              <Phone className="w-3.5 h-3.5" />Phone Number
            </Label>
            <div className="flex items-center gap-2">
              <Input placeholder="+91 98765 43210" value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                className="h-9 bg-background border-input flex-1" />
              <Button size="sm" variant="outline" className="h-9 px-3" onClick={handlePhoneSave}>
                {phoneSaved ? "✓" : "Save"}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Study Durations */}
      <Card className="bg-card border-border shadow-sm mb-4">
        <CardHeader className="pb-2 px-4 pt-4">
          <CardTitle className="text-sm font-semibold text-foreground flex items-center gap-2">
            <Timer className="w-4 h-4 text-primary" />Study Durations
            <TooltipProvider delayDuration={200}>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Info className="w-3.5 h-3.5 text-muted-foreground cursor-help" />
                </TooltipTrigger>
                <TooltipContent side="bottom" className="max-w-65 text-xs">
                  These durations are used to block time on your Google Calendar when Calendar Sync is enabled. New problems block {newProblemDuration} min and revisions block {revisionDuration} min.
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </CardTitle>
        </CardHeader>
        <CardContent className="px-4 pb-4 flex flex-col gap-5">
          <div>
            <div className="flex items-center justify-between mb-2">
              <Label className="text-sm text-foreground flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />New Problem
              </Label>
              <span className="text-sm font-medium text-primary">{newProblemDuration} min</span>
            </div>
            <Slider value={[newProblemDuration]} min={10} max={120} step={5}
              onValueCommit={(v) => handleDurationChange("newProblemDuration", v[0])}
              onValueChange={(v) => setNewProblemDuration(v[0])}
              className="w-full" />
            <p className="text-xs text-muted-foreground mt-1">Time allocated for solving a new problem</p>
          </div>
          <Separator />
          <div>
            <div className="flex items-center justify-between mb-2">
              <Label className="text-sm text-foreground flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />Revision
              </Label>
              <span className="text-sm font-medium text-primary">{revisionDuration} min</span>
            </div>
            <Slider value={[revisionDuration]} min={5} max={60} step={5}
              onValueCommit={(v) => handleDurationChange("revisionDuration", v[0])}
              onValueChange={(v) => setRevisionDuration(v[0])}
              className="w-full" />
            <p className="text-xs text-muted-foreground mt-1">Time for each revision session (used for calendar blocking)</p>
          </div>
        </CardContent>
      </Card>

      {/* Notifications */}
      <Card className="bg-card border-border shadow-sm mb-4">
        <CardHeader className="pb-2 px-4 pt-4">
          <CardTitle className="text-sm font-semibold text-foreground">Notifications</CardTitle>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <div className="flex flex-col gap-1">
            {notificationSettings.map((setting, idx) => (
              <div key={setting.label}>
                <div className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg bg-accent flex items-center justify-center">
                      <setting.icon className="w-4 h-4 text-accent-foreground" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground">{setting.label}</p>
                      {setting.description && <p className="text-xs text-muted-foreground">{setting.description}</p>}
                    </div>
                  </div>
                  <Switch checked={setting.value} onCheckedChange={(checked) => handleToggle(setting.label, checked)} />
                </div>
                {idx < notificationSettings.length - 1 && <Separator />}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Google Calendar */}
      <Card className="bg-card border-border shadow-sm mb-4">
        <CardHeader className="pb-2 px-4 pt-4">
          <CardTitle className="text-sm font-semibold text-foreground">Google Calendar</CardTitle>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          {googleCalendarConnected ? (
            <div className="flex flex-col gap-3">
              <div className="flex items-center gap-3 py-2">
                <div className="w-9 h-9 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                  <CalendarIcon className="w-4 h-4 text-green-600 dark:text-green-400" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-foreground">Connected</p>
                  <p className="text-xs text-muted-foreground">Revisions are auto-synced to your calendar</p>
                </div>
              </div>
              <Button variant="outline" size="sm" className="text-destructive border-destructive/30 hover:bg-destructive/10"
                onClick={async () => {
                  try {
                    await disconnectCalendar()
                    setGoogleCalendarConnected(false)
                    setCalendarSync(false)
                  } catch { /* silent */ }
                }}>
                Disconnect Google Calendar
              </Button>
            </div>
          ) : (
            <Button variant="outline" className="w-full h-11 justify-start gap-3" disabled={calendarSyncing}
              onClick={async () => {
                setCalendarSyncing(true)
                try {
                  const google = (window as any).google
                  if (!google?.accounts?.oauth2) {
                    await new Promise<void>((resolve, reject) => {
                      const script = document.createElement("script")
                      script.src = "https://accounts.google.com/gsi/client"
                      script.onload = () => resolve()
                      script.onerror = () => reject(new Error("Failed to load Google Identity Services"))
                      document.head.appendChild(script)
                    })
                  }
                  const g = (window as any).google
                  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
                  if (!clientId) {
                    toast.error("Google Client ID not configured.")
                    setCalendarSyncing(false)
                    return
                  }
                  const codeClient = g.accounts.oauth2.initCodeClient({
                    client_id: clientId,
                    scope: "https://www.googleapis.com/auth/calendar.events",
                    ux_mode: "popup",
                    callback: async (response: any) => {
                      if (response.code) {
                        try {
                          const result = await authorizeCalendar(response.code)
                          setGoogleCalendarConnected(true)
                          setCalendarSync(true)
                          toast.success(`Google Calendar connected! ${result.synced} existing revision(s) synced.`)
                        } catch (e: any) {
                          toast.error("Failed to connect: " + (e.message || "Unknown error"))
                        }
                      }
                      setCalendarSyncing(false)
                    },
                  })
                  codeClient.requestCode()
                } catch {
                  toast.error("Failed to initialize Google OAuth")
                  setCalendarSyncing(false)
                }
              }}>
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span className="text-sm">{calendarSyncing ? "Connecting..." : "Connect Google Calendar"}</span>
            </Button>
          )}
        </CardContent>
      </Card>

      {/* Appearance */}
      <Card className="bg-card border-border shadow-sm mb-4">
        <CardHeader className="pb-2 px-4 pt-4">
          <CardTitle className="text-sm font-semibold text-foreground">Appearance</CardTitle>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <div className="flex flex-col gap-1">
            {appearanceSettings.map((setting) => (
              <div key={setting.label} className="flex items-center justify-between py-3">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-lg bg-accent flex items-center justify-center">
                    <setting.icon className="w-4 h-4 text-accent-foreground" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-foreground">{setting.label}</p>
                    {setting.description && <p className="text-xs text-muted-foreground">{setting.description}</p>}
                  </div>
                </div>
                <Switch checked={setting.value} onCheckedChange={(checked) => handleToggle(setting.label, checked)} />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Support */}
      <Card className="bg-card border-border shadow-sm mb-4">
        <CardHeader className="pb-2 px-4 pt-4">
          <CardTitle className="text-sm font-semibold text-foreground">Support</CardTitle>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <div className="flex flex-col gap-1">
            {supportSettings.map((setting, idx) => (
              <div key={setting.label}>
                <button className="flex items-center justify-between py-3 w-full">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg bg-accent flex items-center justify-center">
                      <setting.icon className="w-4 h-4 text-accent-foreground" />
                    </div>
                    <p className="text-sm font-medium text-foreground">{setting.label}</p>
                  </div>
                  <ChevronRight className="w-4 h-4 text-muted-foreground" />
                </button>
                {idx < supportSettings.length - 1 && <Separator />}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Button
        variant="outline"
        className="w-full h-12 text-destructive border-destructive/30 hover:bg-destructive/10 hover:text-destructive"
        onClick={logout}
      >
        <LogOut className="w-4 h-4 mr-2" />Sign Out
      </Button>

      <p className="text-center text-xs text-muted-foreground mt-4">AlgoRecall v1.0.0</p>
    </div>
  )
}
