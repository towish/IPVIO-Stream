"""
DubTvP app assets from the transparent Blender render of the mark (T-031).

    python compose_assets.py MARK_ALPHA.png WORDMARK_ALPHA.png OUT_DIR

MARK_ALPHA.png comes from dubtvp_mark_scene.py (alpha=1 res=1024), WORDMARK_ALPHA.png from
dubtvp_wordmark_scene.py (brushed-aluminium "DubTvP" in Sora SemiBold). Needs numpy and pillow. Writes, ready to copy
into app/src/main/res:
  ic_brand_mark_3d.webp              -> drawable-nodpi/   (512 px, transparent; BrandMark / BrandLoader)
  ic_launcher_fg_dubtvp.webp         -> drawable-nodpi/   (432 px adaptive-icon foreground, mark in the safe zone)
  ic_launcher_bg_dubtvp_spun.webp    -> drawable-nodpi/   (432 px adaptive-icon background: dark spun titanium)
  intro_backdrop_spun.webp           -> drawable-nodpi/   (1280 x 720 backdrop of the start intro, T-036)
  brand_wordmark_metal.webp          -> drawable-nodpi/   (transparent brushed-metal wordmark, 240 px high, T-036)
  mipmap-<dpi>_ic_launcher[_round].webp -> mipmap-<dpi>/ic_launcher[_round].webp (legacy icons, 48…192 px)
  banner.png                         -> drawable/         (640 x 360 Android TV banner with the DubTvP wordmark)
  banner_1080.png                    (1920 x 1080 copy of the banner for the README, store pages or a splash)
"""
import sys
import numpy as np
from PIL import Image, ImageFilter, ImageDraw, ImageChops

SRC, WMARK, OUT = sys.argv[1], sys.argv[2], sys.argv[3].rstrip("/") + "/"
src = Image.open(SRC).convert("RGBA")
mark = src.crop(src.getbbox())
wm_src = Image.open(WMARK).convert("RGBA")
wordmark = wm_src.crop(wm_src.getbbox())

def fit(im, box):
    w, h = im.size; s = box / max(w, h); return im.resize((max(1, round(w * s)), max(1, round(h * s))), Image.LANCZOS)

def soft(im, blur, opacity, dy, color=(0, 0, 0)):
    """Blurred silhouette of im (shadow or glow), returned with its padding."""
    a = im.split()[3].point(lambda v: int(v * opacity)); sh = Image.new("RGBA", im.size, color + (0,)); sh.putalpha(a)
    pad = int(blur * 3) + abs(dy); big = Image.new("RGBA", (im.size[0] + 2 * pad, im.size[1] + 2 * pad), (0, 0, 0, 0))
    big.alpha_composite(sh, (pad, pad + dy)); return big.filter(ImageFilter.GaussianBlur(blur)), pad

def place(canvas, im, cx, cy, shadow=None, glow=None):
    for spec, color, dy in ((glow, (255, 30, 60), 0), (shadow, (0, 0, 0), None)):
        if spec:
            s, p = soft(im, spec[0], spec[1], spec[2] if dy is None else 0, color)
            canvas.alpha_composite(s, (round(cx - im.size[0] / 2 - p), round(cy - im.size[1] / 2 - p)))
    canvas.alpha_composite(im, (round(cx - im.size[0] / 2), round(cy - im.size[1] / 2)))

def spun(W, H, cx, cy, seed=7, angle=-32, lobe=0.30, gain=0.16, base=0.030, ring_k=0.10, red=0.0):
    """Dark spun titanium: fine concentric lathe marks, the radial highlight of spun metal, red light of the glass."""
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    dx = x - cx * W; dy = y - cy * H; r = np.hypot(dx, dy); th = np.arctan2(dy, dx)
    n = int(r.max() / 0.6) + 2; rings = rng.normal(0, 1, n).astype(np.float32)
    k = np.exp(-np.arange(-3, 4) ** 2 / 2.0); rings = np.convolve(rings, k / k.sum(), 'same'); rings = rings / rings.std()
    ring = np.interp(r / 0.6, np.arange(n), rings)
    ring = ring * 0.75 + 0.25 * np.interp(r / 0.6 + 3 * np.sin(th * 3 + r * 0.01), np.arange(n), rings)
    a = np.deg2rad(angle)
    d1 = np.angle(np.exp(1j * (th - a))); d2 = np.angle(np.exp(1j * (th - a - np.pi)))
    lob = np.exp(-(d1 / lobe) ** 2) + 0.55 * np.exp(-(d2 / lobe) ** 2)
    R = max(W, H); fall = np.exp(-(r / (0.55 * R)) ** 2) * (1 - np.exp(-(r / (0.03 * R)) ** 2))
    lum = base + gain * lob * fall
    lum = lum * (1 + ring_k * ring * (0.4 + 2.5 * lob * fall))
    lum = lum * (1 - 0.35 * np.clip(np.hypot((x - W / 2) / (W * 0.75), (y - H / 2) / (H * 0.75)), 0, 1) ** 2)
    rgb = np.stack([lum * 0.97, lum * 0.98, lum * 1.04], -1)
    if red > 0:
        gl = np.exp(-(r / (0.20 * R)) ** 2)[..., None] * np.array([0.55, 0.06, 0.10]) * red
        rgb = rgb + gl * (0.6 + 0.4 * lob[..., None])
    rgb = rgb + rng.normal(0, 0.0035, rgb.shape)
    return Image.fromarray((np.clip(rgb, 0, 1) * 255).astype(np.uint8)).convert("RGBA")

