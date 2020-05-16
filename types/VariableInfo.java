package types;


public class VariableInfo {
	public String name, type;
	public int offset;

	public VariableInfo(String type, String name) {
		this.type = type;
		this.name = name;
		offset = 0;
	}
	

	public int typeOffset() {
		if(this instanceof MethodInfo)
			return 8;
		else if(type.equals("int"))
			return 4;
		else if(type.equals("boolean"))
			return 1;
		
		return 8;
	}
}
