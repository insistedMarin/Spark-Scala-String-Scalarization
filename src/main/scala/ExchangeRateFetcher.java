import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.json.*;
import java.time.*;

public class ExchangeRateFetcher {

    private static final String API_URL = "http://apilayer.net/api/live?access_key=71cb6d1a396d611c1f5296e77ef6c60b&currencies=MUR,XPF,EUR&source=USD&format=1";
    private static final String JSON_FILE_PATH = "src/data/rates.json";
    public static JsonObject getRates() throws IOException {
        File jsonFile = new File(JSON_FILE_PATH);
        JsonObject rates = null;

        if (jsonFile.exists()) {
            try (Reader reader = new FileReader(jsonFile)) {
                JsonReader jsonReader = Json.createReader(reader);
                rates = jsonReader.readObject();
                jsonReader.close();
            }

            long timestamp = rates.getJsonNumber("timestamp").longValue();
            Instant then = Instant.ofEpochSecond(timestamp);
            Duration duration = Duration.between(then, Instant.now());

            if (duration.toDays() >= 1) {
                rates = updateRates();
                saveRates(rates);
            }
        }else {
            rates = updateRates();
            saveRates(rates);
        }

        return rates;

    }

    private static void saveRates(JsonObject rates) throws IOException {
        File jsonFile = new File(JSON_FILE_PATH);
        try (Writer writer = new FileWriter(jsonFile)) {
            JsonWriter jsonWriter = Json.createWriter(writer);
            jsonWriter.writeObject(rates);
            jsonWriter.close();
        }
    }

    private static JsonObject updateRates() throws IOException {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (Reader reader = new InputStreamReader(connection.getInputStream())) {
            JsonReader jsonReader = Json.createReader(reader);
            JsonObject rates = jsonReader.readObject();
            jsonReader.close();

            try (Writer writer = new FileWriter(JSON_FILE_PATH)) {
                JsonWriter jsonWriter = Json.createWriter(writer);
                jsonWriter.writeObject(rates);
                jsonWriter.close();
            }

            return rates;
        }
    }
}