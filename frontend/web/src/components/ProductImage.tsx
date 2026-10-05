"use client";

/**
 * ProductImage.tsx — One place where product imagery is rendered.
 *
 * It IS a Client Component, but only for one small reason: `onError`. When an image URL
 * from the gateway 404s (or the demo SVG is missing) we swap in a neutral placeholder
 * instead of the browser's broken-image glyph. Dropping that detail would let the whole
 * component be a Server Component.
 *
 * `next/image` is configured with `unoptimized: true` in `next.config.ts`, which is the
 * right trade-off for this learning project: product images come from arbitrary hosts, and
 * the demo data is local SVG. Nothing needs the optimizer — or a running `sharp` binary.
 */

import { useState } from "react";
import Image from "next/image";

import { cn } from "@/lib/cn";

export interface ProductImageProps {
  src: string | null | undefined;
  alt: string;
  /** Rendered when the image is missing or fails to load. Usually the product initial. */
  fallbackLabel?: string;
  sizes?: string;
  priority?: boolean;
  className?: string;
}

export function ProductImage({ src, alt, fallbackLabel, sizes, priority, className }: ProductImageProps) {
  const [failed, setFailed] = useState(false);
  const url = src?.trim();
  const showPlaceholder = !url || failed;
  const initial = (fallbackLabel ?? alt).trim().charAt(0).toUpperCase() || "?";

  return (
    <div className={cn("relative flex items-center justify-center overflow-hidden bg-surface-muted", className)}>
      {showPlaceholder ? (
        <span aria-hidden className="select-none text-3xl font-semibold text-foreground/25">
          {initial}
        </span>
      ) : (
        <Image
          src={url}
          alt={alt}
          fill
          sizes={sizes}
          priority={priority}
          unoptimized
          onError={() => setFailed(true)}
          className="object-cover transition-transform duration-300 group-hover:scale-[1.03]"
        />
      )}
    </div>
  );
}
