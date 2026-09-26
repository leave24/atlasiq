"use client";
import {ReactNode} from "react";
const items=["Overview","Architecture","Catalog","PR Intelligence","Security","Governance","History","Enterprise","Settings"];
export function Shell({children,active="Architecture"}:{children:ReactNode;active?:string}){return <div className="shell"><aside className="sidebar"><div className="brand"><span className="brandMark">A</span><div><strong>AtlasIQ</strong><small>Architecture Intelligence</small></div></div><nav>{items.map(x=><button key={x} className={x===active?"navItem active":"navItem"}>{x}</button>)}</nav><div className="sidebarFoot"><span className="statusDot"/> QIR connected</div></aside><main className="workspace">{children}</main></div>}
