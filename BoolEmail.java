public class BoolEmail{
public static void main(String[] args) {
if(args.length != 2){
System.out.println("There was an error encountered in delivering the contents of your email");
System.out.println("(this means that there were too few arguments supplied)");
return;}Boolean Bob = Boolean.valueOf(args[0]);
Boolean Bill = Boolean.valueOf(args[1]);

String Sarah = "Let's look at some of the boolean operators";System.out.print(Sarah);
;System.out.print("\n");
;System.out.print("They're similar to their English representations");
;System.out.print("\n");
;System.out.print((Bob || Bill));
;System.out.print("\n");
;System.out.print((Bob && Bill));
;System.out.print("\n");
;System.out.print((!Bob));
;System.out.print("\n");
;System.out.print((!Bill));
;System.out.print("\n");
;
}
}
