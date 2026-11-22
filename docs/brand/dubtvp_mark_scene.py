"""
DubTvP mark: Blender (Cycles) scene for the red glass D whose counter is the play sign (T-031).

Run with Blender's Python module (tested with bpy 5.0.1 on Python 3.11, CPU rendering):

    pip install bpy==5.0.1
    python dubtvp_mark_scene.py alpha=1 res=1024 out=/abs/path/mark_alpha.png      # transparent mark (app assets)
    python dubtvp_mark_scene.py res=768 out=/abs/path/hero.png                      # on a dark studio backdrop
    python dubtvp_mark_scene.py view=turn t=0.5 alpha=1 res=512 samples=40 out=...  # one frame of the intro turn

The geometry is the same 256-unit grid as the flat mark (ic_launcher_mono_dubtvp.xml). Every value can be overridden
with name=value on the command line; the defaults are the look chosen in November 2026.
"""
import bpy, math, sys
from mathutils import Vector

args = dict(a.split("=", 1) for a in sys.argv[1:] if "=" in a)
def arg(name, default): return float(args.get(name, default))

RES = int(args.get("res", "768")); SAMPLES = int(args.get("samples", "128")); OUT = args.get("out", "/tmp/dubtvp_mark.png")
ALPHA = args.get("alpha", "0") == "1"; VIEW = args.get("view", "front"); FRAME = arg("t", "0")

bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene

# ---------- geometry: D with the play sign as its counter (256 grid -> Blender units / 100, y up) ----------
def P(x, y): return ((x - 128) / 100.0, (128 - y) / 100.0)
def arc(cx, cy, r, a0, a1, n):
    return [P(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)), cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]
outer = [P(78, 53), P(123, 53)] + arc(123, 128, 75, -90, 90, 96)[1:] + [P(78, 203)] \
    + arc(78, 183, 20, 90, 180, 16)[1:] + [P(58, 73)] + arc(78, 73, 20, 180, 270, 16)[1:-1]
def rounded_triangle(pts, r, n=12):
    out = []
    for i in range(len(pts)):
        a, b, c = pts[i - 1], pts[i], pts[(i + 1) % len(pts)]
        def toward(u, v, t):
            L = math.hypot(v[0] - u[0], v[1] - u[1]); return (u[0] + (v[0] - u[0]) * t / L, u[1] + (v[1] - u[1]) * t / L)
        q1 = toward(b, a, r); q2 = toward(b, c, r)
        for k in range(n + 1):
            t = k / n
            out.append(P((1 - t) ** 2 * q1[0] + 2 * (1 - t) * t * b[0] + t * t * q2[0], (1 - t) ** 2 * q1[1] + 2 * (1 - t) * t * b[1] + t * t * q2[1]))
    return out
tri = rounded_triangle([(94, 92), (156, 128), (94, 164)], 10)
cu = bpy.data.curves.new("D", "CURVE"); cu.dimensions = '2D'; cu.fill_mode = 'BOTH'
for pts in (outer, tri[::-1]):
    sp = cu.splines.new('POLY'); sp.points.add(len(pts) - 1)
    for i, (x, y) in enumerate(pts): sp.points[i].co = (x, y, 0, 1)
    sp.use_cyclic_u = True
cu.extrude = 0.16; cu.bevel_depth = 0.07; cu.bevel_resolution = 8           # thick slab with fully rounded rims
ob = bpy.data.objects.new("D", cu); sc.collection.objects.link(ob)
bpy.context.view_layer.objects.active = ob; ob.select_set(True)
bpy.ops.object.convert(target='MESH'); ob = bpy.context.view_layer.objects.active
bpy.ops.object.shade_smooth_by_angle(angle=math.radians(35))                # flat faces stay flat, rims are smooth

# ---------- ruby glass: clear surface, colour from absorption (thicker = deeper red) ----------
def principled(name):
    m = bpy.data.materials.new(name); m.use_nodes = True; return m, m.node_tree.nodes["Principled BSDF"], m.node_tree
m, b, nt = principled("ruby")
b.inputs["Base Color"].default_value = (1.0, 0.86, 0.87, 1)
b.inputs["Transmission Weight"].default_value = 1.0
b.inputs["Roughness"].default_value = 0.03
b.inputs["IOR"].default_value = 1.52
vol = nt.nodes.new("ShaderNodeVolumeAbsorption"); vol.inputs["Color"].default_value = (0.92, 0.025, 0.06, 1)
vol.inputs["Density"].default_value = arg("dens", "8")
nt.links.new(vol.outputs[0], nt.nodes["Material Output"].inputs["Volume"])
ob.data.materials.append(m)

# ---------- dark backdrop (hidden from the camera for the transparent render) ----------
bpy.ops.mesh.primitive_plane_add(size=30, location=(0, 0, -0.9)); bg = bpy.context.object
bm, bb, _ = principled("backdrop"); bb.inputs["Base Color"].default_value = (0.006, 0.006, 0.007, 1); bb.inputs["Roughness"].default_value = 0.6
bg.data.materials.append(bm)
if ALPHA: bg.visible_camera = False

