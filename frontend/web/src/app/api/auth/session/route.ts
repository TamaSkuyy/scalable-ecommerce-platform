/**
 * GET /api/auth/session — "who am I?" for client components.
 *
 * The header uses this to decide between "Masuk / Daftar" and the shopper's name.
 * It NEVER returns a token; it only proxies `GET /api/v1/users/me` and hands back the
 * safe user projection.
 *
 * Anonymous visitors get `200 { user: null }` rather than a 401, because "not signed in"
 * is a perfectly normal state for a storefront, not an error. If the gateway is down we
 * also answer `{ user: null, offline: true }` so the header can show a subtle hint instead
 * of a broken state.
 */

import { NextResponse } from "next/server";

import { apiFetchServer, getAuthToken } from "@/lib/api-client";
import { clearSessionCookie } from "@/lib/bff";
import { isBackendUnreachable, toApiError } from "@/lib/api-error";
import type { SessionResponse, UserDto } from "@/lib/types";

export const dynamic = "force-dynamic";

export async function GET() {
  const token = await getAuthToken();
  if (!token) {
    const payload: SessionResponse = { user: null };
    return NextResponse.json(payload, { status: 200 });
  }

  try {
    const user = await apiFetchServer<UserDto>("/users/me", { auth: true, cache: "no-store" });
    const payload: SessionResponse = { user };
    return NextResponse.json(payload, { status: 200 });
  } catch (error) {
    const apiError = toApiError(error);

    // Expired/revoked token: clean the cookie so the shopper is not stuck in a loop.
    if (apiError.isUnauthorized) {
      const payload: SessionResponse = { user: null };
      return clearSessionCookie(NextResponse.json(payload, { status: 200 }));
    }

    // Gateway unreachable: degrade quietly.
    if (isBackendUnreachable(apiError)) {
      const payload: SessionResponse = { user: null, offline: true };
      return NextResponse.json(payload, { status: 200 });
    }

    const payload: SessionResponse = { user: null };
    return NextResponse.json(payload, { status: 200 });
  }
}
