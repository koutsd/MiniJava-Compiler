import java.io.*;
import visitors.*;
import syntaxtree.*;


public class Main {
	public static void main(String[] args) {
		if(args.length == 0) {
			System.err.println("Usage: java Main [file1] [file2] ... [fileN]");
			System.exit(1);
		}

		for(String file : args)
			try {
				System.out.println("\u001B[1;4m" + file + "\u001B[0m:");

				FileInputStream stream = new FileInputStream(file);
				MiniJavaParser parser = new MiniJavaParser(stream);
				Node root = parser.Goal();
				// Create Symbol table and check declarations
				DeclarationVisitor v1 = new DeclarationVisitor();
				root.accept(v1, null);
				// Check types
				TypeCheckVisitor v2 = new TypeCheckVisitor(v1.classes);
				root.accept(v2, null);
				// Print Offsets
				v2.classes.printOffsets();

				// Translate to LLVM
				LLVM_Visitor v3 = new LLVM_Visitor(v2.classes);
				root.accept(v3, null);

				File dir = new File("llvm");
				if (!dir.exists()) dir.mkdirs();	// if llvm directory does not exist create it
				// Store generated LLVM code to ./llvm/[fileName].ll
				v3.emit("llvm/" + file.substring(file.lastIndexOf("/") + 1, file.indexOf(".")) + ".ll");
				// v3.emit(null);
				stream.close();
			}
			catch(Exception e) {
				System.err.println(e.getMessage() + "\n");
			}
	}
}
