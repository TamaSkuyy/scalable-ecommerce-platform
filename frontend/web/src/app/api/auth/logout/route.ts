/**
 * POST /api/auth/logout — BFF logout.
 *
 * Deleting the cookie IS the logout: the JWT is stateless, so as soon as the browser stops
 * sending it the shopper is anonymous. (A production system would also add the token to a
 * server-side denylist so it cannot be replayed before it expires.)
 *
 * Answers 204 No Content — there is nothing meaningful to return.
 */

import { NextResponse } from "next/server";

import { clearSessionCookie } from "@/lib/bff";

export const dynamic = "force-dynamic";

export async function POST() {
  return clearSessionCookie(new NextResponse(null, { status: 204 }));
}
