package com.duellite.net;

import com.duellite.domain.Card;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Random;

public class YgoApiClient {
    private static final String RANDOM_URL = "https://db.ygoprodeck.com/api/v7/randomcard.php";
    private static final String INFO_URL = "https://db.ygoprodeck.com/api/v7/cardinfo.php";
    private final HttpClient http = HttpClient.newHttpClient();

    public Card fetchRandomMonster() throws IOException, InterruptedException {
        // 1) Intentar randomcard.php con validaciones
        for (int i = 0; i < 30; i++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(RANDOM_URL))
                    .header("User-Agent", "DuelLite/1.0")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) { Thread.sleep(80); continue; }
            JSONObject o;
            try { o = new JSONObject(resp.body()); } catch (Exception e) { Thread.sleep(40); continue; }
            String type = o.optString("type", "");
            if (!type.toLowerCase().contains("monster")) { Thread.sleep(30); continue; }
            if (!o.has("atk") || !o.has("def")) { Thread.sleep(30); continue; }
            String name = o.optString("name", "?");
            int atk = o.optInt("atk", 0);
            int def = o.optInt("def", 0);
            String imageUrl = null;
            JSONArray imgs = o.optJSONArray("card_images");
            if (imgs != null && imgs.length() > 0) {
                imageUrl = imgs.getJSONObject(0).optString("image_url", null);
            }
            if (imageUrl == null) { Thread.sleep(30); continue; }
            return new Card(name, atk, def, imageUrl);
        }
        // 2) Fallback: cardinfo.php filtrando tipos Monster con desplazamiento aleatorio
        String[] types = new String[]{
                "Normal Monster","Effect Monster","Ritual Monster","Fusion Monster",
                "Synchro Monster","XYZ Monster","Pendulum Effect Monster","Link Monster"
        };
        Random r = new Random();
        for (int i=0;i<25;i++){
            String t = types[r.nextInt(types.length)];
            int offset = r.nextInt(900);
            String url = INFO_URL + "?type=" + URLEncoder.encode(t, StandardCharsets.UTF_8) +
                    "&num=1&offset=" + offset;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "DuelLite/1.0")
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) { Thread.sleep(80); continue; }
            try {
                JSONObject root = new JSONObject(resp.body());
                if (!root.has("data")) { Thread.sleep(40); continue; }
                JSONArray data = root.getJSONArray("data");
                if (data.length()==0) { Thread.sleep(40); continue; }
                JSONObject o = data.getJSONObject(0);
                if (!o.has("atk") || !o.has("def")) { Thread.sleep(30); continue; }
                String name = o.optString("name", "?");
                int atk = o.optInt("atk", 0);
                int def = o.optInt("def", 0);
                String imageUrl = null;
                JSONArray imgs = o.optJSONArray("card_images");
                if (imgs != null && imgs.length() > 0) {
                    imageUrl = imgs.getJSONObject(0).optString("image_url", null);
                }
                if (imageUrl == null) { Thread.sleep(30); continue; }
                return new Card(name, atk, def, imageUrl);
            } catch (Exception ex){ Thread.sleep(50); }
        }
        throw new IOException("Unable to fetch a Monster card after many attempts (random+info)");
    }
}
