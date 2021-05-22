package ymse3p.app.voicelogger.data

import retrofit2.Response
import ymse3p.app.voicelogger.data.network.RoadsApi
import ymse3p.app.voicelogger.models.RoadsApiResponse
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val roadsApi: RoadsApi
) {
    suspend fun getSnappedPoints(path: String): Response<RoadsApiResponse> =
        roadsApi.getSnappedPoints(path)
}