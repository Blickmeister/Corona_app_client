package cz.fim.uhk.smap.corona_app_client;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    private GraphView graph;
    private LineGraphSeries<DataPoint> actualDataSeries;
    private LineGraphSeries<DataPoint> futureDataSeries;

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
        graph = (GraphView) view.findViewById(R.id.graphView);

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

                /*for(int cislo : coronaInformation.getActualNumberOfCases()) {
                    Log.d(TAG, "nakazeni: " + cislo);
                }
                Log.d(TAG, "info ze serveru: " + coronaInformation.getLastDate());*/
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
        List<Integer> actualNumberOfCases = coronaInformation.getActualNumberOfCases();
        List<Double> futureNumberOfCases = coronaInformation.getFutureNumberOfCases();
        // nastavení a init grafu
        try {
            graph.getViewport().setXAxisBoundsManual(true);
            // nastavení počátečního a koncového času sledovaného vývoje epidemie
            String lastDateString = coronaInformation.getLastDate();
            Calendar cal = Calendar.getInstance();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            cal.setTime(format.parse(lastDateString));
            cal.add(Calendar.DAY_OF_YEAR, -actualNumberOfCases.size());
            graph.getViewport().setMinX(cal.getTime().getTime());
            String startDate = format.format(cal.getTime());

            //Date lastDate = format.parse(lastDateString);
            //Date date = addDays(lastDate, -actualNumberOfCases.size());

            //String startDate = format.format(cal.getTime());
            //Date lastDate = format.parse(lastDateString);
            //LocalDateTime lastLocaleDate = LocalDateTime.ofInstant(lastDate.toInstant(), ZoneId.systemDefault());
            Log.d(TAG, "start date: " + startDate);

            cal.add(Calendar.DAY_OF_YEAR, (actualNumberOfCases.size()+futureNumberOfCases.size()));
            graph.getViewport().setMaxX(cal.getTime().getTime());
            String endDate = format.format(cal.getTime());
            Log.d(TAG, "end date: " + endDate);

            graph.setTitle("Kumulativní vývoj počtu nakažených v období od " + startDate + " do " + endDate);
            graph.setTitleTextSize(30f);

            // nastavení dat grafu
            actualDataSeries = new LineGraphSeries<>(getDataForActualDataSeries(actualNumberOfCases, format, startDate));
            futureDataSeries = new LineGraphSeries<>(getDataForFutureDataSeries(futureNumberOfCases, format, lastDateString));
            graph.addSeries(actualDataSeries);
            graph.addSeries(futureDataSeries);
            actualDataSeries.setColor(Color.RED);
            futureDataSeries.setColor(Color.YELLOW);

            // hranice grafu

            // nastavení labels grafu
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));
            graph.getGridLabelRenderer().setNumHorizontalLabels(10); // omezení prostorem proto jen 4
            graph.getGridLabelRenderer().setNumVerticalLabels(10);
            //graph.getGridLabelRenderer().setHumanRounding(false);
            //graph.getViewport().setDrawBorder(true);

            GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
            //gridLabel.setVerticalLabelsVisible(false);
            //gridLabel.setHorizontalLabelsVisible(false);
            gridLabel.setTextSize(25f);
            gridLabel.setGridStyle(GridLabelRenderer.GridStyle.BOTH);
            gridLabel.setHorizontalLabelsAngle(90);
            gridLabel.setLabelsSpace(20);
            gridLabel.setGridColor(Color.RED);
            gridLabel.setHorizontalAxisTitleTextSize(30f);
            gridLabel.setVerticalAxisTitleTextSize(30f);
            gridLabel.setHorizontalAxisTitle("Datum");
            gridLabel.setVerticalAxisTitle("Kumulativní počet nakažených");

        } catch (Exception e) {
            Log.e(TAG, "Chyba při vytváření grafu: " + e.getMessage());
        }
        // zobrazení dat
        /*if(coronaInformation != null) {
            txtCoronaInfo.setText(coronaInformation.getLastDate());
        }*/
    }

    // metoda pro získání a zpracování aktuálních dat pro graf
    private DataPoint[] getDataForActualDataSeries(List<Integer> actualCases, DateFormat format, String startDate) throws ParseException {
            Collections.reverse(actualCases);
            int size = actualCases.size();
            Calendar cal = Calendar.getInstance();
            cal.setTime(format.parse(startDate));
            DataPoint[] values = new DataPoint[size];
            for (int i = 0; i < size; i++) {
                DataPoint v = new DataPoint(cal.getTime(), actualCases.get(i));
                values[i] = v;
                cal.add(Calendar.DATE, 1);
                Log.d(TAG, "actual value: " + cal.getTime() + " " + actualCases.get(i));
            }
            return values;
    }

    // metoda pro získání a zpracování budoucích predikovaných dat pro graf
    private DataPoint[] getDataForFutureDataSeries(List<Double> futureCases, DateFormat format, String lastDate) throws ParseException {
        int size = futureCases.size();
        Calendar cal = Calendar.getInstance();
        cal.setTime(format.parse(lastDate));
        DataPoint[] values = new DataPoint[size];
        for (int i = 0; i < size; i++) {
            DataPoint v = new DataPoint(cal.getTime(), futureCases.get(i));
            values[i] = v;
            Log.d(TAG, "future value: " + cal.getTime() + " " + futureCases.get(i));
            cal.add(Calendar.DATE, 1);
        }
        return values;
    }

    private Date addDays(Date d, int days) {
        d.setTime(d.getTime() + (long) days*1000*60*60*24);
        return d;
    }
}