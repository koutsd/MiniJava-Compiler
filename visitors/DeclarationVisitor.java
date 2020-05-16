package visitors;

import types.*;
import syntaxtree.*;
import visitor.GJDepthFirst;


public class DeclarationVisitor extends GJDepthFirst<String, Scope> {
	public SymbolTable classes;
	int line, column;

	public DeclarationVisitor() {
		classes = new SymbolTable();
		line = 1;
		column = 1;
	}

	void SemanticException(String message) throws Exception {
		String RED = "\u001B[1;31m";	// Text red bold
		String RESET = "\u001B[0m";		// Text reset
		throw new Exception(RED + "Semantic Error" + RESET + " [" + line + ":" + column + "]\n" + message);
	}

	//Visit
	public String visit(MainClass n, Scope scope) throws Exception {
		ClassInfo currClass = new ClassInfo(n.f1.accept(this, scope));
		MethodInfo currMethod = new MethodInfo("void", "main");

		classes.add(currClass);
		currMethod.addParam(new VariableInfo("String[]", n.f11.accept(this, scope)));
		currClass.addMethod(currMethod);
		currClass.methodOffset = 0;

		n.f14.accept(this, new Scope(currClass, currMethod));	// Variable Declarations
		return null;
	}


	public String visit(ClassDeclaration n, Scope scope) throws Exception {
		ClassInfo currClass = new ClassInfo(n.f1.accept(this, scope));
		if(!classes.add(currClass))
			SemanticException("Class '" + currClass.name + "' already declared");

		Scope classScope = new Scope(currClass);
		n.f3.accept(this, classScope);		// Variable Declarations
		n.f4.accept(this, classScope);		// Method Declarations
		return null;
	}


	public String visit(ClassExtendsDeclaration n, Scope scope) throws Exception {
		String parentName = n.f3.accept(this, scope);
		ClassInfo parent = classes.get(parentName);
		if(parent == null)
			SemanticException("Class '" + parentName + "' has not been declared");

		ClassInfo currClass = new ClassInfo(n.f1.accept(this, scope), parent);
		if(!classes.add(currClass))
			SemanticException("Class '" + currClass.name + "' already declared");

		Scope classScope = new Scope(currClass);
		n.f5.accept(this, classScope);		// Variable Declarations
		n.f6.accept(this, classScope);		// Method Declarations
		return null;
	}


	public String visit(VarDeclaration n, Scope scope) throws Exception {
		VariableInfo var = new VariableInfo(n.f0.accept(this, scope), n.f1.accept(this, scope));
		if(!scope.add(var))
			SemanticException("Variable '" + var.name + "' already declared in this scope");

		return null;
	}


	public String visit(MethodDeclaration n,  Scope scope) throws Exception {
		MethodInfo currMethod = new MethodInfo(n.f1.accept(this, scope), n.f2.accept(this, scope));

		Scope methodScope = new Scope(scope.currClass, currMethod);
		n.f4.accept(this, methodScope);		// Parameters
		n.f2.accept(this, methodScope);

		if(!scope.add(currMethod))
			SemanticException("Method '" + currMethod.name + "' already declared in this scope");

		n.f7.accept(this, methodScope);		// Variable Declaration
		return null;
	}


	public String visit(FormalParameter n, Scope scope) throws Exception {
		VariableInfo var = new VariableInfo(n.f0.accept(this, scope), n.f1.accept(this, scope));
		if(!scope.currMethod.addParam(var))
			SemanticException("Parameter '" + var.name + "' already declared in this scope");

		return null;
	}


	public String visit(IntegerType n, Scope scope) throws Exception {
		return "int";
	}


	public String visit(BooleanType n, Scope scope) throws Exception {
		return "boolean";
	}


	public String visit(IntegerArrayType n, Scope scope) throws Exception {
		return "int[]";
	}


	public String visit(BooleanArrayType n, Scope scope) throws Exception {
		return "boolean[]";
	}


	public String visit(Identifier n, Scope scope) throws Exception {
		line = n.f0.beginLine;
		column = n.f0.beginColumn;
		return n.f0.toString();
	}
}
