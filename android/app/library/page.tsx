"use client"

import { MobileLayout } from "@/components/mobile-layout"
import { LibraryContent } from "@/components/library-content"
import { AuthGuard } from "@/components/auth-guard"

export default function LibraryPage() {
  return (
    <AuthGuard>
      <MobileLayout>
        <LibraryContent />
      </MobileLayout>
    </AuthGuard>
  )
}
