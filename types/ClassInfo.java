package types;

import java.util.List;
import java.util.ArrayList;


public class ClassInfo {
	public String name;
	public ClassInfo parent;
	public List<VariableInfo> variables;
	public List<MethodInfo> methods;
	public int varOffset, methodOffset;

	public ClassInfo(String name) {
		this.name = name;
		parent = null;		// No inheritance
		variables = new ArrayList<>();
		methods = new ArrayList<>();
		varOffset = 0;		// End of last variable
		methodOffset = 0;	// End of last method
	}

	public ClassInfo(String name, ClassInfo parent){	// Extended class
		this(name);
		this.parent = parent;
		varOffset = parent.varOffset;
		methodOffset = parent.methodOffset;
	}


	public boolean addVar(VariableInfo var) {
		for(VariableInfo v : variables)
			if(v.name.equals(var.name))
				return false;
		// Add to variables list
		var.offset = varOffset;
		varOffset += var.typeOffset();
		variables.add(var);
		return true;
	}


	public VariableInfo getVar(String name) {
		for(VariableInfo var : variables)				// Find variable locally
			if(var.name.equals(name))
				return var;
		// Get variable inherited from parent Class
		return (parent == null) ? null : parent.getVar(name);
	}


	public boolean addMethod(MethodInfo method) {
		for(MethodInfo m : methods)
			if(m.name.equals(method.name))
				return false;
		// Check if parent classes have this method
		MethodInfo overridden = (parent == null) ? null : parent.getMethod(method.name);
		if(overridden == null) {					// Method does not override inherited
			method.offset = methodOffset;
			methodOffset += method.typeOffset();	// pointer offset = 8
		} 
		else if(method.type.equals(overridden.type) && method.parameters.size() == overridden.parameters.size()) {
			for(int i = 0; i < method.parameters.size(); i++)
				if(method.parameters.get(i).type != overridden.parameters.get(i).type)
					return false;

			method.offset = overridden.offset;		// Inherit offset from parent
		}
		else return false;
		// Add to methods list
		methods.add(method);
		return true;
	}


	public MethodInfo getMethod(String name) {
		for(MethodInfo method : methods)
			if(method.name.equals(name))
				return method;
		// Get method inherited from parent Class
		return (parent == null) ? null : parent.getMethod(name);
	}

	
	public boolean hasAncestor(String ancestor) {
		return name.equals(ancestor) || (parent != null && parent.hasAncestor(ancestor));
	}


	public void printOffsets() {
		for(VariableInfo var : variables)		// Print all variables with their offsets
			System.out.println(name + "." + var.name + " : " + var.offset);
	
		for(MethodInfo method : methods)		// Print all methods with their offsets
			if(parent == null || parent.getMethod(method.name) == null)
				System.out.println(name + "." + method.name + " : " + method.offset);
	}
}