def icon_spun(S): return spun(S, S, 0.5, 0.495, angle=-35, lobe=0.34, gain=0.30, base=0.045, red=0.45, ring_k=0.10)

def save_webp(im, name): im.save(OUT + name, lossless=True, quality=100)

# 1) in-app mark
m512 = Image.new("RGBA", (512, 512), (0, 0, 0, 0)); place(m512, fit(mark, 452), 256, 256); save_webp(m512, "ic_brand_mark_3d.webp")
# 2) adaptive foreground (108 dp at 4x; safe zone 66 dp = 264 px)
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0)); place(fg, fit(mark, 250), 216, 214, shadow=(9, 0.55, 7), glow=(18, 0.30, 0))
save_webp(fg, "ic_launcher_fg_dubtvp.webp")
icon_spun(432).convert("RGB").save(OUT + "ic_launcher_bg_dubtvp_spun.webp", quality=92, method=6)
# 3) legacy launcher icons
def legacy(round_, S=768):
    base = icon_spun(S); mask = Image.new("L", (S, S), 0); d = ImageDraw.Draw(mask)
    if round_: d.ellipse((0, 0, S - 1, S - 1), fill=255)
    else: d.rounded_rectangle((0, 0, S - 1, S - 1), radius=int(S * 0.22), fill=255)
    place(base, fit(mark, int(S * 0.60)), S / 2, S * 0.495, shadow=(S * 0.02, 0.55, int(S * 0.016)), glow=(S * 0.045, 0.28, 0))
    base.putalpha(ImageChops.multiply(base.split()[3], mask)); return base
for round_ in (False, True):
    big = legacy(round_)
    for dpi, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        save_webp(big.resize((px, px), Image.LANCZOS), f"mipmap-{dpi}_ic_launcher{'_round' if round_ else ''}.webp")
# 4) TV banner, drawn at 1920 x 1080: glass D + brushed-aluminium wordmark on spun titanium
def banner(W, H):
    mk = fit(mark, int(H * 0.50)); wh = H * 0.15; ww = wordmark.size[0] * wh / wordmark.size[1]; gap = H * 0.065
    x0 = (W - (mk.size[0] + gap + ww)) / 2; mx = x0 + mk.size[0] / 2
    base = spun(W, H, mx / W, 0.5, red=0.35, gain=0.22)
    place(base, mk, mx, H / 2, shadow=(H * 0.018, 0.65, int(H * 0.014)), glow=(H * 0.05, 0.30, 0))
    wm = wordmark.resize((round(ww), round(wh)), Image.LANCZOS)
    place(base, wm, x0 + mk.size[0] + gap + ww / 2, H / 2 + H * 0.004, shadow=(H * 0.010, 0.75, int(H * 0.008)))
    return base.convert("RGB")
# 5) start intro (T-036): the banner's backdrop on its own, spin centre behind the D, and the wordmark alone
def banner_mark_centre(W, H):
    mk = fit(mark, int(H * 0.50)); wh = H * 0.15; ww = wordmark.size[0] * wh / wordmark.size[1]; gap = H * 0.065
    return ((W - (mk.size[0] + gap + ww)) / 2 + mk.size[0] / 2) / W
spun(1280, 720, banner_mark_centre(1280, 720), 0.5, red=0.35, gain=0.22).convert("RGB").save(OUT + "intro_backdrop_spun.webp", quality=92, method=6)
wordmark.resize((round(wordmark.size[0] * 240 / wordmark.size[1]), 240), Image.LANCZOS).save(OUT + "brand_wordmark_metal.webp", quality=92, method=6, exact=True)
big = banner(1920, 1080)
big.save(OUT + "banner_1080.png", optimize=True)
big.resize((640, 360), Image.LANCZOS).save(OUT + "banner.png", optimize=True)
print("written to", OUT)
