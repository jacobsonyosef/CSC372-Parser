import java.util.Scanner;

public class Interactive {
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

		// close file for best practice purposes :-)
		in.close();
    }
}
