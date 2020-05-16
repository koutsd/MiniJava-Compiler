package visitors;

import types.ClassInfo;
import types.MethodInfo;
import types.VariableInfo;


class Scope {
    ClassInfo currClass;
    MethodInfo currMethod;

    Scope(ClassInfo currClass) {
        this.currClass = currClass;
        currMethod = null;
    }

    Scope(ClassInfo currClass, MethodInfo currMethod) {
        this(currClass);
        this.currMethod = currMethod;
    }


    boolean add(VariableInfo var) {     // Add var
        return (currMethod == null) ? currClass.addVar(var) : currMethod.addVar(var);
    }


    boolean add(MethodInfo method) {    // Add method
        return currClass.addMethod(method);
    }


    VariableInfo getVar(String name) {
        VariableInfo var = null;
        if(currMethod != null && (var = currMethod.getParam(name)) == null)
            var = currMethod.getVar(name);

        return (var == null) ? currClass.getVar(name) : var;
    }
    

    MethodInfo getMethod(String name) {
        return currClass.getMethod(name);
    }
}