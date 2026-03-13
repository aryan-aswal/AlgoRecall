"use client"

import { MobileLayout } from "@/components/mobile-layout"
import { DashboardContent } from "@/components/dashboard-content"
import { AuthGuard } from "@/components/auth-guard"

export default function DashboardPage() {
  return (
    <AuthGuard>
      <MobileLayout>
        <DashboardContent />
      </MobileLayout>
    </AuthGuard>
  )
}


