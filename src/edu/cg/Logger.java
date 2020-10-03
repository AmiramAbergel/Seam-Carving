package edu.cg;

public interface Logger {
	void log(String s);
	
	default void log(Object obj) {
		log(obj == null ? "null" : obj.toString());
	}
}
