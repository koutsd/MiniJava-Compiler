package visitors;

import java.io.*;
import java.util.*;
import types.*;
import syntaxtree.*;
import visitor.GJDepthFirst;


public class LLVM_Visitor extends GJDepthFirst<String, Scope> {
	public SymbolTable classes;

	MiniJava_Visitor javaVisitor;		// Visitor that returns miniJava name/type of visited expression
	StringBuilder buffer;				// Holds generated LLVM code
	int registerCount, labelCount;		// Ensure unique registers and labels


	public LLVM_Visitor(SymbolTable classes) {
		this.classes = classes;
		javaVisitor = new MiniJava_Visitor(classes);
		buffer = new StringBuilder();
		resetCounters();
	}

	public void emit(String dest) throws Exception {
		if(dest == null)		// Print buffer in console
			System.out.println(buffer);
		else {					// Store buffer in file
			BufferedWriter destFile = new BufferedWriter(new FileWriter(dest));
			destFile.write(buffer.toString());
			destFile.flush();
			destFile.close();
		}
	}

	// Utility
	String newRegister() {
		return "%_" + registerCount++;
	}

	String newLabel() {
		return "LBL_" + labelCount++;
	}

	void resetCounters() {
		labelCount = 1;
		registerCount = 1;
	}
	// Return LLVM Register type from miniJava Type
	String typeToLLVM (String type) {
		switch(type) {
			case "int":
				return "i32";
			case "boolean":
				return "i1";
			case "int[]":
				return "i32*";
			case "boolean[]":
				return "i1*";
			case "String[]":
				return "i8**";
			default:
				return "i8*";
		}
	}
	// Load Declared variable with given name in scope
	String getVariableRegister(VariableInfo var, Scope scope) {
		String retReg = "%" + var.name;
		if (!scope.currMethod.hasVar(var.name)) {		// Variable declared in class scope
			String ptrReg = newRegister();
			retReg = newRegister();
			buffer.append(
				"\n\t" + ptrReg + " = getelementptr i8, i8* %this, i32 " + (var.offset + 8) +
				"\n\t" + retReg + " = bitcast i8* " + ptrReg + " to " + typeToLLVM(var.type) + "*"
			);
		}

		return retReg;
	}
	// Load array length stored in first 4 bytes of array
	String loadArrayLength(String arrReg, String arrType) {
		String tempReg = arrReg;
		if(arrType.equals("boolean[]")) {		// If array is i1* cast to i32* to get length
			tempReg = newRegister();
			buffer.append("\n\t" + tempReg + " = bitcast i1* " + arrReg + " to i32*");
		}
		// Load length value from array
		String lengthReg = newRegister();
		buffer.append("\n\t" + lengthReg + " = load i32, i32* " + tempReg);
		return lengthReg;
	}

	void createVtables() {
		ClassInfo[] classList = classes.values();
		// Create vtable for each class
		for(ClassInfo currClass : classList) {
			StringBuilder tempBuffer = new StringBuilder();
			List<String> allMethods = new ArrayList<>();
			buffer.append("@." + currClass.name + "_vtable = global [" + (currClass.methodOffset / 8) + " x i8*] [");
			// Find all methods of class inherited or not
			for(ClassInfo c = currClass; (c != null && !c.name.equals(classList[0].name)); c = c.parent) {
				StringBuilder temp = new StringBuilder();
				// For all methds of current class (c)
				for (MethodInfo method : c.methods)
					if(!allMethods.contains(method.name)) {		// Check if methods already in vtable
						allMethods.add(method.name);
						temp.append("\n\ti8* bitcast (" + typeToLLVM(method.type) + " (i8*");

						for(VariableInfo param : method.parameters)
							temp.append(", " + typeToLLVM(param.type));

						temp.append(")* @" + c.name + "." + method.name + " to i8*),");
					}

				tempBuffer.insert(0, temp);
			}

			int length = tempBuffer.length();
			if(length > 0 && tempBuffer.charAt(length - 1) == ',')
				tempBuffer.setCharAt(length - 1, '\n');

			buffer.append(tempBuffer + "]\n\n");
		}
	}

