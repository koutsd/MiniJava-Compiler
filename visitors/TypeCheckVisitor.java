package visitors;

import types.*;
import syntaxtree.*;


public class TypeCheckVisitor extends DeclarationVisitor {
	public TypeCheckVisitor(SymbolTable classes) {
		this.classes = classes;
		line = 1;
		column = 1;
	}

	// Utility
	boolean arrayType(String t) {
		return t.equals("boolean[]") || t.equals("int[]");
	}

	boolean validType(String t) {
		return t.equals("boolean") || t.equals("int") || arrayType(t) || classes.hasClass(t);
	}
	// Check if asked type (a) matches declaration type (b)
	boolean matchTypes(String a, String b) {	
		return a.equals(b) || (classes.hasClass(a) && classes.get(a).hasAncestor(b));
	}

	// Visit
	public String visit(MainClass n,  Scope scope) throws Exception {
		ClassInfo currClass = classes.get(n.f1.accept(this, scope));
		MethodInfo currMethod = currClass.getMethod("main");

		Scope mainScope = new Scope(currClass, currMethod);
		n.f14.accept(this, mainScope);		// Variable Declarations
		n.f15.accept(this, mainScope);		// Statements
		return null;
	}


	public String visit(ClassDeclaration n,  Scope scope) throws Exception {
		Scope classScope = new Scope(classes.get(n.f1.accept(this, scope)));
		n.f3.accept(this, classScope);		// Variable Declaration
		n.f4.accept(this, classScope);		// Method Declaration
		return null;
	}


	public String visit(ClassExtendsDeclaration n,  Scope scope) throws Exception {
		Scope classScope = new Scope(classes.get(n.f1.accept(this, scope)));
		n.f5.accept(this, classScope);		// Variable Declaration
		n.f6.accept(this, classScope);		// Method Declaration
		return null;
	}


	public String visit(VarDeclaration n,  Scope scope) throws Exception {
		n.f0.accept(this, scope);			// Type
		return null;
	}


	public String visit(MethodDeclaration n,  Scope scope) throws Exception {
		n.f1.accept(this, scope);			// Type
		MethodInfo currMethod = scope.getMethod(n.f2.accept(this, scope));
		
		Scope methodScope = new Scope(scope.currClass, currMethod);
		n.f4.accept(this, methodScope);		// Parameters
		n.f7.accept(this, methodScope);		// Variable Declaration
		n.f8.accept(this, methodScope);		// Statements
		// Return expression
		if(!matchTypes(n.f10.accept(this, methodScope), currMethod.type))
			SemanticException("Invalid return type in method '" + currMethod.name + "'");

		return null;
	}


	public String visit(FormalParameter n,  Scope scope) throws Exception {
		n.f0.accept(this, scope);			// Type
		return null;
	}


	public String visit(Type n,  Scope scope) throws Exception {
		String type = n.f0.accept(this, scope);
		if(!validType(type))				// if t is not valid return type
			SemanticException("Invalid type '" + type + "'");

		return type;
	}


	public String visit(AssignmentStatement n,  Scope scope) throws Exception {
		String name = n.f0.accept(this, scope);		// identifier
		VariableInfo var = scope.getVar(name);
		if(var == null)
			SemanticException("Variable '" + name + "' not declared in this scope");
		if(!matchTypes(n.f2.accept(this, scope), var.type))
			SemanticException("Invalid assignment for variable '" + name + "'");

		return null;
	}


	public String visit(ArrayAssignmentStatement n,  Scope scope) throws Exception {
		String name = n.f0.accept(this, scope);
		VariableInfo var = scope.getVar(name);
		if(var == null)
			SemanticException("Variable '" + name + "' not declared in this scope");
		if(!arrayType(var.type))
			SemanticException("Variable '" + name + "' not array type");
		if(!n.f2.accept(this,scope).equals("int"))
			SemanticException("Invalid index type in array variable '" + name + "'");
		if(!var.type.contains(n.f5.accept(this, scope)))
			SemanticException("Invalid assignment for array variable '" + name + "'");

		return null;
	}


	public String visit(IfStatement n,  Scope scope) throws Exception {
		if(!n.f2.accept(this, scope).equals("boolean"))
			SemanticException("If condition must be 'boolean'");

		n.f4.accept(this, scope);			// then statement
		n.f6.accept(this, scope);			// else statement
		return null;
	}


	public String visit(WhileStatement n,  Scope scope) throws Exception {
		if(!n.f2.accept(this, scope).equals("boolean"))
			SemanticException("While condition must be 'boolean'");

		n.f4.accept(this, scope);			// Statement
		return null;
	}


	public String visit(PrintStatement n,  Scope scope) throws Exception {
		if(!n.f2.accept(this, scope).equals("int"))
			SemanticException("Print expression must be 'int'");
		
		return null;
	}


	public String visit(AndExpression n,  Scope scope) throws Exception {
		if(!n.f0.accept(this, scope).equals("boolean") || !n.f2.accept(this, scope).equals("boolean"))
			SemanticException("Operator '&&' must have expressions of type 'boolean'");

		return "boolean";
	}


