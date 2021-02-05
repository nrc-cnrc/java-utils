package ca.nrc.debug;

import com.sun.org.apache.bcel.internal.Repository;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;

import java.io.*;

public class ClassVersionChecker {
	public static void main(String[] args) throws Exception {
		ClassVersionChecker checker = new ClassVersionChecker();
		// Fails to retrieve the bytecode of the class
//		checker.checkBareBones();
//		checker.checkWithBCEL();
		checker.checkWithJavaAssist();
	}

	private void checkWithJavaAssist() {
//		try {
//			ClassPool cp = ClassPool.getDefault();
//			ClassFile cf = cp.get("java.lang.Object").getClassFile();
//			cf.write(new DataOutputStream(new FileOutputStream("Object.class")));
//		} catch (NotFoundException e) {
//			e.printStackTrace();
//		}

	}


	private void checkWithBCEL() {
		JavaClass objectClazz = Repository.lookupClass("java.lang.Object");
		System.out.println("-- ClassVersionChecker.checkWithBCEL: objectClazz="+objectClazz);
		System.out.println(objectClazz.toString());
	}

	public void checkBareBones() throws IOException {
		ClassLoader loader = ClassVersionChecker.class.getClassLoader();
		System.out.println("-- ClassVersionChecker: loader="+loader);
		try (InputStream in = loader.getResourceAsStream("ClassVersionChecker.class");
			  DataInputStream data = new DataInputStream(in)) {
			System.out.println("-- ClassVersionChecker: in="+in+", data="+data);
			if (0xCAFEBABE != data.readInt()) {
				throw new IOException("invalid header");
			}
			int minor = data.readUnsignedShort();
			int major = data.readUnsignedShort();
			System.out.println(major + "." + minor);
		}
	}


}