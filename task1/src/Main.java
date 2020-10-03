import App.App;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try (App app = new App("224.0.0.251", 6666)) {
            app.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
