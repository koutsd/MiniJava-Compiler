package visitors;

import types.*;
import syntaxtree.*;
import visitor.GJDepthFirst;

// Visitor that returns miniJava name/type of visited expression
class MiniJava_Visitor extends GJDepthFirst<String, Scope> {
    SymbolTable classes;

    MiniJava_Visitor(SymbolTable classes) {
        this.classes = classes;
    }

    // Visit
    public String visit(AndExpression n,  Scope scope) throws Exception {
        return "boolean";
    }

    public String visit(CompareExpression n,  Scope scope) throws Exception {
        return "boolean";
    }

    public String visit(PlusExpression n,  Scope scope) throws Exception {
        return "int";
    }

    public String visit(MinusExpression n,  Scope scope) throws Exception {
        return "int";
    }

    public String visit(TimesExpression n,  Scope scope) throws Exception {
        return "int";
    }

    public String visit(ArrayLookup n,  Scope scope) throws Exception {
        return n.f0.accept(this, scope).replace("[]", "");
    }

    public String visit(ArrayLength n,  Scope scope) throws Exception {
        return "int";
    }

    public String visit(MessageSend n,  Scope scope) throws Exception {
        return classes.get(n.f0.accept(this, scope)).getMethod(n.f2.accept(this, scope)).type;
    }

    public String visit(PrimaryExpression n,  Scope scope) throws Exception {
        String expr = n.f0.accept(this, scope);
        return (n.f0.which == 3) ? scope.getVar(expr).type : expr;
    }

    public String visit(IntegerLiteral n,  Scope scope) throws Exception {
        return "int";
    }

    public String visit(TrueLiteral n,  Scope scope) throws Exception {
        return "boolean";
    }

    public String visit(FalseLiteral n,  Scope scope) throws Exception {
        return "boolean";
    }

    public String visit(Identifier n, Scope scope) throws Exception {
        return n.f0.toString();
    }

    public String visit(ThisExpression n,  Scope scope) throws Exception {
        return scope.currClass.name;
    }

    public String visit(IntegerArrayAllocationExpression n,  Scope scope) throws Exception {
        return "int[]";
    }

    public String visit(BooleanArrayAllocationExpression n,  Scope scope) throws Exception {
        return "boolean[]";
    }

    public String visit(AllocationExpression n,  Scope scope) throws Exception {
        return n.f1.accept(this, scope);
    }

    public String visit(NotExpression n,  Scope scope) throws Exception {
        return "boolean";
    }

    public String visit(BracketExpression n,  Scope scope) throws Exception {
        return n.f1.accept(this, scope);
    }
}
