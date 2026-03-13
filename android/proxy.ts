import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

// Routes that require authentication
const PROTECTED_ROUTES = ["/", "/add", "/library", "/calendar", "/analytics", "/settings"]

// Routes that should redirect to / if already authenticated
const AUTH_ROUTES = ["/login"]

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Check for JWT token in cookies (set server-side) or rely on client-side guards.
  // Since the token is in localStorage (client-side only), we check a custom cookie
  // that the auth-context can optionally set, or fall back to client-side AuthGuard.
  // This middleware handles the cookie-based token check for SSR routes.
  const token = request.cookies.get("auth_token")?.value

  const isProtectedRoute = PROTECTED_ROUTES.some(
    (route) => pathname === route || (route !== "/" && pathname.startsWith(route))
  )
  const isAuthRoute = AUTH_ROUTES.includes(pathname)

  // Redirect unauthenticated users from protected routes to /login
  if (isProtectedRoute && !token) {
    const loginUrl = new URL("/login", request.url)
    loginUrl.searchParams.set("from", pathname)
    return NextResponse.redirect(loginUrl)
  }

  // Redirect authenticated users away from login page to dashboard
  if (isAuthRoute && token) {
    return NextResponse.redirect(new URL("/", request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    /*
     * Match all routes except:
     * - _next/static (static files)
     * - _next/image (image optimization)
     * - favicon.ico, icons, etc.
     * - api routes
     */
    "/((?!_next/static|_next/image|favicon.ico|.*\\.png|.*\\.svg|.*\\.ico).*)",
  ],
}

