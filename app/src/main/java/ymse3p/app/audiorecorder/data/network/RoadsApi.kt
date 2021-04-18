package ymse3p.app.audiorecorder.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap
import ymse3p.app.audiorecorder.models.SnappedPoints
import ymse3p.app.audiorecorder.util.Constants.Companion.API_KEY

interface RoadsApi {
    @GET("snapToRoads")
    suspend fun getSnappedPoints(
        @QueryMap queries: Map<String, String>,
        @Query("key") apiKey: String = API_KEY,
        @Query("interpolate") interpolate: String = "true"
    ): Response<SnappedPoints>
}