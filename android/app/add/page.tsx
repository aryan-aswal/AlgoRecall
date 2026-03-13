"use client"

import { MobileLayout } from "@/components/mobile-layout"
import { AddProblemContent } from "@/components/add-problem-content"
import { AuthGuard } from "@/components/auth-guard"

export default function CreateStudyPlanPage() {
  return (
    <AuthGuard>
      <MobileLayout>
        <AddProblemContent />
      </MobileLayout>
    </AuthGuard>
  )
}