# ---------- product-studio lights ----------
def area(name, loc, energy, sx, sy, target=(0, 0, 0)):
    l = bpy.data.lights.new(name, 'AREA'); l.energy = energy; l.shape = 'RECTANGLE'; l.size = sx; l.size_y = sy
    o = bpy.data.objects.new(name, l); o.location = loc; sc.collection.objects.link(o)
    o.rotation_euler = (Vector(target) - Vector(loc)).to_track_quat('-Z', 'Y').to_euler()
    return o
area("key", (-1.2, 4.2, 3.4), arg("key", "250"), 5.0, 0.9)        # long thin softbox above: crisp line on the top rim
area("rimR", (3.6, -0.2, 1.2), 520, 0.35, 5.0)                     # strip right: bright right edge
area("rimL", (-3.8, 0.4, 0.8), 240, 0.3, 4.0)                      # weaker strip left
# one crisp glint: a thin strip placed near the mirror direction of the view about the front face
g = area("glint", tuple(float(v) for v in args.get("gpos", "5.0,2.6,6.3").split(",")), arg("glint", "140"), arg("gw", "0.15"), 7.0)
g.rotation_euler.rotate_axis('Z', math.radians(arg("gang", "40"))); g.visible_diffuse = False
# soft pool of light on the backdrop
sp = bpy.data.lights.new("pool", 'SPOT'); sp.energy = arg("pool", "300"); sp.spot_size = math.radians(30); sp.spot_blend = 1.0; sp.shadow_soft_size = 0.6
spo = bpy.data.objects.new("pool", sp); spo.location = (0.6, -1.2, 5.5); sc.collection.objects.link(spo)
spo.rotation_euler = (Vector((0.15, 0.05, -0.9)) - spo.location).to_track_quat('-Z', 'Y').to_euler()
# hidden backlight: seen only through the glass (transmission rays), never by the camera, so the ruby glows
bpy.ops.mesh.primitive_plane_add(size=3.4, location=(0.05, 0.05, -0.55)); em = bpy.context.object
emm = bpy.data.materials.new("backlight"); emm.use_nodes = True; ent = emm.node_tree; ent.nodes.clear()
out = ent.nodes.new("ShaderNodeOutputMaterial"); es = ent.nodes.new("ShaderNodeEmission"); es.inputs["Color"].default_value = (1.0, 0.93, 0.9, 1)
tc = ent.nodes.new("ShaderNodeTexCoord"); gr = ent.nodes.new("ShaderNodeTexGradient"); gr.gradient_type = 'SPHERICAL'
mp = ent.nodes.new("ShaderNodeMapping"); s = arg("blscale", "0.75"); mp.inputs["Scale"].default_value = (s, s, s)
mt = ent.nodes.new("ShaderNodeMath"); mt.operation = 'MULTIPLY'; mt.inputs[1].default_value = arg("bl", "2.4")
ent.links.new(tc.outputs["Object"], mp.inputs["Vector"]); ent.links.new(mp.outputs[0], gr.inputs["Vector"])
ent.links.new(gr.outputs["Fac"], mt.inputs[0]); ent.links.new(mt.outputs[0], es.inputs["Strength"]); ent.links.new(es.outputs[0], out.inputs["Surface"])
em.data.materials.append(emm)
em.visible_camera = False; em.visible_shadow = False; em.visible_diffuse = False; em.visible_glossy = False
w = bpy.data.worlds.new("w"); sc.world = w; w.use_nodes = True
w.node_tree.nodes["Background"].inputs["Color"].default_value = (0.002, 0.002, 0.003, 1)

# ---------- camera and pose ----------
cam = bpy.data.cameras.new("cam"); co = bpy.data.objects.new("cam", cam); sc.collection.objects.link(co); sc.camera = co
co.location = (0.0, -0.35, 9.0); cam.sensor_width = 36; cam.lens = 85 * 9.0 / 7.2
if VIEW == "front":
    ob.rotation_euler = (math.radians(arg("rx", "-10")), math.radians(arg("ry", "16")), 0)
else:  # intro turn, t = 0..1 with ease-out: the fixed lights sweep across the glass while it turns
    e = 1 - (1 - FRAME) ** 3
    ob.rotation_euler = (math.radians(-10 + 4 * (1 - e)), math.radians(-38 + 54 * e), 0)
co.rotation_euler = (Vector((0, 0, 0)) - co.location).to_track_quat('-Z', 'Y').to_euler()

# ---------- render ----------
sc.render.engine = 'CYCLES'; sc.cycles.device = 'CPU'; sc.cycles.samples = SAMPLES; sc.cycles.use_denoising = True
sc.cycles.max_bounces = 16; sc.cycles.transmission_bounces = 16; sc.cycles.volume_bounces = 4
sc.render.resolution_x = RES; sc.render.resolution_y = RES; sc.render.resolution_percentage = 100
sc.view_settings.view_transform = args.get("vt", "Khronos PBR Neutral")   # keeps saturated reds (AgX turns them pink)
sc.render.film_transparent = ALPHA
sc.render.filepath = OUT; bpy.ops.render.render(write_still=True)
