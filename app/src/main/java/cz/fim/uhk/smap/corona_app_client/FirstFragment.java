package cz.fim.uhk.smap.corona_app_client;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import cz.fim.uhk.smap.corona_app_client.handler.MyHttpDataHandler;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.parse;

public class FirstFragment extends Fragment {

    private boolean locationPermissionGranted = false;
    private FusedLocationProviderClient locationProviderClient;
    private Location currentLocation;
    private ProgressBar progressBar;
    private String regionCode;

    private static final String TAG = "StartFragment";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentLocation = null;
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        view.findViewById(R.id.btn_locate_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // získání polohy zařízení
                if(isServicesOk()) {
                    // kontrola povolení k získání aktuální pozice
                    String[] locationPermission = {Manifest.permission.ACCESS_FINE_LOCATION};

                    if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                            FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // povolení již bylo uděleno -> získání polohy a kódu aktuálního kraje
                        locationPermissionGranted = true;
                        getPositionAndRegion();
                    } else {
                        // zažádání o povolení
                        ActivityCompat.requestPermissions(getActivity(),
                                locationPermission,
                                LOCATION_PERMISSION_REQUEST_CODE);
                    }
                }
            }
        });

        // zaslání informace o aktuálním kraji do hlavního zobrazovacího fragmentu
        // jestliže jsme úspěšně získali kód aktuálního kraje
        if(regionCode != null) {
            Bundle bundle = new Bundle();
            bundle.putString("regionCode", regionCode);
            // a redirect do tohoto fragmentu
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;

        // kontrola výsledku zažádání o povolení
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    // cyklus pro případ zvýšení počtu povolení (v tuhle chvíli pouze jedno)
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionGranted = false;
                            return;
                        }
                    }
                    // vše v pořádku lze získat polohu a kód kraje
                    locationPermissionGranted = true;
                    getPositionAndRegion();
                }
            }
        }
    }

    // metoda pro získání polohy a kódu aktuálního kraje
    private void getPositionAndRegion() {
        try {
            if (locationPermissionGranted) {
                // je-li uděleno povolení pro zisk polohy
                // požadavek na získání pozice
                locationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
                Task<Location> location = locationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Pozice úspěšně nalezena");
                            // uložení pozice
                            currentLocation = (Location) task.getResult();

                            // získání kraje ze souřadnic pomocí Geocoding API
                            new GetRegion().execute(String.format("%.4f, %.4f",
                                    currentLocation.getLatitude(),
                                    currentLocation.getLongitude()));
                            Log.d(TAG, "Kód aktuálního kraje: " + regionCode);
                        } else {
                            Log.d(TAG, "Aktuální pozice je null");
                            Toast.makeText(getContext(), "Nelze získat aktuální pozici",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Získání pozice selhalo: " + e.getMessage());
        }
    }

    // metoda pro kontrolu dostupnosti Google play services na cílovém zařízení
    private boolean isServicesOk() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());

        if(available == ConnectionResult.SUCCESS) {
            // Google play services je k dispozici - pozice může být získána
            Log.d(TAG, "isServicesOK: google services is OK");
            return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // objevil se řešitelný problém (stará verze Google play services atp.)
            Log.d(TAG, "isServicesOK: an error occurred but it is solvable");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            // neřešitelný problém (například zcela chybí podpora Google play services atp.)
            Toast.makeText(getContext(), "Nelze nalézt polohu uživatele.", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    // asynchronní vnitřní třída pro získání a zpracování dat z geocoding API (získaní názvu kraje)
    private class GetRegion extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                double lat = Double.parseDouble(strings[0].split(",")[0]);
                double lng = Double.parseDouble(strings[0].split(",")[1]);
                String response;
                // získání výsledku z API
                MyHttpDataHandler myHttpDataHandler = new MyHttpDataHandler();
                // API klíč z manifestu
                ApplicationInfo app = getContext().getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = app.metaData;
                String url = String
                        .format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%.4f,%.4f&key=%s", lat, lng, bundle.getString("com.google.android.geo.API_KEY"));
                response = myHttpDataHandler.getHttpData(url);
                return response;
            } catch (Exception e) {
                Log.e(TAG, "Nepodařilo se získat data z Geocoding API: " + e.getMessage());
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                /*JSONObject jsonObject = new JSONObject(s);
                // získání pouze názvu kraje z responsu
                JSONArray results = (JSONArray) jsonObject.get("results");
                JSONObject addressComponents = results.getJSONObject(0);
                // TODO predelat - ne vzdy je to na indexu 3 - nejak filterovat selektovat
                JSONObject regionObject = addressComponents.getJSONObject(3);
                String regionName= regionObject.getString("long_name");*/

                //List<String> regionName = JsonPath.read(s, "$.results.address_components[?(@types[0] == $['administrative_area_level_1'])].long_name");

                Filter cheapFictionFilter = filter(
                        where("types[0]").is("administrative_area_level_1")
                );
                Map<String, Object> regionNameMap =
                        parse(s).read("$.store.book[?]", cheapFictionFilter);
                // převod regionName na regionCode pro zaslání na server
                //regionCode = getRegionCode(regionNameMap.get(s));
            } catch (Exception e) {
                Log.e(TAG, "Nepodařilo se získat požadovaná data ze server: " + e.getMessage());
            }
            if(progressBar.isIndeterminate()) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
        // metoda pro transfer názvu kraje z API na příslušný kód kraje (nutné pro naše API)
        private String getRegionCode(String regionName) {
            String regionCode = "Nepodařilo se získat data";
            switch (regionName) {
                case "Hlavní město Praha" :
                    regionCode = "CZ010";
                    break;
                case "Jihočeský kraj":
                    regionCode = "CZ031";
                    break;
                case "Jihomoravský kraj":
                    regionCode = "CZ064";
                    break;
                case "Karlovarský kraj":
                    regionCode = "CZ041";
                    break;
                case "Královéhradecký kraj":
                    regionCode = "CZ052";
                    break;
                case "Liberecký kraj":
                    regionCode= "CZ051";
                    break;
                case "Moravskoslezský kraj":
                    regionCode = "CZ080";
                    break;
                case "Olomoucký kraj":
                    regionCode = "CZ071";
                    break;
                case "Pardubický kraj":
                    regionCode= "CZ053";
                    break;
                case "Plzeňský kraj":
                    regionCode = "CZ032";
                    break;
                case "Středočeský kraj":
                    regionCode = "CZ020";
                    break;
                case "Ústecký kraj":
                    regionCode = "CZ042";
                    break;
                case "Kraj Vysočina":
                    regionCode = "CZ063";
                    break;
                case "Zlínský kraj":
                    regionCode = "CZ072";
                    break;
            }
            return regionCode;
        }
    }
}