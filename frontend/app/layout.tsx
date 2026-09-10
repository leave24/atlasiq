import type { Metadata } from "next";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "AtlasIQ",
  description: "Architecture intelligence for engineering teams"
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body style={{ margin: 0, background: "#0b0f14", color: "#f5f7fa" }}>{children}</body>
    </html>
  );
}
