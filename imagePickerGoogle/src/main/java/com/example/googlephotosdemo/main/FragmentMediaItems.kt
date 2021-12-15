package ke.co.calista.googlephotos.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dev.lishabora.Utils.OnclickRecyclerListener
import com.example.googlephotosdemo.R
import com.kogicodes.sokoni.models.custom.MediaType
import com.kogicodes.sokoni.models.custom.Status
import com.example.googlephotosdemo.GooglePhotoLauncherActivity

import ke.co.calista.googlephotos.adapters.GridAdapter
import ke.co.calista.googlephotos.models.MediaItemHolder
import ke.co.calista.googlephotos.models.MediaItemObj

import kotlinx.android.synthetic.main.media_items_fragment.*
import java.util.*


class FragmentMediaItems : Fragment() {


    lateinit var mediaObject: MediaItemHolder
    private var gridAdapter: GridAdapter? = null
    private var mStaggeredLayoutManager: StaggeredGridLayoutManager? = null

    private  var mediaItems: LinkedList<MediaItemObj>? = LinkedList()
    //private  var mediaItems1: LinkedList<MediaItemObj>? = LinkedList()

    private var selectedItem: MediaItemObj? = null

    companion object {
        fun newInstance(mediaObject: MediaItemHolder) = FragmentMediaItems().apply {
            arguments = Bundle().apply {
                putSerializable("mediaType", mediaObject)
            }
        }
    }

    private var mOrientation: RecyclerView.Orientation? = null
    private lateinit var viewModel: MainViewModel
   /* fun fromSelected(gdModel: MediaItemObj) {
        if(mediaItems1==null){
            mediaItems1= LinkedList();
        }
        mediaItems1!!.remove(gdModel)
    }

    fun toSelected(gdModel: MediaItemObj) {


        if(mediaItems1==null){
            mediaItems1= LinkedList();
        }
        mediaItems1!!.add(gdModel)


    }*/
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.media_items_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        mediaObject= arguments?.getSerializable("mediaType") as MediaItemHolder


        setUpList()
        viewModel.observeMediaItem().observe(viewLifecycleOwner, Observer {


            if(it.status==Status.LOADING){
                it.message?.let { it1 -> loading(true, it1) }
            }else if(it.status==Status.ERROR){
                it.message?.let { it1 -> loading(false, it1) }
            }else if(it.status==Status.SUCCESS){
                loading(false, "")
                for(m  in it.data!!){
                    Log.d("sfsf",m.baseUrl+ "=w480-h480")
                    mediaItems?.add(MediaItemObj(m.baseUrl+ "=w480-h480",false))
                }
                setUpList()
            }
        })


        if (mediaObject.type == MediaType.ALBUM){

            mediaObject.albumId?.let { mediaObject.token?.let { it1 -> viewModel.getMediaItem(it1, it) } }
        }else if(mediaObject.type==MediaType.FILTER){
            mediaObject.filter?.let { mediaObject.token?.let { it1 -> viewModel.getMediaItemFiltered(it1, it) } }

        }


        btn_okay.setOnClickListener { download() }
        btn_back.setOnClickListener({ (activity as GooglePhotoLauncherActivity).setMainFragment()})
    }


    private fun download() {
        if(selectedItem != null){

            download(selectedItem!!)
        }else{
            Toast.makeText(context,getString(R.string.error_no_image_selected),Toast.LENGTH_LONG).show()
        }
    }

    private fun download(mediaItems: MediaItemObj) {
        val resultIntent = Intent()
        resultIntent.putExtra(GooglePhotoLauncherActivity.KEY_RESULT_GOOGLE_PHOTOS,  mediaItems.url)
        resultIntent.putExtra(GooglePhotoLauncherActivity.KEY_REQUEST_CODE, GooglePhotoLauncherActivity.REQUEST_CODE_GOOGLE_PHOTOS)
        activity?.setResult(Activity.RESULT_OK, resultIntent)
        activity?.finish()
    }

    fun loading(state : Boolean, msg : String){

        if(state){
            status.text= "$msg......"
        }else{
            status.text= "$msg......"

        }
    }
    fun setUpList(){
        if(mediaItems==null){
            mediaItems= LinkedList()
        }

        gridAdapter= context?.let {
            GridAdapter(it, mediaItems!!,object : OnclickRecyclerListener{
            override fun onClickListener(position: Int) {

               /* if(mediaItems!!.get(position).selected!!){
                    mediaItems!![position].selected=false
                    fromSelected(mediaItems!![position])

                    gridAdapter?.setDataChange(position, mediaItems!![position])

                }else{
                    mediaItems!![position].selected=true

                    toSelected(mediaItems!![position])

                    gridAdapter?.setDataChange(position, mediaItems!![position])
                }*/
                var prevPos = 0
                if(selectedItem != null) {
                     prevPos = mediaItems?.indexOf(selectedItem) ?: 0;
                }
                if (selectedItem?.selected == true) {
                    selectedItem?.selected = false

                    selectedItem = mediaItems?.get(position)
                    selectedItem?.selected = true
                } else {
                    selectedItem = mediaItems?.get(position)
                    selectedItem?.selected = true
                }
                gridAdapter?.setDataChange(position, mediaItems!![position])
                gridAdapter?.setDataChange(prevPos , mediaItems!![prevPos])

            }
        })
        }

        mStaggeredLayoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.layoutManager = mStaggeredLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        gridAdapter?.notifyDataSetChanged()
        recyclerView.adapter = gridAdapter

    }

}
