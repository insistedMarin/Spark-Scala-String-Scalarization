package MappingListString;

import org.json.JSONObject;
import org.json.JSONArray;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class Test {
    public static void main(String[] args) {
        String filePath = "src/data/map/room.json";
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

            // 检查连续性
            boolean isConsecutive = true;
            System.out.println(values);
            System.out.println(values.size());

            for (int i = 1; i < values.size(); i++) {
                if (values.get(i) - values.get(i - 1) != 1) {
                    isConsecutive = false;
                    break;
                }
            }

            if (isConsecutive) {
                System.out.println("Values are consecutive.");
            } else {
                System.out.println("Values are not consecutive.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
