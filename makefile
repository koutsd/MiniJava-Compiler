all:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac ./types/*.java ./visitors/*.java Main.java

clean:
	rm -f *.class *~ ./types/*.class ./visitors/*.class

empty:
	rm -rf visitor syntaxtree llvm *.class *~ ./types/*.class ./visitors/*.class JavaCharStream.java minijava-jtb.jj MiniJavaParser* ParseException.java Token*