	void createUtility() {
		buffer.append(
			"; Utility" +
			"\ndeclare i8* @calloc(i32, i32)" +
			"\ndeclare i32 @printf(i8*, ...)" +
			"\ndeclare void @exit(i32)" +
			"\n" +
			"\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"" +
			"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"" +
			"\n" +		// Print integer
			"\ndefine void @print_int(i32 %i) {" +
			"\n\t%_str = bitcast [4 x i8]* @_cint to i8*" +
			"\n\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)" +
			"\n\tret void" +
			"\n}" +
			"\n" +		// Print Out of bounds error and exit
			"\ndefine void @throw_oob() {" +
			"\n\t%_str = bitcast [15 x i8]* @_cOOB to i8*" +
			"\n\tcall i32 (i8*, ...) @printf(i8* %_str)" +
			"\n\tcall void @exit(i32 1)" +
			"\n\tret void" +
			"\n}"
		);
	}

	// Visit
	public String visit(Goal n, Scope scope) throws Exception {
		createVtables();
		createUtility();

		buffer.append("\n\n; Method Declarations");
		n.f0.accept(this, scope);		// Main Class
		n.f1.accept(this, scope);		// Class Declarations
		return null;
	}


	public String visit(MainClass n,  Scope scope) throws Exception {
		ClassInfo currClass = classes.get(n.f1.accept(javaVisitor, scope));
		MethodInfo currMethod = currClass.getMethod("main");

		resetCounters();
		buffer.append(
			"\ndefine i32 @main() {" +
			"\n\t%" + n.f11.accept(javaVisitor, scope) + " = alloca i8**"
		);

		Scope mainScope = new Scope(currClass, currMethod);
		n.f14.accept(this, mainScope);		// Variable Declarations
		n.f15.accept(this, mainScope);		// Statements

		buffer.append(
			"\n\t; Return" +
			"\n\tret i32 0" +
			"\n}\n"
		);

		return null;
	}


	public String visit(ClassDeclaration n,  Scope scope) throws Exception {
		Scope classScope = new Scope(classes.get(n.f1.accept(javaVisitor, scope)));
		n.f4.accept(this, classScope);		// Method Declaration
		return null;
	}


	public String visit(ClassExtendsDeclaration n,  Scope scope) throws Exception {
		Scope classScope = new Scope(classes.get(n.f1.accept(javaVisitor, scope)));
		n.f6.accept(this, classScope);		// Method Declaration
		return null;
	}


	public String visit(VarDeclaration n,  Scope scope) throws Exception {
		VariableInfo var = scope.getVar(n.f1.accept(javaVisitor, scope));
		buffer.append("\n\t%" + var.name + " = alloca " + typeToLLVM(var.type));
		return null;
	}


	public String visit(MethodDeclaration n,  Scope scope) throws Exception {
		MethodInfo currMethod = scope.getMethod(n.f2.accept(javaVisitor, scope));
		String retType = typeToLLVM(currMethod.type);

		resetCounters();
		buffer.append("\n\ndefine " + retType + " @" + scope.currClass.name + "." + currMethod.name + "(i8* %this");

		for (VariableInfo param : currMethod.parameters)
			buffer.append(", " + typeToLLVM(param.type) + " %." + param.name);

		buffer.append(") {");

		Scope methodScope = new Scope(scope.currClass, currMethod);
		n.f4.accept(this, methodScope);		// Parameters
		n.f7.accept(this, methodScope);		// Variable Declarations
		n.f8.accept(this, methodScope);		// Statements

		String retReg = n.f10.accept(this, methodScope);
		buffer.append(
			"\n\t; Return" +
			"\n\tret " + retType + " " + retReg +
			"\n}\n"
		);

		return null;
	}


