package cz.fim.uhk.smap.corona_app_client;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import cz.fim.uhk.smap.corona_app_client.api.CoronaServerAPI;
import cz.fim.uhk.smap.corona_app_client.model.CoronaInformation;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SecondFragment extends Fragment {

    private CoronaInformation coronaInformation;
    private TextView txtCoronaInfo;

    private static final String TAG = "SecondFragment";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // init
        txtCoronaInfo = (TextView) view.findViewById(R.id.textview_second);

        // obdržení dat ze startovního fragmentu
        String regionCode = getArguments().getString("regionCode");
        // request na server
        MainActivity mainActivity = (MainActivity) getActivity();
        Call<CoronaInformation> call = mainActivity.getCoronaServerAPI().getInformationByRegionCode(regionCode);

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<CoronaInformation>() {
            // TODO
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(Call<CoronaInformation> call, Response<CoronaInformation> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybovou hlášku a návrat z metody
                    Log.d(TAG, "nepodařilo se získat response: " + response.message());
                    return;
                }

                // uložení dat
                coronaInformation = response.body();

                // zobrazení získáných dat ze serveru
                visualizeData();

                for(int cislo : coronaInformation.getActualNumberOfCases()) {
                    Log.d(TAG, "nakazeni: " + cislo);
                }
                Log.d(TAG, "info ze serveru: " + coronaInformation.getLastDate());
            }
            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(Call<CoronaInformation> call, Throwable t) {
                Log.d(TAG, "chyba při zpracování požadavku: " + t.getMessage());
            }
        });

        view.findViewById(R.id.button_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);*/
                if(coronaInformation != null) {
                    System.out.println("TOTU");
                    txtCoronaInfo.setText("data jsou z: " + coronaInformation.getLastDate());
                }
            }
        });
    }

    // metoda pro vizualizaci dat
    private void visualizeData() {
        // zobrazení dat
        if(coronaInformation != null) {
            txtCoronaInfo.setText(coronaInformation.getLastDate());
        }
    }
}