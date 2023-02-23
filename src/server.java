public class server {
    public static void main(String[] args) {
        int portNum = 0;
        if(args.length > 0) {
            Integer.parseInt(args[0]);
            System.out.println("Server started on port " + args[0]);
        } else {
            System.out.println("Could not start server: No port number given.");
            return;
        }

        System.out.println("Server Hello World!");
    }
    
}