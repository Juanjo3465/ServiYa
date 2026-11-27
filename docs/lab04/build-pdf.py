#!/usr/bin/env python3
"""Build the Lab 04 submission PDF from README.md + docs/lab04 evidence.

Usage:
    python3 docs/lab04/build-pdf.py [output.pdf]

Default output: ~/Downloads/Lab_04_Security_2026-II_ServiYa.pdf
Requires: node/npx (marked) and google-chrome (headless print).
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT_PDF = pathlib.Path(sys.argv[1]).expanduser() if len(sys.argv) > 1 else \
    pathlib.Path.home() / "Downloads" / "Lab_04_Security_2026-II_ServiYa.pdf"
TMP = pathlib.Path("/tmp/lab04-pdf")
TMP.mkdir(parents=True, exist_ok=True)

readme = (ROOT / "README.md").read_text()
start = readme.find("\n# Lab 04 — Security")
if start < 0:
    raise SystemExit("Lab 04 section not found in README.md")
lab = readme[start:].strip()
baseline = (ROOT / "docs/lab04/evidence-baseline.md").read_text()
after = (ROOT / "docs/lab04/evidence-after.md").read_text()

diagram = """
<div class="diagram">
  <div class="zone untrusted">
    <div class="zone-title">Untrusted network</div>
    <div class="box client">Browser / API client</div>
  </div>
  <div class="arrow"><span>HTTPS :8443<br>TLS 1.2 / 1.3</span>&#8594;<br><small>HTTP :8081 &#8594; 301</small></div>
  <div class="zone edge">
    <div class="zone-title">Edge zone</div>
    <div class="box nginx"><b>frontend &middot; nginx</b><br>:443 TLS termination<br><small>:80 &#8594; 301</small></div>
    <div class="box cert">frontend_certs<br><small>cert + key (runtime)</small></div>
  </div>
  <div class="arrow"><span>HTTP<br>/api/** &middot; /uploads/**<br>WS /api/chat</span>&#8594;</div>
  <div class="zone app">
    <div class="zone-title">Application network (Docker)</div>
    <div class="box gw">api-gateway :8080</div>
    <div class="row">
      <div class="box small">monolith<br>:8080</div>
      <div class="box small">notifications<br>:8082</div>
      <div class="box small">chat<br>:8000</div>
    </div>
  </div>
</div>
<p class="caption">Trust boundary: nginx (everything left of it crosses the untrusted
network; everything right of it runs on a single Docker host).</p>
"""
lab = re.sub(r"```mermaid\n.*?\n```", diagram, lab, flags=re.S)

_m = re.search(r"\| Pull Request \| (\S+) \|", lab)
PR_URL = _m.group(1) if _m else "see repository"

md = (lab + "\n\n---\n\n# Evidence — baseline (before the change)\n\n" + baseline
      + "\n\n---\n\n# Evidence — after the change\n\n" + after)

res = subprocess.run(["npx", "-y", "marked@12"], input=md, text=True,
                     capture_output=True, cwd=TMP)
if res.returncode != 0:
    raise SystemExit("marked failed:\n" + res.stderr[-2000:])
body = res.stdout

cover = """
<section class="cover">
  <div class="kicker">Software Architecture &middot; 2026-II</div>
  <h1 class="cover-title">Laboratory 4 &mdash; Security</h1>
  <h2 class="cover-sub">Secure Channel (TLS at the edge)</h2>
  <table class="meta">
    <tr><th>Project</th><td>ServiYa &mdash; services marketplace (Spring Boot monolith, API gateway, notifications &amp; chat microservices, React frontend)</td></tr>
    <tr><th>Pattern</th><td>Secure Channel</td></tr>
    <tr><th>Team members</th><td>Fabian Andres Rozo Rodriguez<br>Juan Jos&eacute; Sanchez Mora<br>Ana Sofia Rodriguez Neira<br>Cristian David Arcia Quintero</td></tr>
    <tr><th>Repository</th><td>github.com/Juanjo3465/ServiYa</td></tr>
    <tr><th>Branch</th><td>feat/lab04-secure-channel</td></tr>
    <tr><th>Pull Request</th><td>__PR_URL__</td></tr>
    <tr><th>Evidence</th><td>docs/lab04/evidence-baseline.md &middot; docs/lab04/evidence-after.md</td></tr>
  </table>
</section>
"""
cover = cover.replace("__PR_URL__", PR_URL)

css = """
@page { size: A4; margin: 17mm 15mm 18mm 15mm; }
* { box-sizing: border-box; }
body { font-family: "DejaVu Sans", "Segoe UI", Arial, sans-serif; font-size: 9.6pt;
       line-height: 1.42; color: #17202a; margin: 0; }
h1 { font-size: 17pt; color: #0f3d5c; border-bottom: 2px solid #0f3d5c;
     padding-bottom: 3px; margin: 0 0 10px; page-break-before: always; }
h2 { font-size: 12.6pt; color: #14506e; margin-top: 16px; page-break-after: avoid; }
h3 { font-size: 10.8pt; color: #1d5f7d; margin-top: 13px; page-break-after: avoid; }
p, li { orphans: 3; widows: 3; }
code { font-family: "DejaVu Sans Mono", monospace; font-size: 8.4pt;
       background: #f2f4f6; padding: 0 3px; border-radius: 2px; }
pre { background: #f6f8fa; border: 1px solid #d8dee4; border-radius: 4px;
      padding: 7px 9px; overflow: hidden; page-break-inside: avoid; }
pre code { background: none; padding: 0; font-size: 8px; white-space: pre-wrap;
           word-break: break-word; }
table { border-collapse: collapse; width: 100%; margin: 8px 0 12px; font-size: 8.8pt; }
th, td { border: 1px solid #c9d1d9; padding: 4px 6px; vertical-align: top; text-align: left; }
th { background: #eef3f7; }
tr { page-break-inside: avoid; }
blockquote { border-left: 3px solid #b8c4ce; margin: 8px 0; padding: 2px 10px;
             color: #4a5a68; background: #fafbfc; }
a { color: #14506e; text-decoration: none; }
hr { border: 0; border-top: 1px solid #d8dee4; margin: 14px 0; }
.cover { page-break-after: always; padding-top: 22mm; }
.kicker { color: #6b7a88; letter-spacing: .12em; font-size: 9pt; text-transform: uppercase; }
.cover-title { border: 0; page-break-before: avoid; font-size: 27pt; margin: 12px 0 2px; }
.cover-sub { font-size: 16pt; color: #14506e; margin: 0 0 26px; }
table.meta th { width: 26%; background: #eef3f7; }
table.meta td, table.meta th { font-size: 9.6pt; }
.diagram { display: flex; align-items: stretch; gap: 7px; margin: 12px 0 4px;
           page-break-inside: avoid; }
.zone { border: 1px dashed #9fb3c2; border-radius: 5px; padding: 7px 7px 9px;
        background: #fbfcfd; flex: 1; }
.zone-title { font-size: 7.4pt; text-transform: uppercase; letter-spacing: .06em;
              color: #6b7a88; margin-bottom: 6px; }
.zone.untrusted { background: #fdf6f4; border-color: #d9a79d; }
.zone.edge { background: #f3f8fb; border-color: #8fb6ca; }
.box { border: 1px solid #8aa4b5; border-radius: 4px; background: #fff;
       padding: 6px 7px; font-size: 8.2pt; text-align: center; margin-bottom: 5px; }
.zone .box:last-child { margin-bottom: 0; }
.box.small { font-size: 7.6pt; padding: 5px 4px; }
.row { display: flex; gap: 4px; }
.row .box { flex: 1; margin-bottom: 0; }
.arrow { display: flex; flex-direction: column; justify-content: center;
         align-items: center; text-align: center; font-size: 7.2pt; color: #14506e;
         min-width: 62px; }
.arrow span { font-weight: bold; }
.caption { font-size: 8pt; color: #5b6b79; font-style: italic; margin-top: 2px; }
"""

html_file = TMP / "lab04.html"
html_file.write_text(
    f"""<!doctype html><html><head><meta charset="utf-8">
<title>Lab 04 — Security — Secure Channel — ServiYa</title>
<style>{css}</style></head><body>{cover}{body}</body></html>"""
)

OUT_PDF.parent.mkdir(parents=True, exist_ok=True)
subprocess.run([
    "google-chrome", "--headless=new", "--disable-gpu", "--no-sandbox",
    "--no-pdf-header-footer", "--virtual-time-budget=4000",
    f"--print-to-pdf={OUT_PDF}", f"file://{html_file}",
], check=True, capture_output=True)
print("PDF written:", OUT_PDF)
