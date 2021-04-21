package ymse3p.app.audiorecorder.data

import retrofit2.Response
import ymse3p.app.audiorecorder.data.network.RoadsApi
import ymse3p.app.audiorecorder.models.RoadsApiResponse
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val roadsApi: RoadsApi
) {
    suspend fun getSnappedPoints(path: String): Response<RoadsApiResponse> =
        roadsApi.getSnappedPoints(path)
}