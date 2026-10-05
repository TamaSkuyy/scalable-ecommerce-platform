/**
 * bff.ts — Shared helpers for the BFF route handlers under `src/app/api/auth/*`.
 *
 * These handlers are the ONLY place in the app where a JWT is written to a cookie.
 * Centralising the cookie options here means we cannot accidentally ship a variant that
 * forgets `httpOnly` or `sameSite`.
 *
 * TEACHING POINT — the cookie flags:
 *   httpOnly : JavaScript (`document.cookie`) cannot read it, so an XSS bug cannot steal
 *              the token. This is the whole reason we do BFF instead of localStorage.
 *   sameSite : 'lax' blocks the cookie from being sent on cross-site POSTs (CSRF defence)
 *              while still allowing a normal top-level navigation to keep the session.
 *   secure   : only over HTTPS — enabled automatically in production.
 *   path     : '/' so both the pages and the `/api/v1` proxy see it.
 *   maxAge   : mirrors `expiresInSeconds` from the gateway, so the cookie dies with the JWT.
 */

import { NextResponse } from "next/server";

import { ApiError, isBackendUnreachable, toApiError } from "./api-error";
import { TOKEN_COOKIE } from "./config";
import type { ErrorEnvelope, FieldViolation } from "./types";

/** Options shared by login/register when planting the session cookie. */
export function sessionCookieOptions(maxAgeSeconds: number) {
  return {
    name: TOKEN_COOKIE,
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: Math.max(1, Math.floor(maxAgeSeconds)),
  };
}

/** Writes the JWT into an httpOnly cookie on an existing response. */
export function setSessionCookie(response: NextResponse, accessToken: string, expiresInSeconds: number): NextResponse {
  response.cookies.set({ ...sessionCookieOptions(expiresInSeconds), value: accessToken });
  return response;
}

/** Clears the session cookie (logout, or a 401 from the gateway). */
export function clearSessionCookie(response: NextResponse): NextResponse {
  response.cookies.set({
    name: TOKEN_COOKIE,
    value: "",
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 0,
  });
  return response;
}

/**
 * Turns any thrown value into a JSON error response that matches the gateway's envelope,
 * so the client wrapper (`src/lib/http.ts`) can parse it exactly the same way.
 */
export function errorResponse(error: unknown): NextResponse {
  const apiError = toApiError(error);

  // The gateway is down: answer 503 with a message the shopper can act on.
  if (isBackendUnreachable(apiError)) {
    return NextResponse.json(
      buildEnvelope(503, "Service Unavailable", apiError.message || "API gateway tidak dapat dihubungi."),
      { status: 503 },
    );
  }

  const status = apiError.status && apiError.status >= 400 ? apiError.status : 500;
  const envelope: ErrorEnvelope = {
    ...buildEnvelope(status, apiError.envelope?.error ?? httpStatusText(status), apiError.message),
    ...(apiError.violations.length ? { violations: apiError.violations } : {}),
    ...(apiError.traceId ? { traceId: apiError.traceId } : {}),
  };
  return NextResponse.json(envelope, { status });
}

/** A 400 validation response with field-level `violations`. */
export function validationErrorResponse(violations: FieldViolation[]): NextResponse {
  return NextResponse.json(
    {
      ...buildEnvelope(400, "Bad Request", violations[0]?.message ?? "Data yang dikirim tidak valid."),
      violations,
    },
    { status: 400 },
  );
}

function buildEnvelope(status: number, error: string, message: string): ErrorEnvelope {
  return {
    timestamp: new Date().toISOString(),
    status,
    error,
    message,
    path: "/api/auth",
  };
}

function httpStatusText(status: number): string {
  const map: Record<number, string> = {
    400: "Bad Request",
    401: "Unauthorized",
    403: "Forbidden",
    404: "Not Found",
    409: "Conflict",
    422: "Unprocessable Entity",
    429: "Too Many Requests",
    500: "Internal Server Error",
  };
  return map[status] ?? "Error";
}

/**
 * Parses a JSON request body defensively.
 * Throws an `ApiError(400)` when the payload is not a JSON object.
 */
export async function readJsonObject(request: Request): Promise<Record<string, unknown>> {
  let raw: string;
  try {
    raw = await request.text();
  } catch {
    throw new ApiError("Tidak dapat membaca data permintaan.", 400);
  }
  if (!raw.trim()) return {};
  try {
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
      throw new ApiError("Body permintaan harus berupa objek JSON.", 400);
    }
    return parsed as Record<string, unknown>;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError("Body permintaan bukan JSON yang valid.", 400);
  }
}

/* -------------------------------------------------------------------------- */
/* Minimal server-side validation (the gateway validates again — this is UX)  */
/* -------------------------------------------------------------------------- */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export function asTrimmedString(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

export function validateEmail(email: string, violations: FieldViolation[]): void {
  if (!email) {
    violations.push({ field: "email", message: "Email wajib diisi." });
  } else if (!EMAIL_PATTERN.test(email)) {
    violations.push({ field: "email", message: "Format email tidak valid." });
  }
}

export function validatePassword(password: string, violations: FieldViolation[], minLength = 8): void {
  if (!password) {
    violations.push({ field: "password", message: "Password wajib diisi." });
  } else if (password.length < minLength) {
    violations.push({ field: "password", message: `Password minimal ${minLength} karakter.` });
  }
}

export function validateFullName(fullName: string, violations: FieldViolation[]): void {
  if (!fullName) {
    violations.push({ field: "fullName", message: "Nama lengkap wajib diisi." });
  } else if (fullName.length < 3) {
    violations.push({ field: "fullName", message: "Nama lengkap minimal 3 karakter." });
  }
}