	public String visit(FormalParameter n,  Scope scope) throws Exception {
		VariableInfo param = scope.currMethod.getParam(n.f1.accept(javaVisitor, scope));
		String regType = typeToLLVM(param.type);

		buffer.append(
			"\n\t%" + param.name + " = alloca " + regType +
			"\n\tstore " + regType + " %." + param.name + ", " + regType + "* %" + param.name
		);

		return null;
	}


	public String visit(AssignmentStatement n, Scope scope) throws Exception {
		buffer.append("\n\t; Assignment Statement");

		VariableInfo var = scope.getVar(n.f0.accept(javaVisitor, scope));
		String regType = typeToLLVM(var.type);

		String varReg = getVariableRegister(var, scope);
		String exprReg = n.f2.accept(this, scope);
		buffer.append("\n\tstore " + regType + " " + exprReg + ", " + regType + "* " + varReg);
		return null;
	}


	public String visit(ArrayAssignmentStatement n, Scope scope) throws Exception {
		buffer.append("\n\t; Array Assignment Statement");

		VariableInfo var = scope.getVar(n.f0.accept(javaVisitor, scope));
		String regType = typeToLLVM(var.type.replace("[]", ""));

		String label1 = newLabel();
		String label2 = newLabel();
		String label3 = newLabel();

		String indexReg = n.f2.accept(this, scope);
		String checkReg1 = newRegister();
		buffer.append(					// Check if index is smaller than 0
			"\n\t" + checkReg1 + " = icmp slt i32 " + indexReg + ", 0" +
			"\n\tbr i1 "+ checkReg1 + ", label %"+ label2 + ", label %" + label1 +
			"\n" + label1 + ":\t; Check Bounds next"
		);

		String varReg = getVariableRegister(var, scope);
		String arrReg = newRegister();
		buffer.append("\n\t" + arrReg + " = load " + regType + "*, " + regType + "** " + varReg);

		String lengthReg = loadArrayLength(arrReg, var.type);
		String checkReg2 = newRegister();
		buffer.append(					// Check if index exceeds array length
			"\n\t" + checkReg2 + " = icmp slt i32 " + indexReg + ", " + lengthReg +
			"\n\tbr i1 "+ checkReg2 + ", label %"+ label3 + ", label %" + label2 +
			"\n" + label2 + ":\t; Out of Bounds" +	// Throw out of bounds error
			"\n\tcall void @throw_oob()" +
			"\n\tbr label %" + label3 +
			"\n" + label3 + ":\t; In Bounds"		// Assign new Array
		);

		String exprReg = n.f5.accept(this, scope);
		String offsetReg = newRegister();
		String ptrReg = newRegister();
		buffer.append(
			"\n\t" + offsetReg + " = add i32 " + indexReg + ", " + (regType.equals("i1") ? 4 : 1) +
			"\n\t" + ptrReg + " = getelementptr " + regType + ", " + regType + "* " + arrReg + ", i32 " + offsetReg +
			"\n\tstore " + regType + " " + exprReg + ", " + regType + "* " + ptrReg
		);

		return null;
	}


	public String visit(IfStatement n, Scope scope) throws Exception {
		buffer.append("\n\t; If Statement");

		String label1 = newLabel();
		String label2 = newLabel();
		String label3 = newLabel();
		// If
		String condReg = n.f2.accept(this, scope);
		buffer.append(
			"\n\tbr i1 " + condReg + ", label %" + label1 + ", label %" + label2 +
			"\n" + label1 + ":\t; Then"
		);
		// Then
		n.f4.accept(this, scope);
		buffer.append(
			"\n\tbr label %" + label3 +
			"\n" + label2 + ":\t; Else"
		);
		// Else
		n.f6.accept(this, scope);
		buffer.append(
			"\n\tbr label %" + label3 +
			"\n" + label3 + ":\t; Exit If"
		);

		return null;
	}


