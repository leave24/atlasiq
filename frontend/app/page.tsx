export default function Home() {
  return (
    <main style={{ fontFamily: "system-ui", padding: "3rem", maxWidth: 960, margin: "0 auto" }}>
      <p style={{ textTransform: "uppercase", letterSpacing: ".12em", opacity: .65 }}>Project AtlasIQ</p>
      <h1>Architecture Intelligence for engineering teams</h1>
      <p>
        Connect a repository, reconstruct its architecture, inspect CI/CD and infrastructure,
        and surface DevSecOps findings from a single model.
      </p>
      <section style={{ marginTop: "2rem", display: "grid", gap: "1rem" }}>
        <article><strong>Architecture Map</strong><br/>QIR-based graph of services and dependencies.</article>
        <article><strong>Infrastructure & CI/CD</strong><br/>Docker, Kubernetes, Terraform and GitHub Actions.</article>
        <article><strong>DevSecOps Findings</strong><br/>Deterministic rules before AI recommendations.</article>
      </section>
    </main>
  );
}
