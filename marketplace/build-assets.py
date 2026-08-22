#!/usr/bin/env python3
"""Render marketplace icon + banner for the EzyVector plugin via headless Chrome."""
import base64
import os
import subprocess
import sys
import tempfile

STORY_ROOT = "/Users/tvd12/Documents/tvd12/java/products/story"
FONTS = os.path.join(STORY_ROOT, "story-theme/src/main/resources/static/fonts")
SCRATCH = tempfile.mkdtemp(prefix="ezyvector-market-")
OUT = os.path.dirname(os.path.abspath(__file__))
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

os.makedirs(OUT, exist_ok=True)


def font_face(family, weight, files, style="normal"):
    out = []
    for f in files:
        path = os.path.join(FONTS, f)
        with open(path, "rb") as fp:
            b64 = base64.b64encode(fp.read()).decode()
        out.append(
            "@font-face{font-family:'%s';font-style:%s;font-weight:%d;font-display:block;"
            "src:url(data:font/woff2;base64,%s) format('woff2');}" % (family, style, weight, b64)
        )
    return "".join(out)


FONT_CSS = (
    font_face("Fraunces", 900, ["Fraunces-Black-vn.woff2", "Fraunces-Black-latin.woff2"])
    + font_face("Fraunces", 600, ["Fraunces-SemiBold-vn.woff2", "Fraunces-SemiBold-latin.woff2"])
    + font_face("Be Vietnam Pro", 700, ["BeVietnamPro-Bold-vn.woff2", "BeVietnamPro-Bold-latin.woff2"])
    + font_face("Be Vietnam Pro", 600, ["BeVietnamPro-SemiBold-vn.woff2", "BeVietnamPro-SemiBold-latin.woff2"])
    + font_face("Be Vietnam Pro", 500, ["BeVietnamPro-Medium-vn.woff2", "BeVietnamPro-Medium-latin.woff2"])
    + font_face("Be Vietnam Pro", 400, ["BeVietnamPro-Regular-vn.woff2", "BeVietnamPro-Regular-latin.woff2"])
)

INK = "#1B1B2F"
VIOLET = "#7C3AED"
VIOLET_DARK = "#4C1D95"
VIOLET_LIGHT = "#EDE9FE"
TEAL = "#0D9488"
AMBER = "#F59E0B"
PAPER = "#FFFFFF"

TAGLINE = ("A vector database plugin for EzyPlatform with HNSW indexing "
           "to store and search RAG vectors.")


def mark(scale=1.0):
    """The brand mark: an HNSW graph (entry node -> layers) with the matched
    vector highlighted by a search ring."""
    sw = 1.6 / scale
    return f"""
    <path d="M32 6 L14 22" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M32 6 L32 20" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M32 6 L50 22" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M14 22 L32 20" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".4"/>
    <path d="M32 20 L50 22" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".4"/>
    <path d="M14 22 L8 40" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M14 22 L24 42" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M32 20 L24 42" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M32 20 L40 42" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M50 22 L40 42" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <path d="M50 22 L54 40" stroke="{INK}" stroke-width="{sw}" fill="none" stroke-linecap="round" opacity=".5"/>
    <circle cx="32" cy="6" r="4.4" fill="{VIOLET_DARK}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="14" cy="22" r="3.9" fill="{VIOLET}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="32" cy="20" r="3.9" fill="{VIOLET}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="50" cy="22" r="3.9" fill="{VIOLET}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="8" cy="40" r="3.4" fill="{TEAL}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="24" cy="42" r="3.4" fill="{TEAL}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="54" cy="40" r="3.4" fill="{TEAL}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="40" cy="42" r="3.6" fill="{AMBER}" stroke="{INK}" stroke-width="{sw}"/>
    <circle cx="40" cy="42" r="8.4" fill="none" stroke="{AMBER}" stroke-width="{2.6/scale}"/>
    <path d="M46 48 L52.5 54.5" stroke="{AMBER}" stroke-width="{3.2/scale}" stroke-linecap="round"/>"""


ICON_HTML = f"""<!doctype html><html><head><meta charset="utf-8"><style>
{FONT_CSS}
*{{margin:0;padding:0;box-sizing:border-box}}
html,body{{width:512px;height:512px;overflow:hidden;background:transparent}}
.icon{{width:512px;height:512px;border-radius:112px;background:{VIOLET_LIGHT};
  border:10px solid {INK};position:relative;overflow:hidden;display:flex;
  align-items:center;justify-content:center}}
.icon::before{{content:'';position:absolute;inset:0;
  background:radial-gradient(120% 90% at 18% 8%, rgba(255,255,255,.85) 0%, rgba(255,255,255,0) 55%)}}
svg{{width:378px;height:378px;position:relative}}
</style></head><body>
<div class="icon">
  <svg viewBox="2 -1 60 62" xmlns="http://www.w3.org/2000/svg">{mark()}</svg>
</div>
</body></html>"""

