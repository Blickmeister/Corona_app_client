package cz.fim.uhk.smap.corona_app_client;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cz.fim.uhk.smap.corona_app_client.model.CoronaInformation;
import cz.fim.uhk.smap.corona_app_client.notification.NotificationReceiver;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SecondFragment extends Fragment {

    private CoronaInformation coronaInformation;
    private TextView txtDataFrom;
    private TextView txtDeaths;
    private TextView txtCured;
    private Button btnOtherInfo;
    private TextView txtRegion;
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

        graph = (GraphView) view.findViewById(R.id.graphView);
        txtRegion = (TextView) view.findViewById(R.id.textViewRegion);
        txtDataFrom = (TextView) view.findViewById(R.id.txt_data_from);
        txtDeaths = (TextView) view.findViewById(R.id.txt_number_deaths);
        txtCured = (TextView) view.findViewById(R.id.txt_number_cured);
        btnOtherInfo = (Button) view.findViewById(R.id.button_second);

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
            }
            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(Call<CoronaInformation> call, Throwable t) {
                Log.d(TAG, "chyba při zpracování požadavku: " + t.getMessage());
            }
        });
    }

    // metoda pro vizualizaci dat
    private void visualizeData() {
        txtRegion.setText(txtRegion.getText() + coronaInformation.getRegionName());

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
            Log.d(TAG, "start date: " + startDate);

            cal.add(Calendar.DAY_OF_YEAR, (actualNumberOfCases.size()+futureNumberOfCases.size()));
            graph.getViewport().setMaxX(cal.getTime().getTime());
            String endDate = format.format(cal.getTime());
            Log.d(TAG, "end date: " + endDate);

            graph.setTitle("Kumulativní vývoj počtu nakažených v období od " + startDate + " do " + endDate);
            graph.setTitleColor(Color.BLACK);
            graph.setTitleTextSize(28f);

            // nastavení dat grafu
            actualDataSeries = new LineGraphSeries<>(getDataForActualDataSeries(actualNumberOfCases, format, startDate));
            futureDataSeries = new LineGraphSeries<>(getDataForFutureDataSeries(futureNumberOfCases, format, lastDateString));
            graph.addSeries(actualDataSeries);
            graph.addSeries(futureDataSeries);
            actualDataSeries.setColor(Color.DKGRAY);
            futureDataSeries.setColor(Color.RED);

            // nastavení labels grafu
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));
            graph.getGridLabelRenderer().setNumHorizontalLabels(8); // omezení prostorem proto jen 8
            graph.getGridLabelRenderer().setNumVerticalLabels(8);

            GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
            gridLabel.setTextSize(30f);
            gridLabel.setGridStyle(GridLabelRenderer.GridStyle.BOTH);
            gridLabel.setLabelsSpace(15);
            gridLabel.setHorizontalLabelsVisible(false);
            gridLabel.setGridColor(Color.RED);
            gridLabel.setHorizontalAxisTitleTextSize(30f);
            gridLabel.setVerticalAxisTitleTextSize(30f);
            gridLabel.setHorizontalAxisTitle("Datum");
            gridLabel.setVerticalAxisTitle("Kumulativní počet nakažených");

            // vykreslení čárkované oddělovací čáry
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setPathEffect(new DashPathEffect(new float[]{8,5},0));
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(getDataForDivideLine(format));
            series.setDrawAsPath(true);
            series.setCustomPaint(paint);
            graph.addSeries(series);

        } catch (Exception e) {
            Log.e(TAG, "Chyba při vytváření grafu: " + e.getMessage());
        }
        // zobrazení dat
        if(coronaInformation != null) {
            txtDataFrom.setText(txtDataFrom.getText() + coronaInformation.getLastDate());
            txtDeaths.setText(txtDeaths.getText() + String.valueOf(coronaInformation.getNumberOfDeath()));
            txtCured.setText(txtCured.getText() + String.valueOf(coronaInformation.getNumberOfCured()));
        }

        // řešení notifikace
        sendNotification(futureNumberOfCases);

        // redirect na webovou stránku ministerstva
        btnOtherInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse("https://onemocneni-aktualne.mzcr.cz/covid-19"));
                startActivity(viewIntent);
            }
        });
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
    // metoda pro získání řady pro rozdělovací linii
    private DataPoint[] getDataForDivideLine(DateFormat format) throws ParseException {
        int size = 2;
        DataPoint[] values = new DataPoint[size];
        Date lastDate = format.parse(coronaInformation.getLastDate());
        // nalezení nejvyšší hodnoty grafu
        Double highestValue = 0.0;
        for(Double val : coronaInformation.getFutureNumberOfCases()) {
            if(val >= highestValue) highestValue = val;
        }
        for(Integer val : coronaInformation.getActualNumberOfCases()) {
            if(val >= highestValue) highestValue = Double.valueOf(val);
        }
        DataPoint p1 = new DataPoint(lastDate, 0);
        DataPoint p2 = new DataPoint(lastDate, highestValue);
        values[0] = p1;
        values[1] = p2;
        return values;
    }

    // metoda řešící zasílání notifikace pokud je aktuální stav horší než se předpokládalo
    private void sendNotification(List<Double> futureNumberOfCases) {
        try {
            Calendar calendarLastDatePlusOneDay = Calendar.getInstance();
            Calendar calendarActualLastDate = Calendar.getInstance();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

            // pro zjednodušení se porovnává první predikovaná hodnota z předešlého dne s poslední
            // aktuální hodnotou z aktuálního dne - ideální pro využizí SharedPreferences API
            // je možné rozšíření pro všechny predikované dny (zde by se již vyplatilo použití DB či souboru)
            Context context = getActivity();
            SharedPreferences sharedPref = context.getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            // získání hodnot
            Double predictedValue = (double) sharedPref.getFloat(getString(R.string.predicted_number_of_cases_key),
                    0);
            String lastDate = sharedPref.getString(getString(R.string.last_date_key),
                    null);
            String regionCode = sharedPref.getString(getString(R.string.actual_region_code_key),
                    null);

            // je-li jakákoliv hodnota null -> appka běží poprvé -> uložíme hodnoty
            SharedPreferences.Editor editor = sharedPref.edit();
            if (predictedValue == 0 || lastDate == null || regionCode == null) {
                editor.putFloat(getString(R.string.predicted_number_of_cases_key), futureNumberOfCases.get(0).floatValue());
                editor.putString(getString(R.string.last_date_key), coronaInformation.getLastDate());
                editor.putString(getString(R.string.actual_region_code_key), coronaInformation.getRegionCode());
                editor.apply();
            } else {

                // nesedí-li regionCode -> změnil se uživatelův region -> přepíšeme hodnoty
                if (!regionCode.equals(coronaInformation.getRegionCode())) {
                    editor.putString(getString(R.string.actual_region_code_key), coronaInformation.getRegionCode());

                    // změnilo-li se rovněž datum
                    if (!lastDate.equals(coronaInformation.getLastDate())) {
                        editor.putFloat(getString(R.string.predicted_number_of_cases_key), futureNumberOfCases.get(0).floatValue());
                        editor.putString(getString(R.string.last_date_key), coronaInformation.getLastDate());
                    }
                } else {

                    // jinak je-li právě o jeden den více -> porovnání hodnot -> případná notifikace
                    calendarLastDatePlusOneDay.setTime(format.parse(lastDate));
                    calendarLastDatePlusOneDay.add(Calendar.DAY_OF_YEAR, 1);
                    calendarActualLastDate.setTime(format.parse(coronaInformation.getLastDate()));
                    String testDate = "2020-12-23";
                    calendarLastDatePlusOneDay.setTime(format.parse(testDate));
                    if ((calendarLastDatePlusOneDay.getTime().
                            compareTo(calendarActualLastDate.getTime())) == 0) {
                        // zvednul-li se počet nakažených oproti předpovědi z předešlého dne
                        // aktivace zaslání upozornění notifikací, případně změna notifikace
                        String text = "";
                        // text notifikace
                        if(coronaInformation.getActualNumberOfCases().get(0) > predictedValue) {
                            text = "Vývoj epidemie je horší než se předpokládalo";
                        } else {
                            text = "Vývoj epidemie není horší než se předpokládalo";
                        }
                        setNotification(text);
                    } else {

                        // jinak přepsat datum a akt hodnotu (uživatel nezapl aplikaci následující den)
                        editor.putFloat(getString(R.string.predicted_number_of_cases_key), futureNumberOfCases.get(0).floatValue());
                        editor.putString(getString(R.string.last_date_key), coronaInformation.getLastDate());
                    }
                }
            }
            editor.apply();
        } catch (Exception ex) {
            Log.e(TAG, "Vyskytla se chyba pří řešení notifikací: " + ex.getMessage());
        }
    }

    private void setNotification(String text) {
        // nastavení času zasílání
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 16);
        calendar.set(Calendar.MINUTE, 00);
        Log.d(TAG, "notifikace nastav");


        // vytvoření notifikace
        Intent intent = new Intent(getContext(), NotificationReceiver.class);
        intent.putExtra("notification_text", text);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(),
                100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

    }
}