	public String visit(CompareExpression n,  Scope scope) throws Exception {
		if(!n.f0.accept(this, scope).equals("int") || !n.f2.accept(this, scope).equals("int"))
			SemanticException("Operator '<' must have expressions of type 'int'");

		return "boolean";
	}


	public String visit(PlusExpression n,  Scope scope) throws Exception {
		if(!n.f0.accept(this, scope).equals("int") || !n.f2.accept(this, scope).equals("int"))
			SemanticException("Operator '+' must have expressions of type 'int'");

		return "int";
	}


	public String visit(MinusExpression n,  Scope scope) throws Exception {
		if(!n.f0.accept(this, scope).equals("int") || !n.f2.accept(this, scope).equals("int"))
			SemanticException("Operator '-' must have expressions of type 'int'");

		return "int";
	}


	public String visit(TimesExpression n,  Scope scope) throws Exception {
		if(!n.f0.accept(this, scope).equals("int") || !n.f2.accept(this, scope).equals("int"))
			SemanticException("Operator '*' must have expressions of type 'int'");

		return "int";
	}


	public String visit(ArrayLookup n,  Scope scope) throws Exception {
		if(!n.f2.accept(this, scope).equals("int"))
			SemanticException("Array index must be 'int'");

		String type = n.f0.accept(this, scope);
		if(!arrayType(type))
			SemanticException("Invalid array type");

		return type.replace("[]", "");
	}


	public String visit(ArrayLength n,  Scope scope) throws Exception {
		if(!arrayType(n.f0.accept(this, scope)))
			SemanticException("Attribute 'length' not found");

		return "int";
	}


	public String visit(MessageSend n,  Scope scope) throws Exception {
		ClassInfo currClass = classes.get(n.f0.accept(this, scope));

		String exprList = n.f4.accept(this, scope);
		String[] parameters = (exprList == null) ? new String[0] : exprList.split(" ");

		String name = n.f2.accept(this, scope);
		MethodInfo method = currClass.getMethod(name);

		if(method == null)
			SemanticException("Class '" + currClass.name + "' has no method '" + name + "'");
		if(method.parameters.size() != parameters.length)
			SemanticException("Incorrect number of parameters in method '" + name + "'");
		// Check if each arg matches declaration
		for (int i = 0; i < parameters.length; i++)
			if(!matchTypes(parameters[i], method.parameters.get(i).type))
				SemanticException("Invalid parameter in method '" + name + "'");
		
		return method.type;
	}


	public String visit(ExpressionList n,  Scope scope) throws Exception {
		return n.f0.accept(this, scope) + n.f1.accept(this, scope);
	}


	public String visit(ExpressionTail n,  Scope scope) throws Exception {
		String tail = "";
		for(Node term : n.f0.nodes)
			tail += term.accept(this, scope);

		return tail;
	}


	public String visit(ExpressionTerm n,  Scope scope) throws Exception {
		return " " + n.f1.accept(this, scope);
	}


	public String visit(PrimaryExpression n,  Scope scope) throws Exception {
		String expr = n.f0.accept(this, scope);
		// Expr is not identifier then return type
		if(n.f0.which != 3)
			return expr;
		// Check if variable with this name is declared
		VariableInfo var = scope.getVar(expr);
		if(var == null)
			SemanticException("Variable '" + expr + "' not declared in this scope");
		// Variable type
		return var.type;
	}


	public String visit(IntegerLiteral n,  Scope scope) throws Exception {
		line = n.f0.beginLine;
		column = n.f0.beginColumn;
		return "int";
	}


	public String visit(TrueLiteral n,  Scope scope) throws Exception {
		line = n.f0.beginLine;
		column = n.f0.beginColumn;
		return "boolean";
	}


	public String visit(FalseLiteral n,  Scope scope) throws Exception {
		line = n.f0.beginLine;
		column = n.f0.beginColumn;
		return "boolean";
	}


	public String visit(ThisExpression n,  Scope scope) throws Exception {
		line = n.f0.beginLine;
		column = n.f0.beginColumn;
		return scope.currClass.name;
	}


	public String visit(IntegerArrayAllocationExpression n,  Scope scope) throws Exception {
		if(!n.f3.accept(this, scope).equals("int"))
			SemanticException("Array allocation size must be 'int'");

		return "int[]";
	}


	public String visit(BooleanArrayAllocationExpression n,  Scope scope) throws Exception {
		if(!n.f3.accept(this, scope).equals("int"))
			SemanticException("Array allocation size must be 'int'");

		return "boolean[]";
	}

	
	public String visit(AllocationExpression n,  Scope scope) throws Exception {
		String type = n.f1.accept(this, scope);
		if(!validType(type))
			SemanticException("Invalid data type '" + type + "'");

		return type;
	}


	public String visit(NotExpression n,  Scope scope) throws Exception {
		if(!n.f1.accept(this, scope).equals("boolean"))
			SemanticException("Not expression must be 'boolean'");
		
		return "boolean";
	}


	public String visit(BracketExpression n,  Scope scope) throws Exception {
		return n.f1.accept(this, scope);
	}
}
