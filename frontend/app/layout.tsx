import type {Metadata} from "next";import type {ReactNode} from "react";import "./globals.css";
export const metadata:Metadata={title:"AtlasIQ · Architecture Intelligence",description:"Architecture intelligence for engineering teams"};
export default function RootLayout({children}:{children:ReactNode}){return <html lang="en"><body>{children}</body></html>}