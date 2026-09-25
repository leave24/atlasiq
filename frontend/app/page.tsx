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
  const visibleNodes=nodes.filter(node=>node.type!=="repository").slice(0,48);
  const groups=[
    {key:"kubernetes",label:"Kubernetes",match:(node:Node)=>node.type.startsWith("kubernetes-")},
    {key:"cicd",label:"CI / CD",match:(node:Node)=>node.type.startsWith("ci-")},
    {key:"containers",label:"Containers",match:(node:Node)=>node.type==="container-image"},
    {key:"terraform",label:"Terraform",match:(node:Node)=>node.type.startsWith("terraform-")},
    {key:"other",label:"Other",match:(_:Node)=>true}
  ];
  const assigned=new Set<string>();
  const buckets=groups.map(group=>{
    const items=visibleNodes.filter(node=>!assigned.has(node.id)&&group.match(node));
    items.forEach(node=>assigned.add(node.id));
    return {...group,items};
  }).filter(group=>group.items.length>0);

  const width=1160;
  const groupGap=24;
  const groupWidth=(width-groupGap*(buckets.length-1))/Math.max(1,buckets.length);
  const nodeWidth=Math.max(150,Math.min(210,groupWidth-32));
  const nodeHeight=58;
  const rowGap=94;
  const maxRows=Math.max(1,...buckets.map(group=>group.items.length));
  const height=110+maxRows*rowGap;
  const positions=buckets.flatMap((group,groupIndex)=>group.items.map((node,rowIndex)=>({
    node,
    x:groupIndex*(groupWidth+groupGap)+groupWidth/2,
    y:92+rowIndex*rowGap,
    groupIndex
  })));
  const pos=new Map(positions.map(item=>[item.node.id,item]));
  const visibleEdges=edges.filter(edge=>edge.relationship!=="contains"&&pos.has(edge.from)&&pos.has(edge.to));
  const connected=new Set(visibleEdges.flatMap(edge=>[edge.from,edge.to]));

  return <section>
    <p style={{opacity:.7,marginTop:-8}}>Grouped by architecture domain. Repository containment edges are hidden to reduce visual noise.</p>
    <div style={{overflowX:"auto",border:"1px solid",borderRadius:10,padding:8}}>
      <svg viewBox={`0 0 ${width} ${height}`} style={{width:"100%",minWidth:900,height:"auto"}} role="img" aria-label="Repository architecture graph">
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="currentColor"/>
          </marker>
        </defs>
        {buckets.map((group,i)=>{
          const x=i*(groupWidth+groupGap);
          return <g key={group.key}>
            <rect x={x+4} y="8" width={groupWidth-8} height={height-16} rx="12" fill="none" stroke="currentColor" opacity=".22"/>
            <text x={x+18} y="34" fontSize="14" fontWeight="700" fill="currentColor">{group.label}</text>
            <text x={x+18} y="52" fontSize="10" fill="currentColor" opacity=".55">{group.items.length} components</text>
          </g>;
        })}
        {visibleEdges.map((edge,i)=>{
          const from=pos.get(edge.from)!; const to=pos.get(edge.to)!;
          const sameGroup=from.groupIndex===to.groupIndex;
          const midY=(from.y+to.y)/2;
          const d=sameGroup
            ? `M ${from.x} ${from.y+nodeHeight/2} C ${from.x+55} ${midY}, ${to.x+55} ${midY}, ${to.x} ${to.y-nodeHeight/2}`
            : `M ${from.x} ${from.y} C ${(from.x+to.x)/2} ${from.y}, ${(from.x+to.x)/2} ${to.y}, ${to.x} ${to.y}`;
          return <g key={`${edge.from}-${edge.to}-${i}`} opacity=".55">
            <path d={d} fill="none" stroke="currentColor" strokeWidth="1.4" markerEnd="url(#arrow)"/>
            <title>{edge.relationship}</title>
          </g>;
        })}
        {positions.map(({node,x,y})=><g key={node.id} onClick={()=>setSelected(node)} style={{cursor:"pointer"}} opacity={connected.has(node.id)?1:.72}>
          <rect x={x-nodeWidth/2} y={y-nodeHeight/2} width={nodeWidth} height={nodeHeight} rx="8" fill="#0b1117" stroke="currentColor"/>
          <text x={x} y={y-7} textAnchor="middle" fontSize="10" fill="currentColor" opacity=".65">{node.type.replace(/^kubernetes-/,"k8s · ").replace(/^ci-/,"ci · ").replace(/^terraform-/,"tf · ")}</text>
          <text x={x} y={y+13} textAnchor="middle" fontSize="12" fontWeight="700" fill="currentColor">{shorten(node.name,22)}</text>
        </g>)}
      </svg>
    </div>
    <p><small>Showing {visibleNodes.length} of {nodes.filter(node=>node.type!=="repository").length} non-repository components. Hover a connection for its relationship; select a node for details.</small></p>
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
