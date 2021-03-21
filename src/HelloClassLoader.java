import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @date: 2021-3-21 22:07
 * @author: Znw · Smile
 * @Description:
 */
public class HelloClassLoader extends ClassLoader{
    public static void main(String[] args) {
        try {
            new HelloClassLoader().findClass("Hello").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(name+".xlass"));
            for (int i = 0; i < bytes.length; i++) {
                bytes [i] = (byte) (255 - bytes [i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defineClass(name,bytes,0,bytes.length);
    }
}
