public class server {
    public static void main(String[] args) {
        System.out.println("Server Hello World!");
        if(args.length > 0) {
            System.out.println("Server started on port " + args[0]);
        }
    }
    
}