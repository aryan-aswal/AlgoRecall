"use client"

import { MobileLayout } from "@/components/mobile-layout"
import { CalendarContent } from "@/components/calendar-content"
import { AuthGuard } from "@/components/auth-guard"

export default function CalendarPage() {
  return (
    <AuthGuard>
      <MobileLayout>
        <CalendarContent />
      </MobileLayout>
    </AuthGuard>
  )
}
