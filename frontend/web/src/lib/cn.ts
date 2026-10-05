/**
 * cn.ts — Tiny class-name joiner.
 *
 * Deliberately dependency-free (no `clsx` / `tailwind-merge`): this project keeps its
 * dependency list minimal, and for our component set plain joining is enough.
 */
export type ClassValue = string | number | null | undefined | false | ClassValue[];

export function cn(...values: ClassValue[]): string {
  const out: string[] = [];
  for (const value of values) {
    if (!value) continue;
    if (Array.isArray(value)) {
      const nested = cn(...value);
      if (nested) out.push(nested);
    } else {
      out.push(String(value));
    }
  }
  return out.join(" ");
}
