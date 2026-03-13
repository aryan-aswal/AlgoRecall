"use client"

import { Home, Plus, Database, Calendar, BarChart3, Settings } from "lucide-react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"

const navItems = [
  { href: "/", icon: Home, label: "Home" },
  { href: "/add", icon: Plus, label: "Add" },
  { href: "/library", icon: Database, label: "Library" },
  { href: "/calendar", icon: Calendar, label: "Calendar" },
  { href: "/analytics", icon: BarChart3, label: "Analytics" },
  { href: "/settings", icon: Settings, label: "Settings" },
]

export function MobileLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()

  return (
    <div className="flex flex-col min-h-screen max-w-md mx-auto bg-background">
      {/* Mobile Frame */}
      <div className="relative flex-1 flex flex-col overflow-hidden rounded-[2.5rem] border-[8px] border-foreground/90 shadow-2xl my-4 mx-auto w-full max-w-[390px] h-[844px]">
        {/* Status Bar */}
        <div className="flex items-center justify-between px-6 py-2 bg-card">
          <span className="text-xs font-medium text-foreground">9:41</span>
          <div className="flex items-center gap-1">
            <div className="flex gap-[2px]">
              <div className="w-[3px] h-2 bg-foreground rounded-sm" />
              <div className="w-[3px] h-2.5 bg-foreground rounded-sm" />
              <div className="w-[3px] h-3 bg-foreground rounded-sm" />
              <div className="w-[3px] h-3.5 bg-foreground rounded-sm" />
            </div>
            <svg className="w-4 h-4 ml-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 3C7.46 3 3.34 4.78.29 7.67c-.18.18-.29.43-.29.71 0 .28.11.53.29.71l2.48 2.48c.18.18.43.29.71.29.27 0 .52-.11.7-.28.79-.74 1.69-1.36 2.66-1.85.33-.16.56-.5.56-.88V5.41c1.48-.55 3.07-.85 4.7-.85s3.22.3 4.7.85v3.44c0 .38.23.72.56.88.97.49 1.87 1.11 2.66 1.85.18.17.43.28.7.28.28 0 .53-.11.71-.29l2.48-2.48c.18-.18.29-.43.29-.71 0-.28-.11-.53-.29-.71C20.66 4.78 16.54 3 12 3z"/>
            </svg>
            <div className="flex items-center ml-1">
              <div className="w-6 h-3 border border-foreground rounded-sm relative">
                <div className="absolute inset-[2px] right-1 bg-foreground rounded-[1px]" />
              </div>
              <div className="w-[2px] h-[6px] bg-foreground rounded-r-sm -ml-[1px]" />
            </div>
          </div>
        </div>

        {/* Main Content Area */}
        <main className="flex-1 overflow-y-auto bg-background pb-20 relative">
          {children}
        </main>

        {/* Bottom Navigation */}
        <nav className="absolute bottom-0 left-0 right-0 bg-card border-t border-border px-2 pt-2 pb-6 safe-area-bottom">
          <div className="flex justify-around items-center">
            {navItems.map((item) => {
              const isActive = pathname === item.href
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "flex flex-col items-center gap-1 px-3 py-1.5 rounded-xl transition-all duration-200",
                    isActive 
                      ? "text-primary" 
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  <item.icon className={cn("w-5 h-5", isActive && "scale-110")} />
                  <span className="text-[10px] font-medium">{item.label}</span>
                </Link>
              )
            })}
          </div>
        </nav>

        {/* Home Indicator */}
        <div className="absolute bottom-1 left-1/2 -translate-x-1/2 w-32 h-1 bg-foreground/20 rounded-full" />
      </div>
    </div>
  )
}