	public String visit(WhileStatement n, Scope scope) throws Exception {
		buffer.append("\n\t; While Statement");

		String label1 = newLabel();
		String label2 = newLabel();
		String label3 = newLabel();

		buffer.append(
			"\n\tbr label %" + label1 +
			"\n" + label1 + ":\t; Repeat"		// Repeat
		);
		// While Conndition
		String condReg = n.f2.accept(this, scope);
		buffer.append(
			"\n\tbr i1 " + condReg + ", label %" + label2 + ", label %" + label3 +
			"\n" + label2 + ":\t; Do"
		);
		// Do
		n.f4.accept(this, scope);
		buffer.append(
			"\n\tbr label %" + label1 +
			"\n" + label3 + ":\t; Exit loop"		// Break
		);

		return null;
	}


	public String visit(PrintStatement n, Scope scope) throws Exception {
		buffer.append("\n\t; Print Statement");

		String exprReg = n.f2.accept(this, scope);
		buffer.append("\n\tcall void (i32) @print_int(i32 " + exprReg + ")");
		return null;
	}


	public String visit(AndExpression n, Scope scope) throws Exception {
		String label1 = newLabel();
		String label2 = newLabel();
		String label3 = newLabel();
		String label4 = newLabel();

		String exprReg1 = n.f0.accept(this, scope);
		buffer.append(
			"\n\tbr label %" + label1 +
			"\n" + label1 + ":\t; And Expression" +
			"\n\tbr i1 " + exprReg1 + ", label %" + label2 + ", label %" + label4 +
			"\n" +label2 + ":\t; And Expression"
		);

		String exprReg2 = n.f2.accept(this, scope);
		String retReg = newRegister();
		buffer.append(
			"\n\tbr label %" + label3 +
			"\n" +label3 + ":\t; And Expression" +
			"\n\tbr label %" + label4 +
			"\n" + label4 + ":\t; And Expression" +
			"\n\t" + retReg + " = phi i1 [0, %" + label1 + "], [" + exprReg2 + ", %" + label3 + "]"
		);

		return retReg;
	}


	public String visit(CompareExpression n, Scope scope) throws Exception {
		String exprReg1 = n.f0.accept(this, scope);
		String exprReg2 = n.f2.accept(this, scope);
		String retReg = newRegister();

		buffer.append( "\n\t" + retReg + " = icmp slt i32 " + exprReg1 + ", " + exprReg2);
		return retReg;
	}


	public String visit(PlusExpression n, Scope scope) throws Exception {
		String exprReg1 = n.f0.accept(this, scope);
		String exprReg2 = n.f2.accept(this, scope);
		String retReg = newRegister();

		buffer.append("\n\t" + retReg + " = add i32 " + exprReg1 + ", " + exprReg2);
		return retReg;
	}


	public String visit(MinusExpression n, Scope scope) throws Exception {
		String exprReg1 = n.f0.accept(this, scope);
		String exprReg2 = n.f2.accept(this, scope);
		String retReg = newRegister();

		buffer.append("\n\t" + retReg + " = sub i32 " + exprReg1 + ", " +exprReg2);
		return retReg;
	}


	public String visit(TimesExpression n, Scope scope) throws Exception {
		String exprReg1 = n.f0.accept(this, scope);
		String exprReg2 = n.f2.accept(this, scope);
		String retReg = newRegister();

		buffer.append("\n\t" + retReg + " = mul i32 " + exprReg1 + ", " + exprReg2);
		return retReg;
	}


