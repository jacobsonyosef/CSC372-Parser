import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.PrintWriter;

public class Parser {
	private static final boolean debugMode = false;	// used to toggle debug output
	private static boolean verboseOutput = false;

	// main() code adapted from Parser.java from the class resources
	public static void main (String[] args) {
		if (args.length == 0) {
			// if no file is supplied, return
			System.out.println("Please input a file name.");
			return;
		}

		if (args.length > 1) {
			if (args[args.length-1].equals("-v")) {
				System.out.println("Verbose output enabled.");
				verboseOutput = true;
			}
		}

		Parser parser = new Parser();
		parser.parseText(args[0]);
		try (PrintWriter out = new PrintWriter(args[0].substring(0,args[0].length()-6) +".java")) {
			out.println(parser.javaFile);
		}
		catch (FileNotFoundException e){}
    }

	enum Type {
		BOOL,
		INT,
		CHAR,
		STRING,
		WRONG
	}
	
	// managing scope in functions?
	// declaring a new function for parser
	// Subject: Names...
	// TODO mod operator
	private static Pattern removeWhiteSpace = Pattern.compile(".+");
	private static Pattern function_pattern = Pattern.compile("Subject: ([^ ]+?)\\. ((Dear)( [BICS]([a-zA-Z]+), )+|To whom it may concern, ).+? Best, [BICS]([a-zA-Z]+)\\.");

	private String javaFile;
	private Pattern subject = Pattern.compile("Subject: ([^ ]+)\\. ");
	private Pattern prolog = Pattern.compile("(Dear( [BICS]([a-zA-Z]+?)[,\\.]{1})+|To whom it may concern[,\\.]{1} )");
	// change epilog?
	private Pattern epilog = Pattern.compile("Best, ([BICS]([a-zA-Z]+))");
	private Pattern function = Pattern.compile("Subject: ([^ ]+)\\. (Dear( [BICS]([a-zA-Z]+?)[,\\.]{1})+|To whom it may concern, ).+?(Best, ([BICS]([a-zA-Z]+)))");
	private Pattern return_pattern = Pattern.compile("RE: (.+)");
	private Pattern call_pattern = Pattern.compile("(SEE: (.+) with: (.+))");
	private Pattern sentence = Pattern.compile(".+?(\\.|!)");
	private Pattern statement = Pattern.compile("(.+)[^.!]");
	private Pattern varAssign = Pattern.compile("^([a-zA-Z]+) said (.+)$");
	
	private Pattern int_expr = Pattern.compile("((.+) piggybacks off of (.+)|^(.+) drills down on (.+))");
	private Pattern int_expr1 = Pattern.compile("(^(.+) joins forces with (.+)|^(.+) leverages (.+)|^(.+) remains to be seen of (.+))");
	
	private Pattern bool_expr1 = Pattern.compile("(.+) or (.+)"); // OR
	private Pattern bool_expr2 = Pattern.compile("^(.+) and (.+)$"); // AND
	private Pattern bool_expr3= Pattern.compile("^not (.+)"); // NOT

	private Pattern comp_expr = Pattern.compile("((.+) is on the same page as (.+)|(.+) is greater than (.+)|(.+) is less than (.+))");
	private Pattern conditional = Pattern.compile("^Suppose (.+): then (.+); otherwise, (.+) Thanks$");
	
	
	private Pattern loop = Pattern.compile("^Keep (.+) in the loop regarding: (.+) Thanks");
	private Pattern list = Pattern.compile("(.+?), (.+)");
	
	private Pattern print = Pattern.compile("[hH]{1}ighlight (.+)");
	
	private Pattern var = Pattern.compile("([BICS]([a-zA-Z]+))");
	private Pattern boolVar = Pattern.compile("^B.+$");
	private Pattern intVar = Pattern.compile("^I.+$");
	private Pattern charVar = Pattern.compile("^C.+$");
	private Pattern stringVar = Pattern.compile("^S.+$");
	
