package cz.fim.uhk.smap.corona_app_client.api;

import cz.fim.uhk.smap.corona_app_client.model.CoronaInformation;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CoronaServerAPI {

    @GET("corona/info/region/{code}")
    Call<CoronaInformation> getInformationByRegionCode(@Path("code") String regionCode);
}
