package MappingListString;

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapTransform {
    public static void main(String[] args) {
        String filePath = "src/data/map/equipment_type.json";
        try {
            // 读取文件内容
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONObject jsonObj = new JSONObject(content);

            List<Integer> values = new ArrayList<>();

            // 从每个键中提取"value"的值
            for (String key : jsonObj.keySet()) {
                JSONObject obj = jsonObj.getJSONObject(key);
                values.add(obj.getInt("value"));
            }

            // 对列表进行排序
            Collections.sort(values);
            String fileOutPut = "src/data/map/equipment_type_array.csv";
            int[] array = values.stream().mapToInt(i->i).toArray();
            saveArrayToCSV(array, fileOutPut);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void saveArrayToCSV(int[] array, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (int i = 0; i < array.length; i++) {
                writer.write(Integer.toString(array[i]));

                // 如果不是最后一个元素，则添加逗号
                if (i < array.length - 1) {
                    writer.write(",");
                }
            }
            System.out.println("Array saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