	private Pattern boolVal = Pattern.compile("^yep$|^nope$");
	private Pattern intVal = Pattern.compile("^\\d+$");
	private Pattern charVal = Pattern.compile("^[a-zA-Z0-9]$");
	private Pattern stringVal = Pattern.compile("^\"(.+)\"$");
	
	private HashSet<String> ints;
	private HashSet<String> strings;
	private HashSet<String> bools;
	private HashSet<String> chars;
	private HashMap<String, String> operations;
	
	private class Func {
		public int numArgs;
		public ArrayList<Type> argTypes;
		public Type returnType;
		public Func(ArrayList<Type> args, Type ret) {
			numArgs = args.size();
			argTypes = args;
			returnType = ret;
		}
		public boolean validArgs(Type[] args){
			if(args.length != argTypes.size()) return false;
			for(int i = 0; i < args.length; i++)
				if(argTypes.get(i) != args[i])
					return false;
			return true;
		}
	}
	
	String curFunc = "";
	
	private HashMap<String, Func> functions;
	
	Parser() {
		ints = new HashSet<>();
		strings = new HashSet<>();
		bools = new HashSet<>();
		chars = new HashSet<>();
		// Defining a map of all operations
		functions = new HashMap<String, Func>();
		String [][] opPairs = {
			{"piggybacks off of", "+"},
			{"drill down on", "-"},
			{"joins forces with", "*"},
			{"leverages", "/"},
			{"remains to be seen of", "%"},
			{"or", "||"},
			{"and", "&&"},
			{"not", "!"},
			{"is on the same page as", "=="},
			{"is greater than", ">"},
			{"is less than", "<"}
		};
		operations = new HashMap<>();
		for (String[] pair : opPairs) {
            operations.put(pair[0], pair[1]);
        }
	}
	
	public void parseText(String filename){
		String text = readFile(filename);
		// class name is file name
		javaFile =  "public class " + filename.substring(0, filename.length()-6) + "{\n";
		if (text == null) {
			// prints file name -- BUG?
			System.out.println("Invalid input file " + filename);
			return;
		}
		try {
			parseFunction(text);
		}
		catch (SyntaxError e){
			System.out.println(e.getMessage());
		}
		// Final line to end class def
		javaFile += "\n}";
	}
	
	public Type getReturnType(String text) {
		Matcher m = epilog.matcher(text);
		if (debugMode)	System.out.println("ret text: " + text);
		if (!m.find()){
			return null;
		}
		return findVarType(m.group(1));
	}
	
	public String typeToString(Type t) {
		switch(t) {
			case INT:
				return "int";
			case BOOL:
				return "boolean";
			case CHAR:
				return "char";
			case STRING:
				return "String";
			default:
				return null;
		}
	}
	
	public String parseFunction(String text) throws SyntaxError{
		Matcher fs = function.matcher(text);
		while(fs.find()){
			String func = fs.group();
			Matcher s = subject.matcher(func);
			javaFile += parseProlog(func);
			String body = getBody(func);
			javaFile += parseBody(body);
			javaFile += "\n}";
			if (debugMode)	System.out.println(ints.toString());

			if (verboseOutput)	System.out.println("Parsing '" + text + "' as function declaration.");
		}
		if (debugMode)	System.out.println(text);
		return "";
	}

	/*
	 * Get the body of the email
	 */
    private String getBody(String text){
        Matcher sm = subject.matcher(text);
        Matcher pm = prolog.matcher(text);
        Matcher em = epilog.matcher(text);
        
        String s = "";
        String p = "";
        String e = "";
        
		if(sm.find()) s = sm.group();
        if(pm.find()) p = pm.group();
        if(em.find()) e = em.group();
		// substract out prologue and epilogue to get body of text
		text = text.substring(s.length(), text.length());
        text = text.substring(p.length(), text.length());
        text = text.substring(0, text.length() - e.length());
        
        return text.trim();
    }
    
