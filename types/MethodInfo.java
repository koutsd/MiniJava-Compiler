package types;

import java.util.List;
import java.util.ArrayList;


public class MethodInfo extends VariableInfo {
	public List<VariableInfo> variables, parameters;
	// public int varOffset;

	public MethodInfo(String type, String name) {
		super(type, name);
		variables = new ArrayList<>();
		parameters = new ArrayList<>();
		// varOffset = 0;
	}


	public VariableInfo getParam(String name) {
		for(VariableInfo param : parameters)
			if(param.name.equals(name))
				return param;
		// Not found
		return null;
	}


	public VariableInfo getVar(String name) {
		for(VariableInfo var : variables)
			if(var.name.equals(name))
				return var;
		// Not found
		return null;
	}

	public boolean hasVar(String name) {
		return getParam(name) != null || getVar(name) != null;
	}


	public boolean addVar(VariableInfo var) {
		if(hasVar(var.name))		// Variable already declared
			return false;

		// var.offset = varOffset;
		// varOffset += var.typeOffset();
		variables.add(var);
		return true;
	}

	
	public boolean addParam(VariableInfo var) {
		if(hasVar(var.name))		// Variable already declared
			return false;

		// var.offset = varOffset;
		// varOffset += var.typeOffset();
		parameters.add(var);
		return true;
	}
	

	// public void printOffsets() {
	// 	for(VariableInfo v : variables)		// Print all variables with their offsets
	// 		System.out.println(" - " + name + "." + v.name + " : " + v.offset);
	// }
}
