package types;

import java.util.*;


public class SymbolTable {
	Map<String, ClassInfo> map;

	public SymbolTable() {
		map = new LinkedHashMap<>();
	}


	public ClassInfo get(String name) {
		return map.get(name);
	}


	public boolean hasClass(String name) {
		return map.containsKey(name);
	}

	
	public boolean add(ClassInfo newClass) {
		if(hasClass(newClass.name))
			return false;

		map.put(newClass.name, newClass);
		return true;
	}


	public ClassInfo[] values() {
		return map.values().toArray(new ClassInfo[map.size()]);
	}


	public void printOffsets() {
		ClassInfo[] classes = Arrays.copyOfRange(values(), 1, map.size());		// Ignore main class
		for(ClassInfo currClass : classes)	
			currClass.printOffsets();
	
		System.out.println("");
	}
}
