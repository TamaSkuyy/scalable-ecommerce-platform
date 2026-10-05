/**
 * POST /api/auth/register — BFF registration.
 *
 * The gateway's register endpoint returns the created user but NO token. To avoid making
 * the shopper type their credentials twice we immediately call the login endpoint and, if
 * it succeeds, plant the same httpOnly cookie the login route uses.
 *
 * When the automatic sign-in fails (for example the account needs email verification) we
 * still return 201 with `sessionEstablished: false` so the UI can send the shopper to
 * `/login` with a helpful message instead of showing a fake error.
 */

import { NextResponse } from "next/server";

import { apiFetchServer } from "@/lib/api-client";
import {
  asTrimmedString,
  errorResponse,
  readJsonObject,
  setSessionCookie,
  validateEmail,
  validateFullName,
  validatePassword,
  validationErrorResponse,
} from "@/lib/bff";
import type { FieldViolation, LoginResponse, RegisterResponse, UserDto } from "@/lib/types";

export const dynamic = "force-dynamic";

const MIN_PASSWORD_LENGTH = 8;

export async function POST(request: Request) {
  try {
    const body = await readJsonObject(request);
    const email = asTrimmedString(body.email).toLowerCase();
    const password = typeof body.password === "string" ? body.password : "";
    const fullName = asTrimmedString(body.fullName);

    const violations: FieldViolation[] = [];
    validateEmail(email, violations);
    validatePassword(password, violations, MIN_PASSWORD_LENGTH);
    validateFullName(fullName, violations);
    if (violations.length) return validationErrorResponse(violations);

    const user = await apiFetchServer<UserDto>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, fullName }),
    });

    // Best-effort auto-login so the shopper lands signed in.
    try {
      const session = await apiFetchServer<LoginResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      const payload: RegisterResponse = { user: session.user ?? user, sessionEstablished: true };
      return setSessionCookie(NextResponse.json(payload, { status: 201 }), session.accessToken, session.expiresInSeconds);
    } catch {
      const payload: RegisterResponse = { user, sessionEstablished: false };
      return NextResponse.json(payload, { status: 201 });
    }
  } catch (error) {
    return errorResponse(error);
  }
}
