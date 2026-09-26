import {ReactNode} from "react";
export function Button({children,...props}:{children:ReactNode}&React.ButtonHTMLAttributes<HTMLButtonElement>){return <button {...props} style={{padding:"9px 13px",border:"1px solid",borderRadius:8,...props.style}}>{children}</button>}
export function Card({children}:{children:ReactNode}){return <section style={{border:"1px solid",borderRadius:10,padding:16}}>{children}</section>}
export function Metric({label,value}:{label:string;value:number}){return <Card><strong style={{fontSize:28}}>{value}</strong><div>{label}</div></Card>}
