"use client"

import { MobileLayout } from "@/components/mobile-layout"
import { SettingsContent } from "@/components/settings-content"
import { AuthGuard } from "@/components/auth-guard"

export default function SettingsPage() {
  return (
    <AuthGuard>
      <MobileLayout>
        <SettingsContent />
      </MobileLayout>
    </AuthGuard>
  )
}
