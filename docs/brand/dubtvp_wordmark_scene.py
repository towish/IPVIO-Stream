"""
DubTvP wordmark in brushed aluminium (Blender/Cycles), on a transparent background (T-031).

    pip install bpy==5.0.1
    python dubtvp_wordmark_scene.py font=/abs/path/sora-latin-600-normal.ttf w=2400 h=600 out=/abs/path/wordmark.png

Typeface: Sora SemiBold (SIL Open Font License 1.1), for example from the npm package @fontsource/sora
(files/sora-latin-600-normal.woff, converted to TTF with fontTools). The font is only needed to render the picture;
the app does not ship it. Every value can be overridden with name=value; the defaults are the chosen look.
"""
import bpy, math, sys
from mathutils import Vector
args=dict(a.split("=",1) for a in sys.argv[1:] if "=" in a)
def arg(n,d): return float(args.get(n,d))
FONT=args.get("font"); TEXT=args.get("text","DubTvP"); OUT=args.get("out","/tmp/wm.png")
RW=int(args.get("w","1600")); RH=int(args.get("h","480")); SAMPLES=int(args.get("samples","128"))
bpy.ops.wm.read_factory_settings(use_empty=True); sc=bpy.context.scene
cu=bpy.data.curves.new("wm",'FONT'); cu.body=TEXT; cu.font=bpy.data.fonts.load(FONT)
cu.align_x='CENTER'; cu.align_y='CENTER'; cu.size=1.0; cu.space_character=arg("track","0.97")
cu.extrude=arg("ext","0.05"); cu.bevel_depth=arg("bev","0.014"); cu.bevel_resolution=5; cu.resolution_u=24
ob=bpy.data.objects.new("wm",cu); sc.collection.objects.link(ob)
bpy.context.view_layer.objects.active=ob; ob.select_set(True)
bpy.ops.object.convert(target='MESH'); ob=bpy.context.view_layer.objects.active
bpy.ops.object.shade_smooth_by_angle(angle=math.radians(40))
# ---- brushed metal: fine horizontal streaks drive roughness, colour and a tiny bump; anisotropic along X ----
m=bpy.data.materials.new("brushed"); m.use_nodes=True; nt=m.node_tree; b=nt.nodes["Principled BSDF"]
b.inputs["Metallic"].default_value=1.0
b.inputs["Anisotropic"].default_value=arg("aniso","0.7")
tc=nt.nodes.new("ShaderNodeTexCoord"); mp=nt.nodes.new("ShaderNodeMapping"); mp.inputs["Scale"].default_value=(arg("sx","0.6"),arg("sy","520"),1)
nz=nt.nodes.new("ShaderNodeTexNoise"); nz.inputs["Scale"].default_value=1.0; nz.inputs["Detail"].default_value=6; nz.inputs["Roughness"].default_value=0.7
nt.links.new(tc.outputs["Object"],mp.inputs["Vector"]); nt.links.new(mp.outputs[0],nz.inputs["Vector"])
rr=nt.nodes.new("ShaderNodeMapRange"); rr.inputs["From Min"].default_value=0.3; rr.inputs["From Max"].default_value=0.7
rr.inputs["To Min"].default_value=arg("rmin","0.24"); rr.inputs["To Max"].default_value=arg("rmax","0.32")
nt.links.new(nz.outputs["Fac"],rr.inputs["Value"]); nt.links.new(rr.outputs[0],b.inputs["Roughness"])
cr=nt.nodes.new("ShaderNodeValToRGB"); cr.color_ramp.elements[0].position=0.3; cr.color_ramp.elements[1].position=0.7
c0=arg("c0","0.86"); c1=arg("c1","0.97")
cr.color_ramp.elements[0].color=(c0,c0*1.005,c0*1.03,1); cr.color_ramp.elements[1].color=(c1,c1*1.005,c1*1.02,1)
nt.links.new(nz.outputs["Fac"],cr.inputs["Fac"]); nt.links.new(cr.outputs["Color"],b.inputs["Base Color"])
bp=nt.nodes.new("ShaderNodeBump"); bp.inputs["Strength"].default_value=arg("bump","0.015"); bp.inputs["Distance"].default_value=0.002
nt.links.new(nz.outputs["Fac"],bp.inputs["Height"]); nt.links.new(bp.outputs["Normal"],b.inputs["Normal"])
tg=nt.nodes.new("ShaderNodeCombineXYZ"); tg.inputs["X"].default_value=1.0; nt.links.new(tg.outputs[0],b.inputs["Tangent"])
ob.data.materials.append(m)
# ---- studio: broad soft key for the faces, thin strips for the bevels ----
def area(name,loc,energy,sx,sy,target=(0,0,0),color=(1,1,1)):
    l=bpy.data.lights.new(name,'AREA'); l.energy=energy; l.shape='RECTANGLE'; l.size=sx; l.size_y=sy; l.color=color
    o=bpy.data.objects.new(name,l); o.location=loc; sc.collection.objects.link(o)
    o.rotation_euler=(Vector(target)-Vector(loc)).to_track_quat('-Z','Y').to_euler(); return o
