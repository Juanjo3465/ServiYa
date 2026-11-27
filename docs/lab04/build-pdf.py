#!/usr/bin/env python3
"""Build the Lab 04 submission PDF from the Lab 04 section of README.md.

Usage:
    python3 docs/lab04/build-pdf.py [output.pdf]

Default output: ~/Downloads/Lab_04_Security_2026-II_ServiYa.pdf
Requires: node/npx (marked) and google-chrome (headless print).

The evidence files under docs/lab04/ are part of the repository/PR but are
deliberately NOT printed in the PDF.
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

md = lab
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
  </table>
</section>
"""
cover = cover.replace("__PR_URL__", PR_URL)

css = """
@page { size: A4; margin: 17mm 15mm 18mm 15mm; }
* { box-sizing: border-box; }
body { font-family: "DejaVu Sans", "Segoe UI", Arial, sans-serif; font-size: 9.6pt;
       line-height: 1.38; color: #000; margin: 0; }
h1, h2, h3, h4, p, li, td, th, code, blockquote, figcaption { color: #000; }
h1 { font-size: 17pt; border-bottom: 1px solid #000; padding-bottom: 3px;
     margin: 0 0 10px; page-break-before: always; page-break-after: avoid; }
h2 { font-size: 12.8pt; margin-top: 16px; page-break-after: avoid; }
h3 { font-size: 11pt; margin-top: 13px; page-break-after: avoid; }
p, li { orphans: 3; widows: 3; }
code { font-family: "DejaVu Sans Mono", monospace; font-size: 8.8pt;
       background: #f0f0f0; padding: 0 3px; }
pre { background: #f7f7f7; border: 1px solid #333; padding: 7px 9px;
      page-break-inside: avoid; }
pre code { background: none; padding: 0; font-size: 8.6pt; white-space: pre-wrap;
           word-break: break-word; }
table { border-collapse: collapse; width: 100%; margin: 8px 0 12px; font-size: 8.9pt; }
th, td { border: 1px solid #000; padding: 4px 6px; vertical-align: top; text-align: left; }
th { background: #ededed; }
tr { page-break-inside: avoid; }
blockquote { border-left: 3px solid #000; margin: 8px 0; padding: 2px 10px;
             background: #f7f7f7; }
a { color: #000; text-decoration: underline; }
hr { border: 0; border-top: 1px solid #000; margin: 14px 0; }
.cover { page-break-after: always; padding-top: 22mm; }
.kicker { color: #000; letter-spacing: .10em; font-size: 9pt; text-transform: uppercase; }
.cover-title { border: 0; page-break-before: avoid; font-size: 27pt; margin: 12px 0 2px; }
.cover-sub { font-size: 16pt; margin: 0 0 26px; }
table.meta th { width: 26%; background: #ededed; }
table.meta td, table.meta th { font-size: 9.8pt; }
.diagram { display: flex; align-items: stretch; gap: 7px; margin: 12px 0 4px;
           page-break-inside: avoid; }
.zone { border: 1px solid #000; padding: 7px 7px 9px; flex: 1; }
.zone-title { font-size: 7.6pt; text-transform: uppercase; letter-spacing: .05em;
              margin-bottom: 6px; }
.box { border: 1px solid #000; padding: 6px 7px; font-size: 8.4pt;
       text-align: center; margin-bottom: 5px; }
.zone .box:last-child { margin-bottom: 0; }
.box.small { font-size: 7.8pt; padding: 5px 4px; }
.row { display: flex; gap: 4px; }
.row .box { flex: 1; margin-bottom: 0; }
.arrow { display: flex; flex-direction: column; justify-content: center;
         align-items: center; text-align: center; font-size: 7.4pt;
         min-width: 62px; }
.arrow span { font-weight: bold; }
.caption { font-size: 9pt; margin-top: 2px; }
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
