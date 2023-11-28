import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.HashSet;
import java.io.File;
import java.io.FileNotFoundException; 
import java.util.Scanner; 
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Parser {

	enum Type {
		BOOL,
		INT,
		CHAR,
		WRONG
	}

	// managing scope in functions?
	// declaring a new function for parser
	// Subject: Names...
	private static Pattern removeWhiteSpace = Pattern.compile(".+");
	private String javaFile;

	private Pattern prolog = Pattern.compile("(^(Dear)( [BICS]([a-zA-Z]+), )+|To whom it may concern, )");
	// change epilog?
	private Pattern epilog = Pattern.compile("((Best,) ([BICS]([a-zA-Z]+)))$");
	private Pattern sentence = Pattern.compile(".+?(\\.|!)");
	// THIS MIGHT CAUSE BUGS?
	private Pattern statement = Pattern.compile("(.+)[^.!]");
	private Pattern equality = Pattern.compile("^(.+) says (.+)$");
	private Pattern varAssign = Pattern.compile("^([a-zA-Z]+) said (.+)$");
	
	private Pattern intInc = Pattern.compile("^piggybacking off of (.+)$");
	private Pattern intAdd = Pattern.compile("^(.+) piggybacking off of (.+)$");
	private Pattern intDec = Pattern.compile("^drill down on (.+)$");
	private Pattern intSub = Pattern.compile("^(.+) drill down on (.+)$");
	private Pattern intMult = Pattern.compile("^(.+) joins forces with (.+)$");
	private Pattern intDiv = Pattern.compile("^(.+) leverages (.+)$");
	
	private Pattern boolOR = Pattern.compile("^(.+) or (.+)$");
	private Pattern boolAND = Pattern.compile("^(.+) and (.+)$");
	private Pattern boolNOT = Pattern.compile("^not (.+)$");
	private Pattern conditional = Pattern.compile("^Suppose (.+), then (.+); otherwise, (.+)$");
	private Pattern loop = Pattern.compile("^Keep (.+) in the loop regarding: (.+).");
	
	private Pattern var = Pattern.compile("([BICS]([a-zA-Z]+))");
	private Pattern boolVar = Pattern.compile("^B.+$");
	private Pattern intVar = Pattern.compile("^I.+$");
	private Pattern charVar = Pattern.compile("^C.+$");
	
	private Pattern boolVal = Pattern.compile("^yep$|^nope$");
	private Pattern intVal = Pattern.compile("^\\d+$");
	private Pattern charVal = Pattern.compile("^[a-zA-Z]$");
	
	private HashSet<String> ints;
	private HashSet<String> strings;
	private HashSet<String> bools;
	private HashSet<String> chars;
	
	Parser(String filename) {
		ints = new HashSet<>();
		strings = new HashSet<>();
		bools = new HashSet<>();
		chars = new HashSet<>();
		String text = readFile(filename);

		// class name is file name
		javaFile =  "public class " + filename.substring(0,filename.length()-6) + "{\n";

		if (text == null) {
			// prints file name -- BUG?
			System.out.println("Invalid input file " + filename);
			return;
		}

		System.out.println(text); // for debugging purposes

		try {
			javaFile += parseProlog(text);
			System.out.println(javaFile); // for debugging
			String body = getBody(text);
			System.out.println(body); // for debugging
			parseBody(body);
		}
		catch (SyntaxError e){
			System.out.println(e.getMessage());
		}
		// Final line to end class def
		javaFile += "\n}";
	}
	
	// main() code adapted from Parser.java from the class resources
	public static void main (String[] args) {
		if (args.length == 0) {
			// if no file is supplied, return
			System.out.println("Please input a file name.");
			return;
		}
		Parser parser = new Parser(args[0]);
    }
    
	/*
	 * Get the body of the email
	 */
    private String getBody(String text){
        Matcher pm = prolog.matcher(text);
        Matcher em = epilog.matcher(text);
        String p = "";
        String e = "";

        if(pm.find()) p = pm.group();
        if(em.find()) e = em.group();

		// substract out prologue and epilogue to get body of text
        text = text.substring(p.length(), text.length() - 1);
        text = text.substring(0, text.length() - e.length());

        return text;
    }
    
    private String parseBody(String text) throws SyntaxError {
        var sentences = this.sentence.matcher(text);
		String body = "";
		System.out.println(text);
		int idx = 0;
		try {
			while(sentences.find()) {
				String sentence = sentences.group();
				System.out.println(sentence.trim()); // debugging
				parse(sentence);
			}
		}
		catch(SyntaxError e){
			throw new SyntaxError(
				"Hey, sorry to bother you with this but" +
				" we found the following error in sentence" 
				+ (idx + 1) + ":" +
				""
			);
		}
        return body;
    }

    /*
	 * Read the file into a string.
	 */
    private static String readFile(String filename){
        try {
			String text = Files.readString(Paths.get(filename));
			Matcher m = removeWhiteSpace.matcher(text);
			String t = "";
			while(m.find()){
				t += m.group().trim() + " ";
			}
			System.out.println(t);
			return t;
		} catch (IOException e) {
			return null;
		}
    }
	
	/*
	 * transpile the prologue.
	 */
	private String parseProlog(String text) throws SyntaxError{
		Matcher prologMatch = prolog.matcher(text);

		// No match found... throw error
		if(!prologMatch.find()){
			throw new SyntaxError("AHHH");
		}
		
		String functionStart = "";
		// Check that the correct number of command line arguments was supplied
		String opening = prologMatch.group();
		var var = this.var.matcher(opening); // get individual variable names from comma-separated list
		int idx = 0;

		String body = "";
		System.out.println(opening); // debugging

		while(var.find()){
			String curVar = var.group();

			if (curVar == "To whom it may concern,"){
				break;
			}

			switch (findVarType(curVar)){
				case BOOL:
					bools.add(curVar);
					body += "Boolean " + curVar + " = Boolean.valueOf(args[" + idx+ "]);\n";
				break;
				case INT:
					ints.add(curVar);
					body += "Integer " + curVar + " = Integer.valueOf(args[" + idx + "]);\n";
				break;
				case CHAR:
					chars.add(curVar);
					body += "Char " + curVar + " = Character.valueOf(args[" + idx + "]);\n";
				break;
				case WRONG:
					throw new SyntaxError(
						"We took issue with your addressing of " + curVar + "\n"
						+ "Your email must be addressed to person(s) with name(s) starting with B, I, or S.\n"
						+ "Please do better.\n"
						+ "Sincerely, the email-team."
					);
			}
			idx++;
		}

		functionStart += "public static void main(String[] args) {\n";
		functionStart += "if(args.length != " + (idx) + "){\n";
		functionStart += "System.out.println(";
		functionStart += "\"There was an error encountered in delivering the contents of your email\");\n";
		functionStart += "System.out.println(\"(this means that there were too few arguments supplied)\");\n";
		functionStart += "}";
		return functionStart + body + "\n}";
	}
	
	private void parse(String cmd) throws SyntaxError {
		Matcher m = statement.matcher(cmd);
		boolean match = false;
		
		if (m.find()) {
			String expression = m.group();
			System.out.println(expression);
			match = varAssign(expression);

			if (!match) match = parseLoop(expression);
			if (!match) match = parseEquality(expression);
			if (!match) match = parseIncrement(expression);
			if (!match) match = parseAdd(expression);
			if (!match) match = parseDecrement(expression);
			if (!match) match = parseSubtract(expression);
			if (!match) match = parseMultiply(expression);
			if (!match) match = parseDivide(expression);
			if (!match)	System.out.println("Syntax error.");
		}
		else {
			System.out.println("Syntax error: missing period.");
			throw new SyntaxError("Missing period.");
		}
	}

	private boolean varAssign(String expression) {
		Matcher assignment = varAssign.matcher(expression);

		
		if (assignment.find()) {
			String var = assignment.group(1);
			String val = assignment.group(2);

			// for debugging purposes:
			// System.out.println("Printing beginning here:");
			// System.out.println(assignment.group(1));
			// System.out.println(assignment.group(2));
			
			Type type = findAssignmentType(var, val);

			// System.out.println(type); // DEBUGGING

			// add declaration and assignment to output file
			switch(type) {
				case WRONG:
					return false;

				case BOOL:
					bools.add(var);
					System.out.printf("Assigning bool value of %s to variable name %s\n", val, var);
					break;

				case INT:
					ints.add(var);
					System.out.printf("Assigning int value of %s to variable name %s\n", val, var);
					break;

				case CHAR:
					chars.add(var);
					System.out.printf("Assigning char value of %s to variable name %s\n", val, var);
					break;
			}

			return true;
		}

		return false;
	}
	
	/*	
		Given the left (var) and right (val) sides of an assignment statement,
		determines the type of assignment performed.
	
		Return Values: -1 for invalid, 0 for bool, 1 for int, 2 for char
	*/
	private Type findVarType(String var) {
		Matcher[] m = {
			boolVar.matcher(var), 
			intVar.matcher(var), 
			charVar.matcher(var)
		};
		for(int i = 0; i < m.length; i++)
			if(m[i].find()) return Type.values()[i];
		return Type.WRONG;
	}
	
	private Type findValType(String val) {
		Matcher[] m = {
			boolVal.matcher(val),
			intVal.matcher(val), 
			charVal.matcher(val)
		};
		for(int i = 0; i < m.length; i++)
			if(m[i].find()) return Type.values()[i];
		return Type.WRONG;
	}
	
	private Type findAssignmentType(String var, String val) {
		Type varType = findVarType(var);
		Type valType = findValType(val);
		
		if(varType == valType) return varType;
		
		return Type.WRONG;
	}

	private boolean parseEquality(String expression) {
		Matcher eqMatcher = equality.matcher(expression);

		if (eqMatcher.find()) {
			String expr1 = eqMatcher.group(1);
			String expr2 = eqMatcher.group(2);

			Matcher cVar1 = charVar.matcher(expr1);
			Matcher cVal1 = charVal.matcher(expr1);
			Matcher cVar2 = charVar.matcher(expr2);
			Matcher cVal2 = charVal.matcher(expr2);

			Matcher iVar2 = intVar.matcher(expr2);
			Matcher iVal2 = intVal.matcher(expr2);

			if (evaluateIntExpression(expr1) && evaluateIntExpression(expr2)) {
				System.out.println("Successfully evaluated both expressions as int expressions. TODO: Save return values and compare for equality.");
			}
			else if (cVar1.find() || cVal1.find()) {
				// char c = (cVar1.find()) ? value of variable with name cVar1 : expr2.charAt(0);

				if (cVar2.find()) {
					System.out.println("Comparing char with char variable. TODO: Check for equality.");
				}
				else if (cVal2.find()) {
					System.out.println("Comparing char with char constant. TODO: Check for equality.");
				}
				else if (iVal2.find()) {
					System.out.println("Comparing char with int constant. TODO: Convert char and check for equality.");
				}
				else if (iVar2.find()) {
					System.out.println("Comparing char with int variable. TODO: Convert char and check for equality.");
				}
			}
			else {
				return false;
			}

			return true;
		}

		return false;
	}

	private boolean parseIncrement(String expression) {
		Matcher inc = intInc.matcher(expression);

		if (inc.find()) {
			String argument = inc.group(1);
			Matcher iVar = intVar.matcher(argument);
			Matcher iVal = intVal.matcher(argument);

			if (iVar.find()) {
				// TODO: check if variable name has been declared, find in hashtable, increment by 1 and return result

				System.out.printf("Variable with name %s incremented by one.\n", argument);
			}
			else if (iVal.find()) {
				// TODO: also return values somehow; maybe through returning a tuple-like object, or using a global variable?

				System.out.printf("%s incremented to %d.\n", argument, Integer.valueOf(argument) + 1);
			}
			else {
				return false;
			}

			return true;
		}

		return false;
	}

	private boolean parseAdd(String expression) {
		Matcher add = intAdd.matcher(expression);

		String out = "";

		if (add.find()) {
			String term1 = add.group(1);
			String term2 = add.group(2);
			Matcher t1Var = intVar.matcher(term1);
			Matcher t1Val = intVal.matcher(term1);
			Matcher t2Var = intVar.matcher(term2);
			Matcher t2Val = intVal.matcher(term2);

			if (t1Var.find()) {
				// TODO: get variable from hashtable
				out = "Variable with name " + term1 + " summed with ";
			}
			else if (t1Val.find()) {
				out = "Constant value " + term1 + " summed with ";
			}
			else {
				return false;
			}

			if (t2Var.find()) {
				// TODO: get variable from hashtable
				out += "value of variable with name " + term2 + ".";
			}
			else if (t2Val.find()) {
				// TODO: if term1 was a variable, change its value here.				
				out += "constant value " + term2 + ".";
				// TODO: must also return value, as in parseIncrement
			}
			else {
				return false;
			}

			System.out.println(out);

			return true;
		}

		return false;
	}

	private boolean parseLoop(String expr) {
		Matcher loop = this.loop.matcher(expr);

		if (loop.find()) {
			String argument = loop.group(1);
			//Matcher boolStmt = 
		}
		return true;
	}

	private boolean parseDecrement(String expression) {
		Matcher dec = intDec.matcher(expression);

		if (dec.find()) {
			String argument = dec.group(1);
			Matcher iVar = intVar.matcher(argument);
			Matcher iVal = intVal.matcher(argument);

			if (iVar.find()) {
				// TODO: check if variable name has been declared, find in hashtable, decrement by 1 and return result

				System.out.printf("Variable with name %s decremented by one.\n", argument);
			}
			else if (iVal.find()) {
				// TODO: must also return value as in parseIncrement

				System.out.printf("%s decremented to %d.\n", argument, Integer.valueOf(argument) - 1);
			}
			else {
				return false;
			}

			return true;
		}

		return false;
	}

	private boolean parseSubtract(String expression) {
		Matcher sub = intSub.matcher(expression);

		String out = "";

		if (sub.find()) {
			String term1 = sub.group(1);
			String term2 = sub.group(2);
			Matcher t1Var = intVar.matcher(term1);
			Matcher t1Val = intVal.matcher(term1);
			Matcher t2Var = intVar.matcher(term2);
			Matcher t2Val = intVal.matcher(term2);

			if (t1Var.find()) {
				// TODO: get variable from hashtable
				out = "Variable with name " + term1 + " subtracted by ";
			}
			else if (t1Val.find()) {
				out = "Constant value " + term1 + " subtracted by ";
			}
			else {
				return false;
			}

			if (t2Var.find()) {
				// TODO: get variable from hashtable
				out += "value of variable with name" + term2 + ".";
			}
			else if (t2Val.find()) {
				// TODO: if term1 was a variable, change its value here.				
				out += "constant value " + term2 + ".";
				// TODO: must also return value, as in parseIncrement
			}
			else {
				return false;
			}

			System.out.println(out);

			return true;
		}

		return false;
	}

	private boolean parseMultiply(String expression) {
		Matcher mult = intMult.matcher(expression);

		String out = "";

		if (mult.find()) {
			String term1 = mult.group(1);
			String term2 = mult.group(2);
			Matcher t1Var = intVar.matcher(term1);
			Matcher t1Val = intVal.matcher(term1);
			Matcher t2Var = intVar.matcher(term2);
			Matcher t2Val = intVal.matcher(term2);

			if (t1Var.find()) {
				// TODO: get variable from hashtable
				out = "Variable with name " + term1 + " multiplied by ";
			}
			else if (t1Val.find()) {
				out = "Constant value " + term1 + " multiplied by ";
			}
			else {
				return false;
			}

			if (t2Var.find()) {
				// TODO: get variable from hashtable
				out += "value of variable with name" + term2 + ".";
			}
			else if (t2Val.find()) {
				// TODO: if term1 was a variable, change its value here.				
				out += "constant value " + term2 + ".";
				// TODO: must also return value, as in parseIncrement
			}
			else {
				return false;
			}

			System.out.println(out);

			return true;
		}

		return false;
	}

	private boolean parseDivide(String expression) {
		Matcher div = intDiv.matcher(expression);

		String out = "";

		if (div.find()) {
			String term1 = div.group(1);
			String term2 = div.group(2);
			Matcher t1Var = intVar.matcher(term1);
			Matcher t1Val = intVal.matcher(term1);
			Matcher t2Var = intVar.matcher(term2);
			Matcher t2Val = intVal.matcher(term2);

			if (t1Var.find()) {
				// TODO: get variable from hashtable
				out = "Variable with name " + term1 + " divided by ";
			}
			else if (t1Val.find()) {
				out = "Constant value " + term1 + " divided by ";
			}
			else {
				return false;
			}

			if (t2Var.find()) {
				// TODO: get variable from hashtable
				out += "value of variable with name" + term2 + ".";
			}
			else if (t2Val.find()) {
				// TODO: if term1 was a variable, change its value here.				
				out += "constant value " + term2 + ".";
				// TODO: must also return value, as in parseIncrement
			}
			else {
				return false;
			}

			System.out.println(out);

			return true;
		}

		return false;
	}
	
	// private  boolean parseConditional(String expression) {

	// }

	private  boolean evaluateIntExpression(String expression) {
		Matcher inc = intInc.matcher(expression);
		Matcher add = intAdd.matcher(expression);
		Matcher dec = intDec.matcher(expression);
		Matcher sub = intSub.matcher(expression);
		Matcher mult = intMult.matcher(expression);
		Matcher div = intDiv.matcher(expression);
		Matcher iVar = intVar.matcher(expression);
		Matcher iVal = intVal.matcher(expression);

		// TODO: evaluate expressions, handle variables, handle operator chaining
		if (inc.find()) {
			parseIncrement(expression);
		}
		else if (add.find()) {
			parseAdd(expression);
		}
		else if (dec.find()) {
			parseDecrement(expression);
		}
		else if (sub.find()) {
			parseSubtract(expression);
		}
		else if (mult.find()) {
			parseMultiply(expression);
		}
		else if (div.find()) {
			parseDivide(expression);
		}
		else if (iVar.find()) {
			System.out.printf("Variable with name %s.\n", expression);
		}
		else if (iVal.find()) {
			System.out.printf("Constant value %d.\n", Integer.valueOf(expression));
		}
		else {
			return false;
		}

		return true;
	}
}

class SyntaxError extends Exception {
	public SyntaxError(String message) {
		super(message);
	}
}