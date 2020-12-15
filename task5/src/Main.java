public class Main
{
    public static void main(String[] args)
    {
        if (args.length != 1) {
            System.err.println("Error: wrong number of arguments. Requires: port.");
            System.exit(-1);
        }
        Proxy proxy = new Proxy(Integer.parseInt(args[0]));
        proxy.start();
    }
}
