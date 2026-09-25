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
      <RepositoryAnalysisSummary model={model}/>
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

function RepositoryAnalysisSummary({model}:{model:Model}){
  const types=new Set(model.nodes.map(node=>node.type));
  const workflows=model.nodes.filter(node=>node.type==="ci-workflow").length;
  const containers=model.nodes.filter(node=>node.type==="container-image").length;
  const kubernetes=model.nodes.filter(node=>node.type.startsWith("kubernetes-")).length;
  const terraform=model.nodes.filter(node=>node.type.startsWith("terraform-")).length;
  const criticalHigh=model.findings.filter(f=>["CRITICAL","HIGH"].includes(f.severity)).length;
  const technologies=[
    {label:"Kubernetes",value:kubernetes?\`Detected (\${kubernetes} components)\`:"Not detected"},
    {label:"Containers",value:containers?\`Detected (\${containers} images)\`:"Not detected"},
    {label:"CI / CD",value:workflows?\`GitHub Actions (\${workflows} workflows)\`:"Not detected"},
    {label:"Terraform",value:terraform?\`Detected (\${terraform} components)\`:"Not detected"}
  ];
  return <section style={{border:"1px solid",borderRadius:10,padding:18,margin:"0 0 28px"}}>
    <div style={{display:"flex",justifyContent:"space-between",gap:12,flexWrap:"wrap",alignItems:"baseline"}}>
      <div><h2 style={{margin:"0 0 4px"}}>Repository analysis</h2><small>{model.repository} · {model.ref||"default"}</small></div>
      <small>Deterministic QIR summary</small>
    </div>
    <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(190px,1fr))",gap:12,marginTop:16}}>
      {technologies.map(item=><div key={item.label} style={{padding:12,border:"1px solid",borderRadius:8}}><small>{item.label}</small><div style={{fontWeight:700,marginTop:4}}>{item.value}</div></div>)}
    </div>
    <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(150px,1fr))",gap:12,marginTop:12}}>
      <SummaryStat label="Components" value={model.nodes.filter(n=>n.type!=="repository").length}/>
      <SummaryStat label="Relationships" value={model.edges.length}/>
      <SummaryStat label="Findings" value={model.findings.length}/>
      <SummaryStat label="Critical / High" value={criticalHigh}/>
      <SummaryStat label="QIR node types" value={types.size}/>
    </div>
  </section>;
}
function SummaryStat({label,value}:{label:string,value:number}){return <div style={{padding:"10px 12px",border:"1px solid",borderRadius:8}}><strong style={{fontSize:22}}>{value}</strong><div><small>{label}</small></div></div>}

function Metric({label,value}:{label:string,value:number}){return <article style={{border:"1px solid",borderRadius:8,padding:16}}><strong style={{fontSize:28}}>{value}</strong><div>{label}</div></article>}


function ArchitectureGraph({nodes,edges}:{nodes:Node[];edges:Edge[]}){
  const [selected,setSelected]=useState<Node|null>(null);
  const [domain,setDomain]=useState("all");
  const [edgeFilters,setEdgeFilters]=useState<Record<string,boolean>>({routing:true,selectors:true,secrets:true,uses:true,dependencies:false,containment:false,other:true});
  const edgeCategory=(relationship:string)=>{
    if(relationship==="routes_to")return "routing";
    if(relationship==="selects")return "selectors";
    if(relationship==="reads_secret")return "secrets";
    if(relationship==="uses")return "uses";
    if(relationship==="precedes"||relationship==="depends_on")return "dependencies";
    if(relationship==="contains")return "containment";
    return "other";
  };
  const edgeLabels:Record<string,string>={routing:"Routing",selectors:"Selectors",secrets:"Secrets",uses:"Uses",dependencies:"Job dependencies",containment:"Containment",other:"Other"};
  const domainOf=(node:Node)=>node.type.startsWith("kubernetes-")?"kubernetes":node.type.startsWith("ci-")?"cicd":node.type==="container-image"?"containers":node.type.startsWith("terraform-")?"terraform":"other";
  const labels:Record<string,string>={all:"All",kubernetes:"Kubernetes",cicd:"CI / CD",containers:"Containers",terraform:"Terraform",other:"Other"};
  const rank=(node:Node)=>{
    const t=node.type;
    if(t==="kubernetes-ingress")return 0;
    if(t==="kubernetes-service")return 1;
    if(t==="kubernetes-deployment"||t==="kubernetes-stateful-set"||t==="kubernetes-daemon-set")return 2;
    if(t==="kubernetes-secret"||t==="kubernetes-config-map"||t==="kubernetes-service-account")return 3;
    if(t==="ci-workflow")return 0;
    if(t==="ci-job")return 1;
    if(t==="ci-action")return 2;
    if(t==="terraform-provider")return 0;
    if(t==="terraform-module")return 1;
    if(t==="terraform-resource"||t==="terraform-data")return 2;
    return 1;
  };
  const all=nodes.filter(n=>n.type!=="repository");
  const filtered=all.filter(n=>domain==="all"||domainOf(n)===domain).slice(0,48);
  const domainKeys=[...new Set(filtered.map(domainOf))];
  const sectionGap=28, nodeW=210, nodeH=58, rowGap=112, nodeGap=24, sectionPadding=28;
  const sections=domainKeys.map(key=>{
    const items=filtered.filter(n=>domainOf(n)===key).sort((a,b)=>rank(a)-rank(b)||a.name.localeCompare(b.name));
    const ranks=[...new Set(items.map(rank))].sort((a,b)=>a-b);
    const rankRows=new Map(ranks.map((r,i)=>[r,i]));
    const byRank=new Map<number,Node[]>();
    items.forEach(n=>byRank.set(rank(n),[...(byRank.get(rank(n))||[]),n]));
    const maxPeers=Math.max(1,...[...byRank.values()].map(peers=>peers.length));
    const sectionWidth=Math.max(280,sectionPadding*2+maxPeers*nodeW+Math.max(0,maxPeers-1)*nodeGap);
    return {key,items,rankRows,byRank,sectionWidth};
  });
  const maxDepth=Math.max(1,...sections.map(s=>s.rankRows.size));
  const height=120+maxDepth*rowGap;
  const offsets:number[]=[];
  let cursor=0;
  sections.forEach(section=>{offsets.push(cursor);cursor+=section.sectionWidth+sectionGap;});
  const width=Math.max(1160,cursor-Math.min(sectionGap,cursor));
  const positions=sections.flatMap((section,si)=>section.items.map(node=>{
    const r=rank(node), peers=section.byRank.get(r)||[], peer=peers.findIndex(n=>n.id===node.id);
    const rowWidth=peers.length*nodeW+Math.max(0,peers.length-1)*nodeGap;
    const rowStart=offsets[si]+(section.sectionWidth-rowWidth)/2+nodeW/2;
    return {node,x:rowStart+peer*(nodeW+nodeGap),y:96+(section.rankRows.get(r)||0)*rowGap,section:si};
  }));
  const pos=new Map(positions.map(p=>[p.node.id,p]));
  const visibleEdges=edges.filter(edge=>edgeFilters[edgeCategory(edge.relationship)]!==false&&pos.has(edge.from)&&pos.has(edge.to));
  const domains=[...new Set(all.map(domainOf))];
  const selectedEdges=selected?visibleEdges.filter(edge=>edge.from===selected.id||edge.to===selected.id):[];
  const relatedIds=new Set(selected?[selected.id,...selectedEdges.flatMap(edge=>[edge.from,edge.to])]:[]);
  const incoming=selectedEdges.filter(edge=>edge.to===selected?.id);
  const outgoing=selectedEdges.filter(edge=>edge.from===selected?.id);
  const resetView=()=>{setSelected(null);setDomain("all");setEdgeFilters({routing:true,selectors:true,secrets:true,uses:true,dependencies:false,containment:false,other:true});};

  return <section>
    <div style={{display:"flex",gap:8,flexWrap:"wrap",alignItems:"center",margin:"0 0 14px"}}>
      {["all",...domains].map(key=><button key={key} onClick={()=>{setDomain(key);setSelected(null)}} style={{padding:"8px 12px",borderRadius:18,border:"1px solid",fontWeight:domain===key?700:400}}>{labels[key]}</button>)}
      <button onClick={resetView} style={{marginLeft:"auto",padding:"8px 12px",borderRadius:18,border:"1px solid"}}>Reset view</button>
    </div>
    <div style={{display:"flex",gap:12,flexWrap:"wrap",alignItems:"center",margin:"0 0 14px",padding:"10px 12px",border:"1px solid",borderRadius:8}}>
      <strong style={{fontSize:13}}>Relationships</strong>
      {Object.keys(edgeLabels).map(key=><label key={key} style={{display:"flex",gap:5,alignItems:"center",fontSize:13}}>
        <input type="checkbox" checked={edgeFilters[key]!==false} onChange={e=>setEdgeFilters(current=>({...current,[key]:e.target.checked}))}/>
        {edgeLabels[key]}
      </label>)}
    </div>
    <div style={{overflowX:"auto",border:"1px solid",borderRadius:10,padding:8}}>
      <svg viewBox={`0 0 ${width} ${height}`} style={{width:Math.max(900,width),height:"auto",maxWidth:"none"}} role="img" aria-label="Semantic repository architecture graph">
        <defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto"><path d="M0 0 L10 5 L0 10z" fill="currentColor"/></marker></defs>
        {sections.map((section,i)=>{const x=offsets[i];return <g key={section.key}><rect x={x+4} y="8" width={section.sectionWidth-8} height={height-16} rx="12" fill="none" stroke="currentColor" opacity=".2"/><text x={x+18} y="34" fontSize="14" fontWeight="700" fill="currentColor">{labels[section.key]}</text><text x={x+18} y="52" fontSize="10" fill="currentColor" opacity=".55">{section.items.length} components</text></g>})}
        {visibleEdges.map((edge,i)=>{const a=pos.get(edge.from)!,b=pos.get(edge.to)!;const mid=(a.y+b.y)/2;const category=edgeCategory(edge.relationship);const d=`M ${a.x} ${a.y+nodeH/2} C ${a.x} ${mid}, ${b.x} ${mid}, ${b.x} ${b.y-nodeH/2}`;const dash=category==="dependencies"?"7 5":category==="containment"?"2 6":undefined;const opacity=category==="containment"?.18:category==="dependencies"?.4:.65;const focused=!selected||edge.from===selected.id||edge.to===selected.id;return <g key={i} opacity={focused?opacity:.08}><path d={d} fill="none" stroke="currentColor" strokeWidth={focused&&selected?2.4:category==="routing"?2:1.4} strokeDasharray={dash} markerEnd="url(#arrow)"/><title>{edge.relationship} · {edge.from} → {edge.to}</title></g>})}
        {positions.map(({node,x,y})=>{const focused=!selected||relatedIds.has(node.id);const active=selected?.id===node.id;return <g key={node.id} onClick={()=>setSelected(active?null:node)} style={{cursor:"pointer"}} opacity={focused?1:.18}><rect x={x-nodeW/2} y={y-nodeH/2} width={nodeW} height={nodeH} rx="8" fill="#0b1117" stroke="currentColor" strokeWidth={active?3:1}/><text x={x} y={y-7} textAnchor="middle" fontSize="10" fill="currentColor" opacity=".65">{node.type.replace(/^kubernetes-/,"k8s · ").replace(/^ci-/,"ci · ").replace(/^terraform-/,"tf · ")}</text><text x={x} y={y+13} textAnchor="middle" fontSize="12" fontWeight="700" fill="currentColor">{shorten(displayName(node),24)}</text></g>})}
      </svg>
    </div>
    <div style={{display:"flex",gap:16,flexWrap:"wrap",marginTop:10,fontSize:12,opacity:.72}}>
      <span>━━ structural / runtime</span><span>┄┄ job dependency</span><span>···· containment</span>
    </div>
    <p><small>Semantic layout: infrastructure entry points and workflows appear above their dependants. Showing {filtered.length} of {all.length} components and {visibleEdges.length} filtered relationships.</small></p>
    {selected&&<aside style={{border:"1px solid",borderRadius:8,padding:16,marginTop:12}}>
      <div style={{display:"flex",justifyContent:"space-between",gap:12,alignItems:"center"}}><strong>{displayName(selected)}</strong><button onClick={()=>setSelected(null)} style={{padding:"6px 10px"}}>Clear focus</button></div>
      <p><small>{selected.type} · {selected.source}</small></p><code style={{wordBreak:"break-all"}}>{selected.id}</code>
      <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(260px,1fr))",gap:12,marginTop:14}}>
        <div><strong>Incoming ({incoming.length})</strong>{incoming.length===0?<p><small>None in current filters.</small></p>:<ul>{incoming.map((edge,i)=><li key={i}><code>{edge.relationship}</code> ← {displayName(nodes.find(n=>n.id===edge.from)||{id:edge.from,type:"",name:edge.from,source:""})}</li>)}</ul>}</div>
        <div><strong>Outgoing ({outgoing.length})</strong>{outgoing.length===0?<p><small>None in current filters.</small></p>:<ul>{outgoing.map((edge,i)=><li key={i}><code>{edge.relationship}</code> → {displayName(nodes.find(n=>n.id===edge.to)||{id:edge.to,type:"",name:edge.to,source:""})}</li>)}</ul>}</div>
      </div>
      {selected.metadata&&Object.keys(selected.metadata).length>0&&<pre style={{whiteSpace:"pre-wrap",overflowX:"auto"}}>{JSON.stringify(selected.metadata,null,2)}</pre>}
    </aside>}
  </section>;
}

function displayName(node:Node){
  if(node.type==="container-image"){
    const path=node.id.replace(/^docker:/,"");
    return path&&path!=="Dockerfile"?path:node.source||node.name;
  }
  return node.name;
}

function shorten(value:string,max:number){return value.length<=max?value:`${value.slice(0,max-1)}…`;}
