/**
 * BackendOfflineNotice.tsx — Shown whenever a page renders data from `src/lib/demo-data.ts`.
 *
 * This is the visible half of the "demo fallback": the catalog never crashes when the
 * gateway is down, but the shopper is always told that the data is not real. Being honest
 * about degraded state is part of the UX, not a nice-to-have.
 */

import { API_BASE_URL } from "@/lib/config";
import { BACKEND_OFFLINE_NOTICE, BACKEND_OFFLINE_TITLE } from "@/lib/demo-data";
import { Callout } from "@/components/ui/Callout";

export function BackendOfflineNotice({ className, compact = false }: { className?: string; compact?: boolean }) {
  return (
    <Callout tone="warning" title={BACKEND_OFFLINE_TITLE} className={className}>
      <p>{BACKEND_OFFLINE_NOTICE}</p>
      {compact ? null : (
        <p className="mt-1.5 text-xs text-foreground/60">
          Alamat gateway yang dicoba: <code className="rounded bg-surface-muted px-1.5 py-0.5">{API_BASE_URL}</code>
        </p>
      )}
    </Callout>
  );
}
