/**
 * POST /api/auth/login — BFF login.
 *
 * ── THE IMPORTANT PART ────────────────────────────────────────────────────────
 * The gateway answers with `{ accessToken, tokenType, expiresInSeconds, user }`.
 * We take the `accessToken`, put it in an httpOnly cookie, and return ONLY `{ user }`
 * to the browser. The JWT is therefore unreachable from JavaScript — no `localStorage`,
 * no readable cookie, nothing for an XSS payload to steal.
 *
 *   Browser ──POST /api/auth/login──▶ Next.js route handler ──POST /api/v1/auth/login──▶ gateway
 *              { email, password }                              { accessToken, user }
 *                    ◀── { user } + Set-Cookie: shopflow_token=<jwt>; HttpOnly ──
 */

import { NextResponse } from "next/server";

import { apiFetchServer } from "@/lib/api-client";
import {
  asTrimmedString,
  errorResponse,
  readJsonObject,
  setSessionCookie,
  validateEmail,
  validatePassword,
  validationErrorResponse,
} from "@/lib/bff";
import type { FieldViolation, LoginResponse } from "@/lib/types";

// Route handlers that touch auth are always dynamic.
export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  try {
    const body = await readJsonObject(request);
    const email = asTrimmedString(body.email).toLowerCase();
    const password = typeof body.password === "string" ? body.password : "";

    // Cheap client-facing validation first, so an obvious typo does not cost a round trip.
    const violations: FieldViolation[] = [];
    validateEmail(email, violations);
    validatePassword(password, violations, 1); // login: only require "not empty"
    if (violations.length) return validationErrorResponse(violations);

    const session = await apiFetchServer<LoginResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });

    const response = NextResponse.json({ user: session.user }, { status: 200 });
    return setSessionCookie(response, session.accessToken, session.expiresInSeconds);
  } catch (error) {
    return errorResponse(error);
  }
}
