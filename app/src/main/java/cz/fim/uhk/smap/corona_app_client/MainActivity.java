package cz.fim.uhk.smap.corona_app_client;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import cz.fim.uhk.smap.corona_app_client.api.CoronaServerAPI;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private CoronaServerAPI coronaServerAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // init gson converteru (JSON -> Java)
        Gson gson = new GsonBuilder()
                .create();

        // init a nastavení retrofit objektu pro připojení k serveru
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/") // localhost alias pro AVD
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // naplnění těl metod prostřednictvím retrofit objektu
        coronaServerAPI = retrofit.create(CoronaServerAPI.class);

        // menu ikona
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.menu_icon2);
        toolbar.setOverflowIcon(drawable);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public CoronaServerAPI getCoronaServerAPI() {
        return coronaServerAPI;
    }
}