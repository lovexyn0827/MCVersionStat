package lovexyn0827.mcversionstat;

import java.lang.reflect.Modifier;
import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassHandler extends ClassVisitor {
	private VersionInfo.Builder ctx;
	private boolean outerClassVisited = false;
	public final Clazz.Builder clazzBuilder = new Clazz.Builder();

	protected ClassHandler(VersionInfo.Builder ctx) {
		super(Opcodes.ASM9);
		this.ctx = ctx;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (Modifier.isInterface(access)) {
			this.ctx.interfaces++;
		}
		
		if (Modifier.isAbstract(access)) {
			this.ctx.abstractClasses++;
		} else if ((access & Opcodes.ACC_ENUM) != 0) {
			this.ctx.enumClasses++;
		} else if ((access & Opcodes.ACC_RECORD) != 0) {
			this.ctx.recordClasses++;
		}
		
		this.ctx.indepClasses++;
		this.ctx.ordinaryClasses++;
		this.clazzBuilder.interfaces = interfaces;
		this.clazzBuilder.name = name;
		this.clazzBuilder.superClazz = superName;
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		this.outerClassVisited = true;
		this.clazzBuilder.outerClazz = owner;
		this.clazzBuilder.annoymous = name != null;
		if (name != null) {
			this.ctx.anonymousClasses++;
		}
		
		this.ctx.indepClasses--;
		this.ctx.innerClasses++;
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (Modifier.isVolatile(access)) {
			this.ctx.vioatileFields++;
		}
		
		if (Modifier.isStatic(access)) {
			this.ctx.staticFields++;
		} else {
			this.ctx.ordinaryFields++;
		}
		
		return null;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, 
			String signature, String[] exceptions) {
		if (Modifier.isSynchronized(access)) {
			this.ctx.syncedMethods++;
		}
		
		if (Modifier.isStatic(access)) {
			this.ctx.staticMethods++;
			this.ctx.methodsImplements++;
		} else if (Modifier.isAbstract(access)) {
			this.ctx.abstractMethods++;
		} else {
			this.ctx.methodsImplements++;
		}
		
		this.clazzBuilder.methodsByName.computeIfAbsent(name, (k) -> new HashSet<>()).add(descriptor);
		return new MethodHandler(Opcodes.ASM9);
	}
	

	private final class MethodHandler extends MethodVisitor {
		private MethodHandler(int api) {
			super(api);
		}

		@Override
		public void visitLineNumber(int line, Label start) {
			ClassHandler.this.clazzBuilder.lines = Math.max(line, ClassHandler.this.clazzBuilder.lines);
		}

		@Override
		public void visitInsn(int opcode) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitVarInsn(int opcode, int varIndex) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle bSMHandle, Object... bsmArgs) {
			ClassHandler.this.ctx.insns++;
			ClassHandler.this.ctx.lambdas++;
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitLabel(Label label) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitLdcInsn(Object value) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitIincInsn(int varIndex, int increment) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			ClassHandler.this.ctx.insns++;
		}

		@Override
		public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
			ClassHandler.this.ctx.insns++;
		}
	}
}
