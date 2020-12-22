package cz.fim.uhk.smap.corona_app_client.handler;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MyHttpDataHandler {

    private static final String TAG = "MyHttpDataHandler";

    public MyHttpDataHandler() {}

    public String getHttpData(String requestUrl) {

        URL url;
        String response = "";

        try {
            url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            // je-li response v pořádku
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {

                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                // není-li response v  pořádku
                response = "";
                Log.e(TAG, "Nepodařilo se získat response ze serveru");
            }
        } catch (IOException e) {
            Log.e(TAG, "Chyba při navázají spojení se serverem: " + e.getMessage());
        }
        return response;
    }
}
