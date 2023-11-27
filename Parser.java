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

	private Pattern prolog = Pattern.compile("^(Dear)( [BICS]([a-zA-Z]+),)+");
	private Pattern epilog = Pattern.compile("((Best,) ([BICS]([a-zA-Z]+)))$");
	private Pattern sentence = Pattern.compile("^(.+)\\.$");
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
	
	Parser() {
		ints = new HashSet<>();
		strings = new HashSet<>();
		bools = new HashSet<>();
		chars = new HashSet<>();
	}
	
	// main() code adapted from Parser.java from the class resources
	public static void main (String[] args) {
		Parser parser = new Parser();

		if (args.length == 0) {
			// if no file is supplied, go into command-line mode
			REPL(parser);
			return;
		}

		String text = readFile(args[0]);
		// class name is file name
		String output = "public class " + args[0].substring(0,args[0].length()-6) + "{\n";

		if (text == null) {
			// prints file name -- BUG?
			System.out.println("Invalid input file " + args[0]);
			return;
		}

		System.out.println(text); // for debugging purposes

		try {
			output += parser.parseProlog(text);
			System.out.println(output); // debugging
			System.out.println(parser.getBody(text)); // debugging
		} 
		catch (SyntaxError e) {
			System.out.println(e.getMessage());
		}
		// Final line to end class def
		output += "\n}";
    }
    
	/*
	 * Command-line interactive moee
	 */
    private static void REPL(Parser parser) {
        Scanner in = new Scanner(System.in);
		System.out.println("Welcome to EMAIL LANG 1.0!! You get it all day at work, now you can get it at home too!");
		System.out.println("Type \"Farewell\" to exit the program.");
		System.out.print(">> ");
		String input = in.nextLine();

		// COME BACK TO THIS??
		while(!input.equals("Farewell")) {
			// Matcher epilogMatcher = epilog.Matcher();
			parser.parse(input);
			System.out.print(">> ");
			input = in.nextLine();
		}
		in.close();
    }
    
    private String getBody(String text){
        Matcher pm = prolog.matcher(text);
        Matcher em = epilog.matcher(text);
        String p = "";
        String e = "";
        if(pm.find()){
            p = pm.group();
        }
        if(em.find()){
            e = em.group();
        }
        text = text.substring(p.length(), text.length() - 1);
        text = text.substring(0, text.length() - e.length());
        return text;
    }
    
    private String parseBody(String text){
        var sentences = sentence.matcher(text);
		String body = "";
        // body += 
        
        return body;
    }

    
    private static String readFile(String filename){
        try {
			String text = Files.readString(Paths.get(filename));
			return text;
		} catch (IOException e) {
			return null;
		}
    }
	
	private String parseProlog(String text) throws SyntaxError{
		Matcher prologMatch = this.prolog.matcher(text);
		if(!prologMatch .find()){
			throw new SyntaxError("AHHH");
		}
		
		String functionStart = "";
		// Check that the correct number of command line arguments was supplied
		String opening = prologMatch.group();
		var var = this.var.matcher(opening);
		int idx = 0;

		String body = "";
		System.out.println(opening);
		while(var.find()){
			String curVar = var.group();
			System.out.println(curVar);
			System.out.println(findVarType(curVar));
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
						+ "Your email must be addressed to someone(s) with name(s) starting with B, I, or S.\n"
						+ "Please do better.\n"
						+ "Sincerely, the email-team"
					);
			}
			idx++;
		}
		/*String var = prologMatch.group(i);
		
		 */
		functionStart += "public static void main(String[] args) {\n";
		functionStart += "if(args.length < " + var.groupCount() + "){\n";
		functionStart += "System.out.println(";
		functionStart += "\"There was an error encountered in delivering the contents of your email\");\n";
		functionStart += "System.out.println(\"(this means that there were too few arguments supplied)\");\n";
		functionStart += "}";
		return functionStart + body + "\n}";
	}
	
	private void parse(String cmd) {
		Matcher m = sentence.matcher(cmd);
		boolean match = false;

		if (m.find()) {
			String expression = m.group(1);

			match = varAssign(expression);
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
		}
	}

	private boolean varAssign(String expression) {
		Matcher assignment = varAssign.matcher(expression);
		
		if (assignment.find()) {
			String var = assignment.group(1);
			String val = assignment.group(2);
			
			Type type = findAssignmentType(var, val);

			switch(type) {
				case WRONG:
					return false;

				// TODO: Use hashtable or similar to store variables
				case BOOL:
					System.out.printf("Assigning bool value of %s to variable name %s\n", val, var);
					break;

				case INT:
					System.out.printf("Assigning int value of %s to variable name %s\n", val, var);
					break;

				case CHAR:
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
		for(int i = 0; i < m.length; i++){
			if(m[i].find()){
				return Type.values()[i];
			}
		}
		return Type.WRONG;
	}
	
	private Type findValType(String val){
		Matcher[] m = {
			boolVal.matcher(val),
			intVal.matcher(val), 
			charVal.matcher(val)
		};
		for(int i = 0; i < m.length; i++){
			if(m[i].find()){
				return Type.values()[i];
			}
		}
		return Type.WRONG;
	}
	
	private Type findAssignmentType(String var, String val) {
		Type varType = findVarType(var);
		Type valType = findValType(val);
		
		if(varType == valType){
			return varType;
		}
		
		return Type.WRONG;
	}

	private  boolean parseEquality(String expression) {
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

	private  boolean parseIncrement(String expression) {
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

	private  boolean parseAdd(String expression) {
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

	private  boolean parseSubtract(String expression) {
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

	private  boolean parseMultiply(String expression) {
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

	private  boolean parseDivide(String expression) {
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
	public SyntaxError(String message){
		super(message);
	}
}