    private String parseBody(String text) throws SyntaxError {
        var sentences = this.sentence.matcher(text);
		String body = "";
		if (debugMode)	System.out.println("Body: " + text);
		int idx = 0;
		try {
			while(sentences.find()) {
				String sentence = sentences.group();
				sentence = sentence.trim();
				// System.out.println(sentence.trim()); // debugging
				body += parseSentence(sentence);
				idx++;
			}
		}
		catch(SyntaxError e){
			throw new SyntaxError(
				"Hey, sorry to bother you with this but" +
				" we found the following error in sentence" 
				+ (idx + 1) + ":\n" +
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
			t = t.trim().replaceAll(" +", " ");
			// System.out.println(t); // debugging 
			return t;
		} catch (IOException e) {
			return null;
		}
    }
	
	private String parseEpilog(String text) throws SyntaxError{
		Matcher epilog = this.epilog.matcher(text);
		if(!epilog.find())
			throw new SyntaxError("Looks like there's an issue with the sign-off: " + text);
		javaFile += "\n}";
		return "";
	}
	
	/*
	 * transpile the prologue.
	 */
	private String parseProlog(String text) throws SyntaxError{
		bools.clear();
		ints.clear();
		chars.clear();
		Matcher sub = subject.matcher(text);
		if (!sub.find()){
			throw new SyntaxError("Couldn't find the subject heading for your email at: " + text);
		}
		String subject = sub.group(1);
		if (debugMode)	System.out.println(subject);
		Matcher prologMatch = prolog.matcher(text);
		// No match found... throw error
		if(!prologMatch.find()){
			throw new SyntaxError("It looks like there's an issue with the greeting: " + text);
		}
		
		String functionStart = "";
		// Check that the correct number of command line arguments was supplied
		String opening = prologMatch.group();
		var var = this.var.matcher(opening); // get individual variable names from comma-separated list
		int idx = 0;
		this.curFunc = subject;
		String body = "";
		if (debugMode)	System.out.println(opening);
		if(subject.equals("Main")){
			subject = "main";
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
					case STRING:
						strings.add(curVar);
						body += "String " + curVar + " = args[" + idx + "]);\n";
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
			functionStart += "public static void " + subject + "(String[] args) {\n";
			functionStart += "if(args.length != " + (idx) + "){\n";
			functionStart += "System.out.println(";
			functionStart += "\"There was an error encountered in delivering the contents of your email\");\n";
			functionStart += "System.out.println(\"(this means that there were too few arguments supplied)\");\n";
			functionStart += "return;";
			functionStart += "}";

			functions.put(subject, new Func(new ArrayList<Type>(), Type.INT));
			return functionStart + body + "\n";
		}
		
		String args = "";
		ArrayList<Type> argType = new ArrayList<>();
		while(var.find()){
			
			String curVar = var.group();
			if (curVar == "To whom it may concern,"){
				break;
			}
			switch (findVarType(curVar)){
				case BOOL:
					argType.add(Type.BOOL);
					bools.add(curVar);
					args += "boolean " + curVar +",";
				break;
				case INT:
					argType.add(Type.INT);
					ints.add(curVar);
					args += "int " + curVar + ",";
				break;
				case CHAR:
					argType.add(Type.CHAR);
					chars.add(curVar);
					args += "char " + curVar + ",";
				break;
				case STRING:
					argType.add(Type.STRING);
					strings.add(curVar);
					args += "String " + curVar + ",";
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
		if(args.length() > 0)
			args = args.substring(0, args.length() - 1);
		functions.put(subject, new Func(argType, getReturnType(text)));
		functionStart += "public static " + typeToString(getReturnType(text)) + " " + subject + "(" + args + ")" + "{\n";
		if (debugMode)	System.out.println(functionStart);
		return functionStart;
	}
	
	private String parseReturn(String ret) throws SyntaxError{
		Matcher r = return_pattern.matcher(ret);
		if (!r.find())
			return "";
		if (debugMode)	System.out.println(r.group());
		String toRet = r.group(1);
		switch(functions.get(curFunc).returnType) {
			case INT:
				return "return " + parseIntExpr(toRet);
			case BOOL:
				return "return " + parseBoolExpr(toRet);
			case STRING:
				return "return " + parseStringExpr(toRet);
			case WRONG:
				throw new SyntaxError("Sorry, but it looks like you used an invalid sign-off name: '" + toRet + "'.\nPlease use a name starting with B, I, or S.");
		}
		return "";
	}
	
	
	private String parseFunctionCall(String functionCall) throws SyntaxError{
		Matcher r = call_pattern.matcher(functionCall);
		if (!r.find())
			return "";

		String funcName = r.group(2);
		Func func = functions.get(funcName);
		String args = r.group(3);

		if (verboseOutput)	System.out.println("Parsing '" + functionCall + "' as function call with name '" + funcName + "' and arguments '" + args + "'.");

		String[] argList = new String[func.numArgs];
		String rem = args;
		for(int i = 0; i < func.numArgs; i++){
			Matcher a = list.matcher(rem);
			if(a.find()){
				if (debugMode) {
					System.out.println(a.group(1));
					System.out.println(a.group(2));
				}
				switch(func.argTypes.get(i)) {
					case INT:
						argList[i] = parseIntExpr(a.group(1));
						break;
					case BOOL:
						argList[i] = parseBoolExpr(a.group(1));
						break;
					case STRING:
						argList[i] = parseStringExpr(rem);
						break;
					case WRONG:
						throw new SyntaxError("Please check the name(s) of your addressee(s): '" + rem + "'.\nPlease remember to only use names starting with B, I, or S.");
				}
				rem = a.group(2);
			}
			else {
				switch(func.argTypes.get(i)) {
					case INT:
						argList[i] = parseIntExpr(rem);
						break;
					case BOOL:
						argList[i] = parseBoolExpr(rem);
						break;
					case STRING:
						argList[i] = parseStringExpr(rem);
						break;
					case WRONG:
						throw new SyntaxError("Please check the name(s) of your addressee(s): '" + rem + "'.\nPlease remember to only use names starting with B, I, or S.");
				}
			}
		}
		String s = "";
		for(int i = 0; i < func.numArgs; i++){
			s += argList[i];
			if (i != func.numArgs - 1){
				s += ",";
			}
		}
		return funcName +"(" + s + ")";
	}
	

	
	public String parseSentence(String cmd) throws SyntaxError {
		Matcher m = statement.matcher(cmd);
		String match = "";
		
		if (m.find()) {
			String expression = m.group();

			if (debugMode)	System.out.println(expression);
			match = varAssign(expression);
			if (debugMode)	System.out.println(match);
			if (debugMode)	System.out.println(match.length());
			if(match.length() >  0) return match + ";";
			match = loop(expression);
			if (match.length() >  0) return match + ";";
			match = condition(expression);
			if(match.length() > 0) return match + ";";
			match = print(expression);
			if (match.length() >  0) return match + ";";
			match = parseReturn(expression);
			if (match.length() >  0) return match + ";";
			match = parseFunctionCall(expression);
			if (match.length() >  0) return match + ";";
			try {
				match = evalExpr(expression);
			}
			catch (SyntaxError e) {}
			if (match.length() >  0) return match + ";";
			throw new SyntaxError("Missing period.");
		}
		else {
			throw new SyntaxError("Missing period.");
		}
	}

	private String toBool(String val) {
		if (val.equals("yep")) return "true";
		else if(val.equals("nope")) return "false";
		else return val;
	}

	private String print(String p) throws SyntaxError{
		Matcher m = print.matcher(p);
		if(!m.find())
			return "";
		String expr = m.group(1);
		String toString = evalExpr(expr);

		if (verboseOutput)	System.out.println("Parsing print operation on the string '" + toString + "'.");

		if (debugMode)	System.out.println(toString);
		return "System.out.print(" + toString + ");\n";
	}

	private String varAssign(String expression) throws SyntaxError {
		Matcher assignment = varAssign.matcher(expression);
		if (debugMode)	System.out.println("assign: "+ expression);
		if (assignment.find()) {
			String var = assignment.group(1);
			String val = assignment.group(2);
			Type type = findVarType(var);			// add declaration and assignment to output file
			if (debugMode)	System.out.println("assign: " + val);
			switch(type) {
				case WRONG:
					return "";
				case BOOL:
					if (bools.contains(var)) return  var + " = " + parseBoolExpr(val);
					else {
						String out = "boolean " + var + " = " + parseBoolExpr(val);
						bools.add(var);
						if (verboseOutput)	System.out.printf("Assigning bool value of %s to variable name %s\n", val, var);
						return out;
					}

				case INT:
					if (ints.contains(var)) return var + " = " + parseIntExpr(val);
					else {
						String out = "int " + var + " = " + parseIntExpr(val);
						ints.add(var);
						if (verboseOutput)	System.out.printf("Assigning int value of %s to variable name %s\n", val, var);
						return out;
					}

				case CHAR:
					if (chars.contains(var)) return var + " = " + "'" + val + "'";
					else {
						chars.add(var);
						if (verboseOutput)	System.out.printf("Assigning char value of %s to variable name %s\n", val, var);

						return "char " + var + " = " + "'" + val + "'";
					}

				case STRING:
					if (strings.contains(var)) return var + " = " + "'";
					else {
						String out = "String " + var + " = " + "" + val;
						strings.add(var);
						if (verboseOutput)	System.out.printf("Assigning string value of %s to variable name %s\n", val, var);

						return out;
					}
			}
		}

		return "";
	}
	
	private String evalExpr(String expr) throws SyntaxError{
		try {
			return parseIntExpr(expr);
		}
		catch(SyntaxError e){}
		try {
			return parseBoolExpr(expr);
		}
		catch(SyntaxError e){}
		try {
			return parseStringExpr(expr);
		}
		catch (SyntaxError e) {
			throw new SyntaxError(
				"It seems you have a formatting issue here: '" + expr + "'."
			);
		}
	}
	
	
	private String condition(String cond) throws SyntaxError{
		Matcher m = conditional.matcher(cond);
		if(!m.find()){
			return "";
		}
		String condition = parseBoolExpr(m.group(1));
		String list = parseList(m.group(2));
		String otherwise = parseList(m.group(3));

		if (verboseOutput)	System.out.println("Parsing conditional with condition '" + condition + "', then '" + list + "', and otherwise '" + otherwise + "'.");

		String out =  "if (" +condition + ")";
		out += "\n" + " {" + list + "}";
		out +=  "\n" + "else {" + otherwise + "}";
		return out;
	}
	
	private String loop(String loop) throws SyntaxError {
		Matcher m = this.loop.matcher(loop);
		if (debugMode)	System.out.println("LOOP " + loop);
		if(!m.find()){
			return "";
		}
		if (debugMode)	System.out.println("LOOP");
		String condition = parseBoolExpr(m.group(1));
		if (debugMode)	System.out.println("LIST:" +m.group(2));
		String list = parseList(m.group(2));

		if (verboseOutput)	System.out.println("Parsing while loop with condition '" + condition + "' and body '" + list + "'.");

		String out =  "while (" +condition + ")";
		out += "\n" + " {" + list + "}";
		return out;
	}
	
	private String parseList(String in) throws SyntaxError{
		String out = "";
		if (debugMode)	System.out.println("LIST");
		Matcher l = list.matcher(in);
		for(String el : in.split(",")){
			out += parseSentence(el.strip());
		}
		return out;
	}
	
	
	private interface Expr {
		public String call(String expr) throws SyntaxError;
	}
	private String unaryExpr(
		String expr, 
		Pattern p, 
		String op, 
		Expr match,
		Expr noMatch
	) throws SyntaxError {
		String out = "";
		Matcher m = p.matcher(expr);
		if (debugMode)	System.out.println("HERE" + expr);
		if(m.find()){
			String e1 = m.group(1);
			if (debugMode)	System.out.println("CALL");
			return "(" + op + match.call(e1) + ")";
		}
		return noMatch.call(expr);
	}
	
	private String getOp(String op) throws SyntaxError{
		op = op.trim();
		if(operations.get(op) != null){
			return operations.get(op);
		}
		else {
			throw new SyntaxError("There seems to be an issue with the operation: '" + op + "'.");
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
		Expr noMatch
	) throws SyntaxError {
		if (debugMode)	System.out.println(expr);
		String out = "";
		Matcher m = p.matcher(expr);
		if(m.find()){
			String e1 = m.group(1);
			String e2 = m.group(2);
			if (debugMode)	System.out.println(m.groupCount());

			// Hack to generalize around the fact that using '|'
			// to match multiple groups ends up putting the values in weird groups
			// I think it relies on left associativity (late as I'm coding)
			if(m.groupCount() > 2){
				for(int i = 2; i < m.groupCount(); i+=2){
					if(m.group(i) != null){
						e1 = m.group(i);
						e2 = m.group(i + 1);
						if (debugMode) {
							System.out.println(e1);
							System.out.println(e2);
						}
					}
				}
			}
			String op = expr.substring(e1.length(), expr.length() - e2.length()).trim();

			if (verboseOutput)	System.out.println("Parsing '" + expr + "' as '(" + left.call(e1) + " " +  getOp(op) + " " + right.call(e2) + ")'.");

			return "(" + left.call(e1) + " " +  getOp(op) + " " + right.call(e2) + ")";
		}
		return noMatch.call(expr);
	}

	// Calling it parseAtom since it's for parsing the most basic level (variable or literals)
	private String parseAtom(
		String atom, 
		Pattern varPattern, 
		Pattern valPattern, 
		HashSet<String> scope
	) throws SyntaxError {
		Matcher varMatcher = varPattern.matcher(atom);
		Matcher valMatcher = valPattern.matcher(atom);
		if (debugMode)	System.out.println("HERE:" + atom);
		if(varMatcher.find()){
			// Check if we have already declared the variable
			if(scope.contains(atom))
				return atom;
			// Maybe need a new type of error for this
			throw new SyntaxError(
				atom + " doesn't seem to be in your contacts.\n"
				+ "You currently have the following contacts: \n"
				+ scope.toString()
			);
		} 
		else if(valMatcher.find()){
			return atom;
		}
		else {
			throw new SyntaxError(
				"Hey, you may have sent this to the wrong person.\n"
				+ atom + " usually isn't responsible for this work."
			);
		}
	}

	private String parseComp(String comp) throws SyntaxError{
		Matcher m = comp_expr.matcher(comp);
		if(m.find()){
			try {
				return binaryExpr(
					comp,
					comp_expr,
					x->parseBoolExpr1(x),
					x->parseBoolExpr1(x),
					x->parseBoolExpr1(x)
				);
			}
			catch(SyntaxError e){}
			try {
				return binaryExpr(
					comp,
					comp_expr,
					x->parseIntExpr(x),
					x->parseIntExpr(x),
					x->parseIntExpr(x)
				);
			}
			catch (SyntaxError e) {
				throw new SyntaxError(
					"Hey, I wanted to sync up about your comparisons.\n"
					+ "You said \"" + comp + "\"\n"
					+ "But, I wasn't totally sure what you meant."
				);
			}
		}
		throw new SyntaxError("You should check your comparison here: '" + comp + "'.");
	}

	private String parseStringExpr(String expr) throws SyntaxError {
		try {
			return parseAtom(expr, stringVar, stringVal, strings);
		}
		catch(SyntaxError e)
		{
			throw new SyntaxError(e.getMessage());
		}
	}

	private String parseBoolExpr(String expr) throws SyntaxError {

		return unaryExpr(
			expr, 
			comp_expr,
			"",
			x->parseComp(x),
			x->parseBoolExpr0(x)
		);
	}
	
	private String parseBoolExpr0(String expr) throws SyntaxError {
		//<bool_expr> ::= <comp> | <bool_expr1>
		// unaryExpr probably should be renamed 
		// but basically just to call parseCompExpr if comp is a match
		return unaryExpr(
			expr, 
			call_pattern,
			"",
			x->parseFunctionCall(x),
			x->parseBoolExpr1(x)
		);
	}
	
	private String parseBoolExpr1(String expr) throws SyntaxError {
		// <bool_expr1> ::= <bool_expr1> or <bool_expr2> | <bool_expr2>

		return binaryExpr(
			expr, 
			bool_expr1, 
			x->parseBoolExpr1(x), 
			x->parseBoolExpr2(x),
			x->parseBoolExpr2(x)
		);
	}
	private String parseBoolExpr2(String expr) throws SyntaxError {
		//<bool_expr2> ::= <bool_expr2> and <bool_expr3> | <bool_expr3>
		if (debugMode)	System.out.println(expr);
		return binaryExpr(
			expr, 
			bool_expr2, 
			x->parseBoolExpr2(x), 
			x->parseBoolExpr3(x),
			x->parseBoolExpr3(x)
		);
	}
	
	private String parseBoolExpr3(String expr) throws SyntaxError {
		//<bool_expr3> ::= not <bool_expr4> | <bool_expr4>
		return unaryExpr(
			expr, 
			bool_expr3, 
			"!", 
			x->parseBool(x), 
			x->parseBool(x) 
		);
	}
	
	private String parseBool(String bool) throws SyntaxError{
		return toBool(parseAtom(
			bool,
			boolVar,
			boolVal,
			bools
		));
	}

	private String parseIntExpr(String expr) throws SyntaxError {

		if (debugMode)	System.out.println(expr);
		return binaryExpr(
			expr,
			int_expr, 
			x->parseIntExpr(x), 
			x->parseIntExpr0(x), 
			x->parseIntExpr0(x)
		);
	}
	private String parseIntExpr0(String expr) throws SyntaxError {
		//<bool_expr> ::= <comp> | <bool_expr1>
		// unaryExpr probably should be renamed 
		// but basically just to call parseCompExpr if comp is a match
		return unaryExpr(
			expr, 
			call_pattern,
			"",
			x->parseFunctionCall(x),
			x->parseIntExpr1(x)
		);
	}
	
	private String parseIntExpr1(String expr) throws SyntaxError {
		if (debugMode)	System.out.println(expr);
		return binaryExpr(
			expr,
			int_expr1, 
			x->parseIntExpr1(x), 
			x->parseInt(x), 
			x->parseInt(x)
		);
	}
	
	private String parseInt(String _int) throws SyntaxError {
		return parseAtom(
			_int,
			intVar,
			intVal,
			this.ints
		);
	}
	
	/*
		Given the left (var) and right (val) sides of an assignment statement,
		determines the type of assignment performed.
	
		Return Values: TYPE of var (INT, BOOL, CHAR, STRING, or WRONG)
	*/
	private Type findVarType(String var) {
		Matcher[] m = {
			boolVar.matcher(var), 
			intVar.matcher(var), 
			charVar.matcher(var),
			stringVar.matcher(var)
		};
		for(int i = 0; i < m.length; i++)
			if(m[i].find()) return Type.values()[i];
		return Type.WRONG;
	}
	
	private Type findValType(String val) {
		Matcher[] m = {
			boolVal.matcher(val),
			intVal.matcher(val), 
			charVal.matcher(val),
			stringVal.matcher(val)
		};
		for(int i = 0; i < m.length; i++)
			if(m[i].find()) return Type.values()[i];
		return Type.WRONG;
	}

}

class SyntaxError extends Exception {
	public SyntaxError(String message) {
		super(message);
	}
}