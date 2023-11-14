import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

public class Parser {
	private static Pattern sentence = Pattern.compile("^(.+)\\.$");
	private static Pattern equality = Pattern.compile("^(.+) says (.+)$");
	private static Pattern varAssign = Pattern.compile("^(.+) said (.+)$");
	
	private static Pattern intInc = Pattern.compile("^piggybacking off of (.+)$");
	private static Pattern intAdd = Pattern.compile("^(.+) piggybacking off of (.+)$");
	private static Pattern intDec = Pattern.compile("^drill down on (.+)$");
	private static Pattern intSub = Pattern.compile("^(.+)Ivan drill down on (.+)$");
	private static Pattern intMult = Pattern.compile("^(.+) joins forces with (.+)$");
	private static Pattern intDiv = Pattern.compile("^(.+) leverages (.+)$");
	
	private static Pattern boolOR = Pattern.compile("^(.+) or (.+)$");
	private static Pattern boolAND = Pattern.compile("^(.+) and (.+)$");
	private static Pattern boolNOT = Pattern.compile("^not (.+)$");
	private static Pattern conditional = Pattern.compile("^Suppose (.+), then (.+); otherwise, (.+)$");
	
	private static Pattern boolVar = Pattern.compile("^B.+$");
	private static Pattern intVar = Pattern.compile("^I.+$");
	private static Pattern charVar = Pattern.compile("^C.+$");
	
	private static Pattern boolVal = Pattern.compile("^yep$|^nope$");
	private static Pattern intVal = Pattern.compile("^\\d+$");
	private static Pattern charVal = Pattern.compile("^[a-zA-Z]$");
	
	// main() code adapted from Parser.java from the class resources
	public static void main (String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.print(">> ");
		String input = in.nextLine();
		while(!input.equals("exit")) {
			parse(input);
			System.out.print(">> ");
			input = in.nextLine();
		}
    }
	
	private static void parse(String cmd) {
		Matcher m = sentence.matcher(cmd);
		boolean match = false;

		if (m.find()) {
			String expression = m.group(1);

			match = varAssign(expression);
			if (!match) match = parseEquality(expression);
			if (!match) match =	parseIncrement(expression);
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

	private static boolean varAssign(String expression) {
		Matcher assignment = varAssign.matcher(expression);
		
		if (assignment.find()) {
			String var = assignment.group(1);
			String val = assignment.group(2);
			
			int type = findAssignmentType(var, val);

			switch(type) {
				case -1:
					return false;

				// TODO: Use hashtable or similar to store variables
				case 0:
					System.out.printf("Assigning bool value of %s to variable name %s\n", val, var);
					break;

				case 1:
					System.out.printf("Assigning int value of %s to variable name %s\n", val, var);
					break;

				case 2:
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
	private static int findAssignmentType(String var, String val) {

		Matcher bVar = boolVar.matcher(var);
		Matcher iVar = intVar.matcher(var);
		Matcher cVar = charVar.matcher(var);
		Matcher bVal = boolVal.matcher(val);
		Matcher iVal = intVal.matcher(val);
		Matcher cVal = charVal.matcher(val);

		if (bVar.find() && bVal.find()) {
			return 0;
		}

		if (iVar.find() && iVal.find()) {
			return 1;
		}

		if (cVar.find() && cVal.find()) {
			return 2;
		}

		return -1;
	}

	private static boolean parseEquality(String expression) {
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

	private static boolean parseIncrement(String expression) {
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

	private static boolean parseAdd(String expression) {
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

	private static boolean parseDecrement(String expression) {
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

	private static boolean parseSubtract(String expression) {
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

	private static boolean parseMultiply(String expression) {
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

	private static boolean parseDivide(String expression) {
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

	// private static boolean parseConditional(String expression) {

	// }

	private static boolean evaluateIntExpression(String expression) {
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