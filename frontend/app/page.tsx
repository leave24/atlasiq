"use client";

import { FormEvent, useState } from "react";

type Node = { id: string; type: string; name: string; source: string; metadata?: Record<string, unknown> };
type Edge = { from: string; to: string; relationship: string };
type Finding = { id: string; severity: string; category: string; resourceId: string; title: string; recommendation: string };
type Model = { repository: string; ref: string; nodes: Node[]; edges: Edge[]; findings: Finding[] };

export default function Home() {
  const [repository,setRepository]=useState("https://github.com/leave24/infranauta");
  const [ref,setRef]=useState("");
  const [model,setModel]=useState<Model|null>(null);
  const [error,setError]=useState("");
  const [loading,setLoading]=useState(false);

  async function analyze(e:FormEvent){
    e.preventDefault(); setLoading(true); setError(""); setModel(null);
    try{
      const response=await fetch("/api/scans",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({repository,ref})});
      if(!response.ok){ const body=await response.text(); throw new Error(body.includes("repository workspace does not exist")?"Repository not found. Enter a public GitHub HTTPS URL or an existing local workspace.":body || "Analysis failed"); }
      setModel(await response.json());
    }catch(err){setError(err instanceof Error?err.message:"Analysis failed");}
    finally{setLoading(false);}
  }

  return <main style={{fontFamily:"system-ui",padding:"2rem",maxWidth:1200,margin:"0 auto"}}>
    <p style={{textTransform:"uppercase",letterSpacing:".12em",opacity:.6}}>AtlasIQ · MVP</p>
    <h1>Architecture Intelligence</h1>
    <p>Analyze a public GitHub repository or local workspace and reconstruct Kubernetes, Docker, Terraform and GitHub Actions into one QIR model.</p>

    <form onSubmit={analyze} style={{display:"flex",gap:12,flexWrap:"wrap",margin:"2rem 0"}}>
      <input aria-label="Repository" value={repository} onChange={e=>setRepository(e.target.value)} placeholder="https://github.com/owner/repository" required style={{padding:12,minWidth:260}}/>
      <input aria-label="Ref" value={ref} onChange={e=>setRef(e.target.value)} placeholder="Ref (optional)" style={{padding:12,minWidth:160}}/>
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
      <h2>Architecture graph</h2>
      <ArchitectureGraph nodes={model.nodes} edges={model.edges}/>

      <h2 style={{marginTop:32}}>Relationships</h2>
      <div style={{overflowX:"auto"}}><table style={{width:"100%",borderCollapse:"collapse"}}><thead><tr><th>From</th><th>Relationship</th><th>To</th></tr></thead>
        <tbody>{model.edges.map((e,i)=><tr key={i}><td>{e.from}</td><td>{e.relationship}</td><td>{e.to}</td></tr>)}</tbody></table></div>
      <h2 style={{marginTop:32}}>DevSecOps findings</h2>
      {model.findings.length===0?<p>No findings detected.</p>:model.findings.map(f=><article key={f.id} style={{border:"1px solid",padding:12,marginBottom:10}}>
        <strong>{f.severity} · {f.title}</strong><p>{f.resourceId}</p><small>{f.recommendation}</small>
      </article>)}
    </>}
  </main>;
}
function Metric({label,value}:{label:string,value:number}){return <article style={{border:"1px solid",borderRadius:8,padding:16}}><strong style={{fontSize:28}}>{value}</strong><div>{label}</div></article>}


function ArchitectureGraph({nodes,edges}:{nodes:Node[];edges:Edge[]}){
  const [selected,setSelected]=useState<Node|null>(null);
  const visibleNodes=nodes.slice(0,40);
  const index=new Map(visibleNodes.map((node,i)=>[node.id,i]));
  const width=1100;
  const columns=Math.max(1,Math.min(5,Math.ceil(Math.sqrt(visibleNodes.length))));
  const rows=Math.max(1,Math.ceil(visibleNodes.length/columns));
  const cellW=width/columns;
  const cellH=150;
  const height=Math.max(220,rows*cellH);
  const positions=visibleNodes.map((node,i)=>({
    node,
    x:(i%columns)*cellW+cellW/2,
    y:Math.floor(i/columns)*cellH+70
  }));
  const pos=new Map(positions.map(p=>[p.node.id,p]));
  const visibleEdges=edges.filter(edge=>index.has(edge.from)&&index.has(edge.to));

  return <section>
    <div style={{overflowX:"auto",border:"1px solid",borderRadius:10,padding:8}}>
      <svg viewBox={`0 0 ${width} ${height}`} style={{width:"100%",minWidth:760,height:"auto"}} role="img" aria-label="Repository architecture graph">
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor"/>
          </marker>
        </defs>
        {visibleEdges.map((edge,i)=>{
          const from=pos.get(edge.from)!; const to=pos.get(edge.to)!;
          return <g key={`${edge.from}-${edge.to}-${i}`} opacity=".45">
            <line x1={from.x} y1={from.y} x2={to.x} y2={to.y} stroke="currentColor" strokeWidth="1.5" markerEnd="url(#arrow)"/>
            <text x={(from.x+to.x)/2} y={(from.y+to.y)/2-4} textAnchor="middle" fontSize="10" fill="currentColor">{edge.relationship}</text>
          </g>;
        })}
        {positions.map(({node,x,y})=><g key={node.id} onClick={()=>setSelected(node)} style={{cursor:"pointer"}}>
          <rect x={x-90} y={y-30} width="180" height="60" rx="8" fill="#0b1117" stroke="currentColor"/>
          <text x={x} y={y-7} textAnchor="middle" fontSize="10" fill="currentColor" opacity=".7">{node.type}</text>
          <text x={x} y={y+13} textAnchor="middle" fontSize="12" fontWeight="700" fill="currentColor">{shorten(node.name,24)}</text>
        </g>)}
      </svg>
    </div>
    {nodes.length>40&&<p><small>Showing the first 40 components to keep the MVP graph readable.</small></p>}
    {selected&&<aside style={{border:"1px solid",borderRadius:8,padding:16,marginTop:12}}>
      <strong>{selected.name}</strong>
      <p><small>{selected.type} · {selected.source}</small></p>
      <code style={{wordBreak:"break-all"}}>{selected.id}</code>
      {selected.metadata&&Object.keys(selected.metadata).length>0&&<pre style={{whiteSpace:"pre-wrap",overflowX:"auto"}}>{JSON.stringify(selected.metadata,null,2)}</pre>}
      <button onClick={()=>setSelected(null)} style={{padding:"8px 12px"}}>Close details</button>
    </aside>}
  </section>;
}

function shorten(value:string,max:number){return value.length<=max?value:`${value.slice(0,max-1)}…`;}
