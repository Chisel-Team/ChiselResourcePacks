package team.chisel.resourcepacks.common.asm;

import java.util.Iterator;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import team.chisel.Chisel;

@MCVersion("1.9")
public class ChiselResourceTransformer implements IClassTransformer {

    // From EnderCore
    protected static class ObfSafeName {

        private String deobf, srg;

        public ObfSafeName(String deobf, String srg) {
            this.deobf = deobf;
            this.srg = srg;
        }

        public String getName() {
            return ChiselResourceCorePlugin.runtimeDeobfEnabled ? srg : deobf;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof String) {
                return obj.equals(deobf) || obj.equals(srg);
            } else if (obj instanceof ObfSafeName) {
                return ((ObfSafeName) obj).deobf.equals(deobf) && ((ObfSafeName) obj).srg.equals(srg);
            }
            return false;
        }

        // no hashcode because I'm naughty
    }
    
    private static final String methodsClass = "team/chisel/resourcepacks/common/asm/ChiselResourceMethods";

    private static final String blockRendererDispatcherClass = "net.minecraft.client.renderer.BlockRendererDispatcher";
    private static final ObfSafeName extendedStateMethod = new ObfSafeName("renderBlock", "func_175018_a");
    private static final String extendedStateMethodSig = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;";
    
    private static final String blockModelRendererClass = "net.minecraft.client.renderer.BlockModelRenderer";
    private static final String forgeBlockModelRendererClass = "net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer";
    private static final ObfSafeName smoothRenderMethod = new ObfSafeName("renderModelSmooth", "func_187498_b");
    private static final ObfSafeName flatRenderMethod = new ObfSafeName("renderModelFlat", "func_187497_c");
    private static final ObfSafeName forgeRender = new ObfSafeName("render", "render"); // Forge method, no srg
    private static final String cleanStateMethodSig = "(Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/block/state/IBlockState;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals(blockRendererDispatcherClass) || transformedName.equals(blockModelRendererClass) || transformedName.equals(forgeBlockModelRendererClass)) {
            Chisel.logger.info("Transforming Class [" + transformedName + "], Method [" + extendedStateMethod.getName() + "]");

            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(basicClass);
			classReader.accept(classNode, 0);

			Iterator<MethodNode> methods = classNode.methods.iterator();

			switch (transformedName) {
			case blockRendererDispatcherClass:
				transformBlockRendererDispatcher(methods);
				break;
			case blockModelRendererClass:
				transformBlockModelRenderer(methods, smoothRenderMethod.getName(), flatRenderMethod.getName());
				break;
			case forgeBlockModelRendererClass:
				transformBlockModelRenderer(methods, forgeRender.getName());
				break;
			default:
				break;
			}

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);
            Chisel.logger.info("Transforming " + transformedName + " Finished.");
            return cw.toByteArray();
        }
        
        return basicClass;
    }
    
    private void transformBlockRendererDispatcher(Iterator<MethodNode> methods) {

        while (methods.hasNext()) {
            MethodNode m = methods.next();
            if (extendedStateMethod.equals(m.name)) {
                for (int i = 0; i < m.instructions.size(); i++) {
                    AbstractInsnNode next = m.instructions.get(i);
                    if (next instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) next;
                        if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.name.equals("getExtendedState")) {
                            if ((next = call.getNext()) instanceof VarInsnNode) {
                                VarInsnNode store = (VarInsnNode) next;
                                if (store.getOpcode() == Opcodes.ASTORE && store.var == 1) {
                                    m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, methodsClass, "getExtendedState", extendedStateMethodSig, false));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void transformBlockModelRenderer(Iterator<MethodNode> methods, String... names) {
    	while (methods.hasNext()) {
    		MethodNode m = methods.next();
    		if (ArrayUtils.contains(names, m.name)) {
    			for (int i = 0; i < m.instructions.size(); i++) {
                    AbstractInsnNode next = m.instructions.get(i);
                    if (next instanceof VarInsnNode) {
                    	VarInsnNode load = (VarInsnNode) next;
                        if (load.getOpcode() == Opcodes.ILOAD && load.var == 6 /* checkSides */) {
                        	load = (VarInsnNode) load.getNext().getNext();
                        	m.instructions.insert(load, new MethodInsnNode(Opcodes.INVOKESTATIC, methodsClass, "cleanState", cleanStateMethodSig, false));
                        }
                    }
                }
    		}
    	}
    }
}