	public String visit(ArrayLookup n, Scope scope) throws Exception {
		String type = n.f0.accept(javaVisitor, scope);
		String regType = typeToLLVM(type.replace("[]", ""));

		String label1 = newLabel();
		String label2 = newLabel();
		String label3 = newLabel();

		String checkReg1 = newRegister();
		String indexReg = n.f2.accept(this, scope);
		buffer.append(					// Check ig index is smaller than 0
			"\n\t" + checkReg1 + " = icmp slt i32 " + indexReg + ", 0" +
			"\n\tbr i1 "+ checkReg1 + ", label %"+ label2 + ", label %" + label1 +
			"\n" + label1 + ":\t; Check Bounds next"
		);

		String arrReg = n.f0.accept(this, scope);
		String lengthReg = loadArrayLength(arrReg, type);
		String checkReg2 = newRegister();
		String offsetReg = newRegister();
		String ptrReg = newRegister();
		String retReg = newRegister();

		buffer.append(					// Check if index exceeds array length
			"\n\t" + checkReg2 + " = icmp slt i32 " + indexReg + ", " + lengthReg +
			"\n\tbr i1 "+ checkReg2 + ", label %"+ label3 + ", label %" + label2 +
			"\n" + label2 + ":\t; Out of Bounds" +		// Throw out of bounds error
			"\n\tcall void @throw_oob()" +
			"\n\tbr label %" + label3 +
			"\n" + label3 + ":\t; In Bounds" +			// Get value of array at index
			"\n\t" + offsetReg + " = add i32 " + indexReg + ", " + (regType.equals("i1") ? 4 : 1) +
			"\n\t" + ptrReg + " = getelementptr " + regType + ", " + regType + "* " + arrReg + ", i32 " + offsetReg +
			"\n\t" + retReg + " = load " + regType + ", " + regType + "* " + ptrReg
		);

		return retReg;
	}


	public String visit(ArrayLength n, Scope scope) throws Exception {
		String arrReg = n.f0.accept(this, scope);
		String regType = n.f0.accept(javaVisitor, scope);
		return loadArrayLength(arrReg, regType);
	}


	public String visit(MessageSend n,  Scope scope) throws Exception {
		MethodInfo method = classes.get(n.f0.accept(javaVisitor, scope)).getMethod(n.f2.accept(javaVisitor, scope));
		String regType = typeToLLVM(method.type);

		String exprReg =  n.f0.accept(this, scope);
		String args = (n.f4.node == null) ? "" : n.f4.accept(this, scope);
		String classPtrReg = newRegister();
		String classReg = newRegister();
		String ptrReg = newRegister();
		String methodPtrReg = newRegister();
		String methodReg = newRegister();
		String retReg = newRegister();

		buffer.append(
			"\n\t" + classPtrReg + " = bitcast i8* " + exprReg + " to i8***" +
			"\n\t" + classReg + " = load i8**, i8*** " + classPtrReg +
			"\n\t" + ptrReg + " = getelementptr i8*, i8** " + classReg + ", i32 " + (method.offset / 8) +
			"\n\t" + methodPtrReg + " = load i8*, i8** " + ptrReg +
			"\n\t" + methodReg + " = bitcast i8* " + methodPtrReg + " to " + regType + " (i8*"
		);
		
		for(VariableInfo param : method.parameters)
			buffer.append(", " + typeToLLVM(param.type));

		buffer.append(
				")*" +
			"\n\t" + retReg + " = call " + regType + " " + methodReg + "(i8* " + exprReg + args + ")"
		);

		return retReg;
	}


	public String visit(ExpressionList n,  Scope scope) throws Exception {
    	String exprReg = n.f0.accept(this, scope);
    	String regType = typeToLLVM(n.f0.accept(javaVisitor, scope));
		return ", " + regType + " " + exprReg + n.f1.accept(this, scope);
	}


	public String visit(ExpressionTail n,  Scope scope) throws Exception {
		String tail = "";
		for(Node term : n.f0.nodes)
			tail += term.accept(this, scope);

		return tail;
	}


	public String visit(ExpressionTerm n,  Scope scope) throws Exception {
    	String exprReg = n.f1.accept(this, scope);
    	String regType = typeToLLVM(n.f1.accept(javaVisitor, scope));
		return ", " + regType + " " + exprReg;
	}


