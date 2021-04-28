package ymse3p.app.audiorecorder.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ymse3p.app.audiorecorder.models.RoadsApiResponse
import ymse3p.app.audiorecorder.util.Constants.Companion.API_KEY
import java.util.*

interface RoadsApi {
    @GET("snapToRoads")
    suspend fun getSnappedPoints(
        @Query("path") path: String,
        @Query("quotaUser") quotaUser: String = UUID.randomUUID().toString(),
        @Query("interpolate") interpolate: String = "true",
        @Query("key") apiKey: String = API_KEY
    ): Response<RoadsApiResponse>
}