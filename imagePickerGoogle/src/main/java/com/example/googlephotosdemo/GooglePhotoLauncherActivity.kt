package com.example.googlephotosdemo

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.googlephotosdemo.Utils.PhotosLibraryClientFactory
import com.example.googlephotosdemo.Utils.PreferenceHelper
import com.example.googlephotosdemo.Utils.PreferenceHelper.set
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.kogicodes.sokoni.models.custom.Status
import ke.co.calista.googlephotos.ui.main.MainFragment
import ke.co.calista.googlephotos.ui.main.MainViewModel
import kotlinx.android.synthetic.main.main_activity.*


class GooglePhotoLauncherActivity : AppCompatActivity() {


    companion object {
        const val KEY_RESULT_GOOGLE_PHOTOS = "KEY_RESULT_GOOGLE_PHOTOS"
        const val REQUEST_CODE_GOOGLE_PHOTOS = 1001
        const val KEY_REQUEST_CODE = "request_code"

        const val KEY_CLIENT_ID = "CLIENT_ID"
        const val KEY_CLIENT_TOKEN = "CLIENT_TOKEN"
        const val KEY_SERVER_KEY = "SERVER_KEY"
    }

    var serverCode: String = PhotosLibraryClientFactory.SERVER_KEY

    private lateinit var viewModel: MainViewModel
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        getExtra()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(
                PhotosLibraryClientFactory.CLIENT_ID,
                false
            )
            .requestScopes(Scope(PhotosLibraryClientFactory.SERVER_URL))
            .build()


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)



        sign_in_button.visibility = VISIBLE
        linearLayoutSigninHeader.visibility = VISIBLE
        val textView = sign_in_button.getChildAt(0) as TextView
        textView.text = getString(R.string.lable_conect_to_photos)
        viewModel.observeAccount().observe(this, Observer(function = {
            run {
                when {
                    it != null -> when {
                        it.status == Status.SUCCESS -> {

                            sign_in_button.visibility = GONE
                            linearLayoutSigninHeader.visibility = GONE
                            viewPhotos.visibility = VISIBLE
                            name.text = (it.data as GoogleSignInAccount).displayName
                            Log.d("sdfsd", serverCode)

                            if (it.data.photoUrl != null) {
                                Glide.with(this).load(it.data.photoUrl.toString()).into(image)
                            }
                            supportFragmentManager?.beginTransaction()
                                ?.replace(R.id.container, MainFragment.newInstance(serverCode))
                                ?.commit()


                        }
                        else -> {
                            sign_in_button.visibility = VISIBLE
                            linearLayoutSigninHeader.visibility = VISIBLE
                            viewPhotos.visibility = GONE

                        }
                    }
                    else -> {

                    }
                }

            }
        }))

        sign_in_button.setOnClickListener {
            if (PreferenceHelper.defaultPrefs(this).getString(PreferenceHelper.ACCESS_TOKEN, "")
                    .isNullOrEmpty()
            ) {
                signIn()
            } else {
                supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.container, MainFragment.newInstance(serverCode))
                    ?.commit()
                sign_in_button.visibility = GONE
                linearLayoutSigninHeader.visibility = GONE
            }

        }
        signout.setOnClickListener { signOut() }

        for (i in 0 until signout.getChildCount()) {
            val v = signout.getChildAt(i)

            if (v is TextView) {
                val tv = v as TextView
                tv.textSize = 14f
                tv.setTypeface(null, Typeface.NORMAL)
                tv.text = getString(R.string.label_logout)
                tv.setTextColor(Color.parseColor("#212121"))

                tv.setSingleLine(true)

                return
            }
        }
    }

    private fun getExtra() {
        PhotosLibraryClientFactory.CLIENT_ID = intent.getStringExtra(KEY_CLIENT_ID) ?: ""
        PhotosLibraryClientFactory.CLIENT_TOKEN = intent.getStringExtra(KEY_CLIENT_TOKEN) ?: ""
        PhotosLibraryClientFactory.SERVER_KEY = intent.getStringExtra(KEY_SERVER_KEY) ?: ""
    }


    fun setMainFragment() {
        supportFragmentManager?.beginTransaction()
            ?.replace(R.id.container, MainFragment.newInstance(serverCode))?.commit()

    }


    private fun signIn() {
        val intent: Intent = mGoogleSignInClient.getSignInIntent()
        resultLauncher.launch(intent)
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                updateUI(GoogleSignIn.getLastSignedInAccount(this))
                PreferenceHelper.defaultPrefs(this).set(PreferenceHelper.ACCESS_TOKEN, "")
            }
    }

    override fun onStart() {
        super.onStart()
        /*val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            signOut()
        } else {
            updateUI(null)
        }*/

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && PreferenceHelper.defaultPrefs(this)
                .getString(PreferenceHelper.ACCESS_TOKEN, "")
                .isNullOrEmpty()
        ) {
            signOut()
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {

        if (account != null) {
            viewModel.setIsSignedIn(account!!)
        } else {
            viewModel.setIsNotSignedIn()
        }
    }


    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            //  if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
            // }
        }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>?) {
        try {
            val account = task?.getResult(ApiException::class.java)
            serverCode = account?.serverAuthCode.toString()
            updateUI(account)
        } catch (e: ApiException) {
            updateUI(null)
        }
    }
}
