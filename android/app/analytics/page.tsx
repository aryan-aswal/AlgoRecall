"use client"

import { MobileLayout } from "@/components/mobile-layout"
import { AnalyticsContent } from "@/components/analytics-content"
import { AuthGuard } from "@/components/auth-guard"

export default function AnalyticsPage() {
  return (
    <AuthGuard>
      <MobileLayout>
        <AnalyticsContent />
      </MobileLayout>
    </AuthGuard>
  )
}