BANNER_HTML = f"""<!doctype html><html><head><meta charset="utf-8"><style>
{FONT_CSS}
*{{margin:0;padding:0;box-sizing:border-box}}
html,body{{width:820px;height:360px;overflow:hidden}}
.banner{{width:820px;height:360px;position:relative;overflow:hidden;
  background:linear-gradient(135deg,{PAPER} 0%,#F5F3FF 55%,{VIOLET_LIGHT} 100%);
  display:flex;align-items:center;font-family:'Be Vietnam Pro',sans-serif}}
.blob{{position:absolute;border-radius:50%}}
.blob-1{{width:430px;height:430px;right:-120px;top:-110px;
  background:radial-gradient(circle at 35% 35%,{VIOLET} 0%,{VIOLET_DARK} 60%,{INK} 100%);opacity:.14}}
.blob-2{{width:190px;height:190px;right:150px;bottom:-90px;background:{TEAL};opacity:.12}}
.rule{{position:absolute;left:0;top:0;bottom:0;width:10px;
  background:linear-gradient(180deg,{TEAL} 0%,{VIOLET} 55%,{AMBER} 100%)}}
.left{{padding:0 26px 0 54px;width:530px;position:relative;z-index:2}}
h1{{font-family:'Fraunces',serif;font-weight:900;font-size:34px;line-height:1.16;
  color:{VIOLET_DARK};letter-spacing:-.8px;margin-bottom:16px}}
h1 em{{font-style:normal;color:{AMBER}}}
p{{font-size:15px;line-height:1.6;color:#3A3A4A;font-weight:500;max-width:470px}}
.chips{{display:flex;gap:8px;margin-top:22px;flex-wrap:wrap}}
.chip{{font-size:12px;font-weight:700;letter-spacing:.3px;text-transform:uppercase;
  padding:6px 12px;border-radius:999px;background:rgba(255,255,255,.85);
  color:{VIOLET_DARK};border:1.5px solid rgba(27,27,47,.14)}}
.chip:nth-child(2){{color:#0E7C90}}
.chip:nth-child(3){{color:#B4530A}}
.art{{position:absolute;right:34px;top:50%;transform:translateY(-50%);z-index:2}}
.art .card{{width:232px;height:232px;border-radius:52px;background:{PAPER};
  border:5px solid {INK};box-shadow:14px 14px 0 rgba(27,27,47,.10);
  display:flex;align-items:center;justify-content:center}}
.art svg{{width:186px;height:186px}}
</style></head><body>
<div class="banner">
  <div class="blob blob-1"></div>
  <div class="blob blob-2"></div>
  <div class="rule"></div>
  <div class="left">
    <h1>Fast vector search,<br><em>built on HNSW</em></h1>
    <p>{TAGLINE}</p>
    <div class="chips">
      <span class="chip">HNSW index</span>
      <span class="chip">RAG ready</span>
      <span class="chip">Fast search</span>
    </div>
  </div>
  <div class="art"><div class="card">
    <svg viewBox="2 -1 60 62" xmlns="http://www.w3.org/2000/svg">{mark()}</svg>
  </div></div>
</div>
</body></html>"""


def render(html, name, w, h, scale):
    src = os.path.join(SCRATCH, name + ".html")
    with open(src, "w") as f:
        f.write(html)
    dst = os.path.join(OUT, name + ".png")
    cmd = [
        CHROME, "--headless", "--disable-gpu", "--no-sandbox", "--hide-scrollbars",
        "--default-background-color=00000000",
        "--force-device-scale-factor=%d" % scale,
        "--window-size=%d,%d" % (w, h),
        "--screenshot=" + dst,
        "--virtual-time-budget=3000",
        "file://" + src,
    ]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if not os.path.exists(dst):
        print(r.stderr[-2000:], file=sys.stderr)
        sys.exit(1)
    print(dst, os.path.getsize(dst), "bytes")


render(ICON_HTML, "icon", 512, 512, 1)
render(BANNER_HTML, "banner", 820, 360, 2)
