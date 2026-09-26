export type QirNode={id:string;type:string;name:string;source:string;metadata?:Record<string,unknown>};
export type QirEdge={from:string;to:string;relationship:string};
export type Finding={id:string;severity:string;category:string;resourceId:string;title:string;recommendation:string};
export type QirModel={repository:string;ref:string;nodes:QirNode[];edges:QirEdge[];findings:Finding[]};
export const domainOf=(n:QirNode)=>n.type.startsWith("kubernetes-")?"kubernetes":n.type.startsWith("ci-")?"cicd":n.type==="container-image"?"containers":n.type.startsWith("terraform-")?"terraform":n.type.includes("database")?"data":n.type.includes("api")?"api":"other";
export const displayName=(n:QirNode)=>n.type==="container-image"?(n.id.replace(/^docker:/,"")||n.source||n.name):n.name;
export const severityRank=(s:string)=>({CRITICAL:4,HIGH:3,MEDIUM:2,LOW:1}[s.toUpperCase()]||0);
