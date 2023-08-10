import java.nio.file.Files;
import java.nio.file.Paths;

public class LoadMap {
    public String[] load(String path){

        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            return content.split(",");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[0];
    }
}
