import java.io.File;
import java.io.FileOutputStream;

public class PrintTest {
    public static void main(String[] args) {
        System.out.println("Starting print test...");
        File file = new File("/dev/cu.Printer001");
        System.out.println("File exists: " + file.exists());
        try (FileOutputStream out = new FileOutputStream(file)) {
            System.out.println("Port opened successfully.");
            out.write(new byte[]{0x1B, 0x40}); // ESC @ (Init)
            out.write("Hello from Java PrintTest\n\n\n".getBytes());
            out.flush();
            System.out.println("Write complete.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