area("face",(0.0,1.6,4.2),arg("kface","380"),7.0,1.6)              # broad softbox above the camera: brushed sheen on the faces
area("top",(0.0,3.0,1.2),arg("ktop","320"),9.0,0.25)              # thin strip above: bright line on the upper bevels
area("low",(0.0,-3.0,0.9),arg("klow","70"),9.0,0.25,color=(1.0,0.86,0.86))   # weak warm strip below: lower bevels
area("side",(5.5,0.3,1.4),arg("kside","90"),0.3,3.0)
# the world reflects in polished metal: dark with a soft horizon band
w=bpy.data.worlds.new("w"); sc.world=w; w.use_nodes=True; wn=w.node_tree
bg=wn.nodes["Background"]; gr=wn.nodes.new("ShaderNodeTexGradient"); tcw=wn.nodes.new("ShaderNodeTexCoord")
ramp=wn.nodes.new("ShaderNodeValToRGB"); ramp.color_ramp.elements[0].color=(0.002,0.002,0.003,1); ramp.color_ramp.elements[1].color=(0.05,0.05,0.055,1)
sep=wn.nodes.new("ShaderNodeSeparateXYZ"); wn.links.new(tcw.outputs["Generated"],sep.inputs[0])
mr=wn.nodes.new("ShaderNodeMapRange"); mr.inputs["From Min"].default_value=0.35; mr.inputs["From Max"].default_value=0.75
wn.links.new(sep.outputs["Z"],mr.inputs["Value"]); wn.links.new(mr.outputs[0],ramp.inputs["Fac"]); wn.links.new(ramp.outputs["Color"],bg.inputs["Color"])
# ---- camera: orthographic, front, a hair from below so the top bevels catch the strip ----
cam=bpy.data.cameras.new("cam"); cam.type='ORTHO'; co=bpy.data.objects.new("cam",cam); sc.collection.objects.link(co); sc.camera=co
dims=ob.dimensions; cam.ortho_scale=max(dims.x*1.08, dims.y*1.08*RW/RH)
co.location=(0,-0.35,6.0); co.rotation_euler=(Vector((0,0,0))-co.location).to_track_quat('-Z','Y').to_euler()
sc.render.engine='CYCLES'; sc.cycles.device='CPU'; sc.cycles.samples=SAMPLES; sc.cycles.use_denoising=True
sc.render.resolution_x=RW; sc.render.resolution_y=RH; sc.render.film_transparent=True
sc.view_settings.view_transform="Khronos PBR Neutral"
sc.render.filepath=OUT; bpy.ops.render.render(write_still=True)
print("dims", tuple(round(v,4) for v in dims), "ortho", cam.ortho_scale)
