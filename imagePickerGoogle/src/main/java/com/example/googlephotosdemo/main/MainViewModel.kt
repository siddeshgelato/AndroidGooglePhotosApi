package ke.co.calista.googlephotos.ui.main

import androidx.lifecycle.*
import android.app.Application
import android.os.SystemClock
import com.example.googlephotosdemo.R
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.photos.library.v1.PhotosLibraryClient
import com.google.photos.library.v1.proto.Filters
import com.google.photos.types.proto.Album
import com.google.photos.types.proto.MediaItem
import com.kogicodes.sokoni.models.custom.Resource
import com.squareup.okhttp.*
import com.example.googlephotosdemo.Utils.PhotosLibraryClientFactory
import com.example.googlephotosdemo.Utils.PreferenceHelper
import com.example.googlephotosdemo.Utils.PreferenceHelper.set
import com.google.api.gax.rpc.UnauthenticatedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {


    internal var client: PhotosLibraryClient? = null
    internal var applicationc: Application? = null

    internal var mAccessToken: String = ""
    var mTokenExpired: Long = 0

    private val accountObserver = MutableLiveData<Resource<GoogleSignInAccount>>()
    private val albumsObservable = MutableLiveData<Resource<LinkedList<Album>>>()
    private val mediaItemObservable = MutableLiveData<Resource<List<MediaItem>>>()
    private val mediaItemFilteredObservable = MutableLiveData<Resource<List<MediaItem>>>()
    private val accessTokenObservable = MutableLiveData<Resource<String>>()

    init {
        applicationc = application

    }

    fun setIsSignedIn(account: GoogleSignInAccount) {

        accountObserver.value = Resource.success("", account)
    }

    fun setIsNotSignedIn() {
        accountObserver.value =
            Resource.error(applicationc?.getString(R.string.msg_logged_out) ?: "", null)

    }


    fun observeAccount(): LiveData<Resource<GoogleSignInAccount>> {
        return accountObserver
    }


    fun observeAlbums(): LiveData<Resource<LinkedList<Album>>> {
        return albumsObservable
    }

    fun getAlbums(googleSignInAccount: String) {
        albumsObservable.value =
            Resource.loading(applicationc?.getString(R.string.msg_loading_albums) ?: "", null)

        GlobalScope.launch(context = Dispatchers.Main) {


            client = googleSignInAccount?.let { it1 ->
                PhotosLibraryClientFactory.createClient(googleSignInAccount)
            }
            val response = try {
                 client?.listAlbums()
            } catch (e: UnauthenticatedException) {
                PreferenceHelper.defaultPrefs(applicationc?.applicationContext!!).set(PreferenceHelper.ACCESS_TOKEN, "")
                accountObserver.value =
                    Resource.error(applicationc?.getString(R.string.msg_logged_out) ?: "", null)
                return@launch
            }
            val albums = LinkedList<Album>()

            if (response != null) {
                for (album in response.iterateAll()) {
                    albums.add(album)
                }
                if (albums.size > 0) {
                    albumsObservable.postValue(Resource.success("", albums))
                } else {
                    albumsObservable.postValue(
                        Resource.error(
                            applicationc?.getString(R.string.msg_no_album) ?: "", albums
                        )
                    )

                }
            } else {
                albumsObservable.postValue(
                    Resource.error(
                        applicationc?.getString(R.string.error_no_response) ?: "", albums
                    )
                )

            }
        }


    }


    fun observeMediaItem(): LiveData<Resource<List<MediaItem>>> {
        return mediaItemObservable
    }

    fun getMediaItem(googleSignInAccount: String, albumId: String) {
        mediaItemObservable.value =
            Resource.loading(applicationc?.getString(R.string.msg_loading_images) ?: "", null)
        GlobalScope.launch(context = Dispatchers.Main) {


            client = googleSignInAccount.let { it1 -> PhotosLibraryClientFactory.createClient(it1) }

            val response = client?.searchMediaItems(albumId)

            val mediaItems = LinkedList<MediaItem>()

            if (response != null) {
                for (m in response.iterateAll()) {
                    mediaItems.add(m)
                }
            } else {
                mediaItemObservable.postValue(
                    Resource.error(
                        applicationc?.getString(R.string.error_no_response) ?: "", mediaItems
                    )
                )

            }
            if (mediaItems.size > 0) {
                mediaItemObservable.postValue(Resource.success("", mediaItems))
            } else {
                mediaItemObservable.postValue(
                    Resource.error(
                        applicationc?.getString(R.string.error_no_media) ?: "", mediaItems
                    )
                )

            }


        }
    }


    fun getMediaItemFiltered(googleSignInAccount: String, filters: Filters) {
        mediaItemObservable.value =
            Resource.loading(applicationc?.getString(R.string.msg_loading_images) ?: "", null)
        GlobalScope.launch(context = Dispatchers.Main) {

            client =
                googleSignInAccount?.let { it1 -> PhotosLibraryClientFactory.createClient(it1) }

            val response = client?.searchMediaItems(filters)

            val mediaItems = LinkedList<MediaItem>()

            if (response != null) {
                for (m in response.iterateAll()) {
                    mediaItems.add(m)
                }
            }
            if (mediaItems.size > 0) {
                mediaItemObservable.postValue(Resource.success("", mediaItems))
            } else {
                mediaItemObservable.postValue(
                    Resource.error(
                        applicationc?.getString(R.string.error_no_media) ?: "", mediaItems
                    )
                )

            }
        }


    }

    fun getAccessToken(googleSignInAccount: String) {

        GlobalScope.launch(context = Dispatchers.Main) {

            val client = OkHttpClient()
            val requestBody = FormEncodingBuilder()
                .add("grant_type", "authorization_code")
                .add(
                    "client_id",
                    PhotosLibraryClientFactory.CLIENT_ID
                )
                .add("client_secret", PhotosLibraryClientFactory.CLIENT_TOKEN)
                .add("redirect_uri", "")
                .add("code", googleSignInAccount)
                .build()
            val request = Request.Builder()
                .url(PhotosLibraryClientFactory.TOKEN_URL)
                .post(requestBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(request: Request, e: IOException) {

                }

                @Throws(IOException::class)
                override fun onResponse(response: Response) {
                    try {
                        val jsonObject = JSONObject(response.body().string())

                        mTokenExpired =
                            SystemClock.elapsedRealtime() + jsonObject.optLong("expires_in") * 1000

                        accessTokenObservable.postValue(
                            Resource.success(
                                "",
                                jsonObject.optString("access_token")
                            )
                        )


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            })


        }


    }

    fun observeAccessToken(): LiveData<Resource<String>> {
        return accessTokenObservable
    }

}






