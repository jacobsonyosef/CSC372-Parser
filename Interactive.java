import java.util.Scanner;

public class Interactive {
    	/*
	 * Command-line interactive moee
	 */
    public static void main(String[] args) {
		Parser parser = new Parser();

        Scanner in = new Scanner(System.in);
		System.out.println("Welcome to EMAIL LANG 1.0!! You get it all day at work, now you can get it at home too!");
		System.out.println("Type \"Farewell\" to exit the program.");
		System.out.print(">> ");
		String input = in.nextLine();

		// COME BACK TO THIS??
		while(!input.equals("Farewell")) {
			// Matcher epilogMatcher = epilog.Matcher();
			try {
				parser.parseSentence(input);
			}
			catch(SyntaxError e) {
				System.out.println("Sytax Error: " + e.getMessage());
			}
			System.out.print(">> ");
			input = in.nextLine();
		}

		// close file for best practice purposes :-)
		in.close();
    }
}