	public String visit(IntegerLiteral n, Scope scope) throws Exception {
		return n.f0.toString();
	}


	public String visit(TrueLiteral n, Scope scope) throws Exception {
		return "1";
	}


	public String visit(FalseLiteral n, Scope scope) throws Exception {
		return "0";
	}


	public String visit(Identifier n, Scope scope) throws Exception {
		VariableInfo var = scope.getVar(n.f0.toString());
		String regType = typeToLLVM(var.type);

		String ptrReg = getVariableRegister(var, scope);
		String retReg = newRegister();
		buffer.append("\n\t" + retReg + " = load " + regType + ", " + regType + "* " + ptrReg);
		return retReg;
	}


	public String visit(ThisExpression n, Scope scope) throws Exception {
		return "%this";
	}


	public String visit(ArrayAllocationExpression n, Scope scope) throws Exception {
		String regType = typeToLLVM(n.f0.accept(javaVisitor, scope));
		boolean isBoolean = regType.equals("i1*");

		String label1 = newLabel();
		String label2 = newLabel();

		String lengthReg = n.f0.accept(this, scope);
		String checkReg = newRegister();
		String offsetReg = newRegister();
		String allocReg = newRegister();
		String arrReg = newRegister();

		buffer.append(					// Check if length is smaller than 0
			"\n\t" + checkReg + " = icmp slt i32 " + lengthReg + ", 0" +
			"\n\tbr i1 "+ checkReg + ", label %"+ label1 + ", label %" + label2 +
			"\n" + label1 + ":\t; Out of Bounds" +		// Throw out of bounds error
			"\n\tcall void @throw_oob()" +
			"\n\tbr label %" + label2 +
			"\n" + label2 + ":\t; In Bounds" +			// Allocate new array
			"\n\t" + offsetReg + " = add i32 " + lengthReg + ", " + (isBoolean ? 4 : 1) +
			"\n\t" + allocReg + " = call i8* @calloc(i32 " + (isBoolean ? 1 : 4) + ", i32 " + offsetReg + ")" +
			"\n\t" + arrReg + " = bitcast i8* " + allocReg + " to i32*" +
			"\n\tstore i32 " + lengthReg + ", i32* " + arrReg
		);
		
		String retReg = arrReg;
		if(isBoolean) {
			retReg = newRegister();
			buffer.append("\n\t" + retReg + " = bitcast i32* " + arrReg + " to i1*");
		}

		return retReg;
	}


	public String visit(IntegerArrayAllocationExpression n, Scope scope) throws Exception {
		return n.f3.accept(this, scope);
	}


	public String visit(BooleanArrayAllocationExpression n, Scope scope) throws Exception {
		return n.f3.accept(this, scope);
	}


	public String visit(AllocationExpression n, Scope scope) throws Exception {
		ClassInfo c = classes.get(n.f1.accept(javaVisitor, scope));

		String retReg = newRegister();
		String ptrReg = newRegister();
		String allocReg = newRegister();
		buffer.append(
			"\n\t" + retReg + " = call i8* @calloc(i32 1, i32 " + (c.varOffset + 8) + ")" +
			"\n\t" + ptrReg + " = bitcast i8* " + retReg + " to i8***" +
			"\n\t" + allocReg + " = getelementptr [" + (c.methodOffset / 8) + " x i8*], [" + (c.methodOffset / 8) + " x i8*]* @." + c.name + "_vtable, i32 0, i32 0" +
			"\n\tstore i8** " + allocReg + ", i8*** " + ptrReg
		);
		
		return retReg;
	}


	public String visit(NotExpression n, Scope scope) throws Exception {
		String exprReg = n.f1.accept(this, scope);
		String retReg = newRegister();
		buffer.append("\n\t" + retReg + " = xor i1 1, " + exprReg);
		return retReg;
	}


	public String visit(BracketExpression n, Scope scope) throws Exception {
		return n.f1.accept(this, scope);
	}
}
