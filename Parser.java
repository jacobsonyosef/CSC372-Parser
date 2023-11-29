import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.HashSet;
import java.util.HashTable;
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
	private Pattern epilog = Pattern.compile("(Best, ([BICS]([a-zA-Z]+)) )$");
	private Pattern sentence = Pattern.compile(".+?(\\.|!)");
	// THIS MIGHT CAUSE BUGS?
	private Pattern statement = Pattern.compile("(.+)[^.!]");
	private Pattern equality = Pattern.compile("^(.+) says (.+)\\.");
	private Pattern varAssign = Pattern.compile("^([a-zA-Z]+) said (.+)$");
	
	private Pattern intInc = Pattern.compile("^piggybacking off of (.+)$");
	private Pattern intAdd = Pattern.compile("^(.+) piggybacking off of (.+)$");
	private Pattern intDec = Pattern.compile("^drill down on (.+)$");
	private Pattern intSub = Pattern.compile("^(.+) drill down on (.+)$");
	private Pattern intMult = Pattern.compile("^(.+) joins forces with (.+)$");
	private Pattern intDiv = Pattern.compile("^(.+) leverages (.+)$");
	
	private Pattern bool_expr1 = Pattern.compile("^(.+) or (.+)$"); // OR
	private Pattern bool_expr2 = Pattern.compile("^(.+) and (.+)$"); // AND
	private Pattern bool_expr3= Pattern.compile("^not (.+)$"); // NOT
	private Pattern bool_expr4 =  Pattern.compile("\\((.+)\\)$"); // NOT

	// | <str_expr> is on the same page as <str_expr>
	// | <char_expr> is on the same page <char_expr> 
	// | <int_expr> greater than <int_expr> 
	// | <str_expr> greater than <str_expr> 
	//| <char_expr> greater than <char_expr> 
	// |  <int_expr> less than <int_expr> | <str_expr> less than <str_expr> | <char_expr> less than <char_expr>

	private Pattern comp = Pattern.compile("((.+) is on the same page as (.+)|(.+) greater than (.+)|(.+) less than (.+))")
	

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
	private HashMap<String, String> operations;
	
	Parser(String filename) {
		ints = new HashSet<>();
		strings = new HashSet<>();
		bools = new HashSet<>();
		chars = new HashSet<>();
		// Defining a map of all operations
		String [][] opPairs = {
			{"piggybacking off of", "+"}
			{"drill down on", "-"},
			{"joins forces with", "*"},
			{"leverages", "/"},
			{"or", "||"},
			{"and", "&&"},
			{"not", "!"}
		}
		operations = new HashTable<>();
		for (String[] pair : pairs) {
            operations.put(pair[0], pair[1]);
        }
		
		String text = readFile(filename);

		// class name is file name
		javaFile =  "public class " + filename.substring(0,filename.length()-6) + "{\n";

		if (text == null) {
			// prints file name -- BUG?
			System.out.println("Invalid input file " + filename);
			return;
		}

		// System.out.println(text); // for debugging purposes

		try {
			javaFile += parseProlog(text);
			// System.out.println(javaFile); // for debugging
			String body = getBody(text);
			// System.out.println(body); // for debugging
			parseBody(body);
			// javaFile += parseEpilog(text);
		}
		catch (SyntaxError e){
			System.out.println(e.getMessage());
		}
		// Final line to end class def
		javaFile += "\n}";

		System.out.println(javaFile);
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
		System.out.println(e);
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
				sentence = sentence.trim();
				// System.out.println(sentence.trim()); // debugging
				parseSentence(sentence);
			}
		}
		catch(SyntaxError e){
			throw new SyntaxError(
				"Hey, sorry to bother you with this but" +
				" we found the following error in sentence" 
				+ (idx + 1) + ":" +
				e.getMessage()
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
			// System.out.println(t); // debugging 
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
		functionStart += "return;";
		functionStart += "}";
		return functionStart + body + "\n}";
	}
	
	private void parseSentence(String cmd) throws SyntaxError {
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

	private String toBool(String val) {
		if (val.equals("yep")) return "true";
		else return "false";
	}

	private boolean varAssign(String expression) {
		Matcher assignment = varAssign.matcher(expression);

		if (assignment.find()) {
			String var = assignment.group(1);
			String val = assignment.group(2);
			
			Type type = findAssignmentType(var, val);

			// add declaration and assignment to output file
			switch(type) {
				case WRONG:
					return false;

				case BOOL:
					if (bools.contains(var)) javaFile += var + " = " + parseBoolExpr(val) + ";\n";
					else {
						bools.add(var);
						javaFile += "boolean " + var + " = " + parseBoolExpr(val) + ";\n";
						System.out.printf("Assigning bool value of %s to variable name %s\n", val, var);
					}
					break;

				case INT:
					if (ints.contains(var)) javaFile += var + " = " + parseIntExpr(val) + ";\n";
					else {
						ints.add(var);
						javaFile += "int " + var + " = " + parseIntExpr(val) + ";\n";
						System.out.printf("Assigning int value of %s to variable name %s\n", val, var);
					}
					break;

				case CHAR:
					if (chars.contains(var)) javaFile += var + " = " + "'" + val + "'" + ";\n";
					else {
						chars.add(var);
						javaFile += "char " + var + " = " + "'" + val + "'" + ";\n";
						System.out.printf("Assigning char value of %s to variable name %s\n", val, var);
					}
					break;
			}

			return true;
		}

		return false;
	}
	
	private interface Expr {
		public String call(String expr);
	}
	private String unaryExpr(
		String expr, 
		Pattern p, 
		String op, 
		Expr match,
		Expr noMatch,
	)   {
		String out = "";
		Matcher m = p.matcher(expr);
		if(m.find()){
			return "(" + op + match.call(expr) + ")"
		}
		return noMatch.call(expr);
	}
	private String getOp(String op) throws SyntaxError{
		op = op.trim();
		if(operations.has(op)){
			return operations.get(op);
		}
		else {
			throw new SyntaxError("Invalid operation: " + op);
		}
	}
	/* Function that applies the supplied pattern to expr and 
	* calls the corresponding expr according to the operation
	*/
	private String binaryExpr(
		String expr,
		Pattern p, 
		Expr left, 
		Expr right,
		Expr noMatch,
	){
		String out = "";
		Matcher m = p.matcher(expr);
		if(m.find()){
			String e1 = m.group(1);
			String e2 = m.group(2);
			String op = comp.substring(e1.length(), comp.length() - e2.length()).trim();
			return "(" + left.call(e1) + " " +  opMap.call(op) + " " + right.call(e2) + ")"
		}
		return noMatch.call(expr);
	}
	// Calling it parseAtom since it's for parsing the most basic level (variable or literals)
	private String parseAtom(
		String atom, 
		Pattern varPattern, 
		Pattern valPattern, 
		HashSet<String> scope
	){
		Matcher varMatcher = varPattern.matcher(bool);
		Matcher valMatcher = valPattern.matcher(bool);
		
		if(varMatcher.find()){
			// Check if we have already declared the variable
			if(scope.has(atom))
				return atom;
			// Maybe need a new type of error for this
			throw SyntaxError(
				atom + "doesn't seem to be in your contacts.\n"
				+ "You currently have the following contacts: \n"
				+ scope.toString();
			)
		} 
		else if(valMatcher.find()){
			return toBool(bool);
		}
		else {
			throw SyntaxError(
				"Hey, you may have sent this to the wrong person.\n"
				+ bool + " usually isn't responsible for this work."
			);
		}
	}
	
	private String getComp(String op){
		if(op.equals("is on the same page as")) return " == ";
		else if(op.equals("greater than")) return " > ";
		else return " < ";
	}
	
	private String parseComp(String comp) throws SyntaxError{
		Matcher m = p.matcher(comp);
		if(m.find()){
			String e1 = m.group(1);
			String e2 = m.group(2);
			String op = comp.substring(e1.length(), comp.length() - e2.length()).trim();
			//TODO figure out smart way to figure out the types being compared?
			String l = "";
			String r = "";
			try {
				String l = boolExpr(e1);
				String r = boolExpr(e2);
			}
			catch(SyntaxError e){}
			if (l.equals("") || r.equals("")) try {
				String l = intExpr(e1);
				String r = intExpr(e2);
			}
			if (l.equals("") || r.equals("")) try {
				String l = intExpr(e1);
				String r = intExpr(e2);
			}
			catch {
				throw new SyntaxError(
					"Hey, I wanted to sync up about your comparisons.\n"
					+ "You said \"" + comp + "\"\n"
					+ "But, I wasn't totally sure what you meant."
				)
			}
			return e1 + getComp(op) + e2; 
		}
		throw new SyntaxError("Encountered the following invalid comparison:" + comp);
	}

	private String boolExpr(String expr) {
		//<bool_expr> ::= <comp> | <bool_expr1>
		// unaryExpr probably should be renamed 
		// but basically just to call parseCompExpr if comp is a match
		return unaryExpr(
			expr, 
			comp,
			"",
			x->parseComp(x),
			x->boolExpr1(x)
		)
	}
	
	private String boolExpr1(String expr){
		// <bool_expr1> ::= <bool_expr1> or <bool_expr2> | <bool_expr2>
		return binaryExpr(
			expr, 
			bool_expr2, 
			x->parseBoolExpr1(x), 
			x->parseBoolExpr2(x),
			x->parseBoolExpr2(x),
		);
	}
	private String parseBoolExpr2(String expr){
		//<bool_expr2> ::= <bool_expr2> and <bool_expr3> | <bool_expr3>
		return binaryExpr(
			expr, 
			bool_expr2, 
			x->parseBoolExpr2(x), 
			x->parseBoolExpr3(x),
			x->parseBoolExpr3(x),
		);
	}
	
	private String parseBoolExpr3(String expr){
		//<bool_expr3> ::= not <bool_expr4> | <bool_expr4>
		return unaryExpr(
			expr, 
			bool_expr3, 
			"!", 
			x->parseBoolExpr3(x), 
			x->parseBoolExpr4(x) 
		);
	}
	private String parseBoolExpr4(String expr){
		//  <bool_expr4> ::= (<bool_expr>) | <bool>
		return unaryExpr(
			expr, 
			bool_expr4, 
			"", 
			x->parseBoolExpr(x), 
			x->parseBool(x) 
		);
	}
	
	private String parseBool(String bool) throws SyntaxError{
		return parseAtom(
			bool,
			boolVar,
			boolVal,
			bools
		)
	}

	private String parseIntExpr(String expr) {
		return binaryExpr(
			expr,
			int_expr, 
			x->parseIntExpr(x), 
			x->parseIntExpr1(x), 
			x->parseIntExpr(x)
		);
	}
	private String parseIntExpr1(String expr) {
		return binaryExpr(
			expr,
			int_expr1, 
			x->parseIntExpr1(x), 
			x->parseIntExpr2(x), 
			x->parseIntEpx1(x)
		);
	}
	private String parseIntExpr2(String expr) {
		return unaryExpr(
			expr,
			int_expr2,
			"",
			x->parseIntExpr(x), 
			x->parseInt(x), 
		);
	}
	private String parseInt(String _int){
		return parseAtom(
			_int,
			intVar,
			intVal,
			ints
		)
	}

	private String char_expr(String expr) {
		// TODO
		return "";
	}

	private String string_expr(String expr) {
		//TODO
		return "";
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
	
	// private  boolean parseConditional(String expression) {

	// }

}

class SyntaxError extends Exception {
	public SyntaxError(String message) {
		super(message);
	}
}