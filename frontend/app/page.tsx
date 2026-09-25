"use client";

import { FormEvent, useState } from "react";

type Node = { id: string; type: string; name: string; source: string; metadata?: Record<string, unknown> };
type Edge = { from: string; to: string; relationship: string };
type Finding = { id: string; severity: string; category: string; target: string; title: string; recommendation: string };
type Model = { repository: string; ref: string; nodes: Node[]; edges: Edge[]; findings: Finding[] };

export default function Home() {
  const [repository,setRepository]=useState("demo");
  const [ref,setRef]=useState("local");
  const [model,setModel]=useState<Model|null>(null);
  const [error,setError]=useState("");
  const [loading,setLoading]=useState(false);

  async function analyze(e:FormEvent){
    e.preventDefault(); setLoading(true); setError(""); setModel(null);
    try{
      const response=await fetch("/api/scans",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({repository,ref})});
      if(!response.ok) throw new Error(await response.text() || "Analysis failed");
      setModel(await response.json());
    }catch(err){setError(err instanceof Error?err.message:"Analysis failed");}
    finally{setLoading(false);}
  }

  return <main style={{fontFamily:"system-ui",padding:"2rem",maxWidth:1200,margin:"0 auto"}}>
    <p style={{textTransform:"uppercase",letterSpacing:".12em",opacity:.6}}>AtlasIQ · MVP</p>
    <h1>Architecture Intelligence</h1>
    <p>Analyze a repository workspace and reconstruct Kubernetes, Docker, Terraform and GitHub Actions into one QIR model.</p>

    <form onSubmit={analyze} style={{display:"flex",gap:12,flexWrap:"wrap",margin:"2rem 0"}}>
      <input aria-label="Repository" value={repository} onChange={e=>setRepository(e.target.value)} placeholder="Repository workspace" required style={{padding:12,minWidth:260}}/>
      <input aria-label="Ref" value={ref} onChange={e=>setRef(e.target.value)} placeholder="Ref" required style={{padding:12,minWidth:160}}/>
      <button disabled={loading} style={{padding:"12px 20px"}}>{loading?"Analyzing…":"Analyze repository"}</button>
    </form>
    {error && <pre style={{whiteSpace:"pre-wrap",padding:16,border:"1px solid"}}>{error}</pre>}

    {model && <>
      <section style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(180px,1fr))",gap:12,marginBottom:24}}>
        <Metric label="Components" value={model.nodes.length}/>
        <Metric label="Relationships" value={model.edges.length}/>
        <Metric label="Findings" value={model.findings.length}/>
        <Metric label="Critical / High" value={model.findings.filter(f=>["CRITICAL","HIGH"].includes(f.severity)).length}/>
      </section>
      <h2>Architecture map</h2>
      <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(220px,1fr))",gap:10}}>
        {model.nodes.map(n=><article key={n.id} style={{border:"1px solid",borderRadius:8,padding:12}}>
          <small>{n.type}</small><strong style={{display:"block",marginTop:4}}>{n.name}</strong><small>{n.source}</small>
        </article>)}
      </div>
      <h2 style={{marginTop:32}}>Relationships</h2>
      <div style={{overflowX:"auto"}}><table style={{width:"100%",borderCollapse:"collapse"}}><thead><tr><th>From</th><th>Relationship</th><th>To</th></tr></thead>
        <tbody>{model.edges.map((e,i)=><tr key={i}><td>{e.from}</td><td>{e.relationship}</td><td>{e.to}</td></tr>)}</tbody></table></div>
      <h2 style={{marginTop:32}}>DevSecOps findings</h2>
      {model.findings.length===0?<p>No findings detected.</p>:model.findings.map(f=><article key={f.id} style={{border:"1px solid",padding:12,marginBottom:10}}>
        <strong>{f.severity} · {f.title}</strong><p>{f.target}</p><small>{f.recommendation}</small>
      </article>)}
    </>}
  </main>;
}
function Metric({label,value}:{label:string,value:number}){return <article style={{border:"1px solid",borderRadius:8,padding:16}}><strong style={{fontSize:28}}>{value}</strong><div>{label}</div></article>}
