package ke.co.calista.googlephotos.ui.main

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.dev.lishabora.Utils.OnclickRecyclerListener
import com.kogicodes.sokoni.models.custom.Status
import ke.co.calista.googlephotos.adapters.AlbumAdapter
import java.util.*
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.borax12.materialdaterangepicker.date.DatePickerDialog
import com.example.googlephotosdemo.R
import com.google.photos.library.v1.proto.*
import com.google.photos.types.proto.Album
import com.google.photos.types.proto.DateRange
import com.kogicodes.sokoni.models.custom.MediaType
import com.example.googlephotosdemo.GooglePhotoLauncherActivity
import com.example.googlephotosdemo.Utils.PreferenceHelper
import com.example.googlephotosdemo.Utils.PreferenceHelper.set

import ke.co.calista.googlephotos.models.MediaItemHolder
import kotlinx.android.synthetic.main.main_fragment.*


class MainFragment : Fragment(), DatePickerDialog.OnDateSetListener {

    lateinit var serverCode: String

    companion object {
        fun newInstance(serverCode: String?) = MainFragment()

    }

    private var mOrientation: RecyclerView.Orientation? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        loading(false, "")
        viewModel.observeAccessToken().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            PreferenceHelper.defaultPrefs(requireContext()).set(PreferenceHelper.ACCESS_TOKEN, it.data)

            setUpuI()
        })
        setUpuI()
    }


    private fun setUpuI() {
        main.visibility = GONE

        serverCode = (activity as GooglePhotoLauncherActivity).serverCode
        if (PreferenceHelper.defaultPrefs(requireContext()).getString(PreferenceHelper.ACCESS_TOKEN, "").isNullOrEmpty()) {
            viewModel.getAccessToken(serverCode)
        } else {
            main.visibility = VISIBLE
        }

        album.setOnClickListener { loadAlbums() }
        topic.setOnClickListener { loadImagesByTopic() }
        date.setOnClickListener { loadImagesByDate() }
    }


    private fun loadImagesByTopic() {
        context?.let {
            MaterialDialog(it).show {
                listItemsMultiChoice(R.array.filters) { dialog, indices, items ->
                    filter(indices)
                }
                positiveButton(R.string.select)
            }
        }
    }

    private fun loadImagesByDate() {
        val now = Calendar.getInstance()
        val dpd = DatePickerDialog.newInstance(
            this,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show(activity?.fragmentManager, "Datepickerdialog")
    }

    private fun loadAlbums() {


        viewModel.observeAlbums().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when {
                it.status == Status.LOADING -> {
                    album.isEnabled = false
                    it.message?.let { it1 -> loading(true, it1) }
                }
                it.status == Status.ERROR -> {
                    album.isEnabled = true
                    it.message?.let { it1 -> loading(false, it1) }
                }
                it.status == Status.SUCCESS -> {
                    album.isEnabled = true
                    it.message?.let { it1 -> loading(false, it1) }
                    it.data?.let { it1 ->
                        showAlbums(it1, object : MyInterface {
                            override fun onComplete(albumEntry: Album) {


                                activity?.supportFragmentManager?.beginTransaction()
                                    ?.addToBackStack("media")?.replace(
                                        R.id.container,
                                        FragmentMediaItems.newInstance(
                                            MediaItemHolder(
                                                MediaType.ALBUM,
                                                albumEntry.id,
                                                PreferenceHelper.defaultPrefs(requireContext()).getString(PreferenceHelper.ACCESS_TOKEN, "")
                                            )
                                        )
                                    )?.commit()

                            }
                        })
                    }
                }
            }
        })




        if (!PreferenceHelper.defaultPrefs(requireContext()).getString(PreferenceHelper.ACCESS_TOKEN, "").isNullOrEmpty()) {
            viewModel.getAlbums(PreferenceHelper.defaultPrefs(requireContext()).getString(PreferenceHelper.ACCESS_TOKEN, "") ?: "")
        } else {
            setUpuI()
        }


    }


    fun showAlbums(albums: LinkedList<Album>, myInterface: MyInterface) {

        requireActivity().runOnUiThread {


            val layoutInflaterAndroid = LayoutInflater.from(context)
            val mView = layoutInflaterAndroid.inflate(R.layout.dialog_albums, null)

            val alertDialogBuilderUserInput = context?.let { AlertDialog.Builder(it) }
            alertDialogBuilderUserInput?.setView(mView as View)
            alertDialogBuilderUserInput?.setTitle(getString(R.string.label_select_album))


            alertDialogBuilderUserInput?.setCancelable(true)
                ?.setPositiveButton(getString(R.string.label_continue), { dialogBox, id -> })
                ?.setNegativeButton(
                    getString(R.string.label_back),
                    { dialogBox, id -> dialogBox.cancel() })

            val alertDialogAndroid = alertDialogBuilderUserInput?.create()
            val recyclerView: RecyclerView


            recyclerView = mView.findViewById(R.id.recyclerView)
            val albumAdapter = context?.let {
                AlbumAdapter(it, albums, object : OnclickRecyclerListener {
                    override fun onClickListener(position: Int) {
                        alertDialogAndroid?.dismiss()
                        myInterface.onComplete(albums[position])
                    }


                })
            }


            val spanCount = 2
            val layoutManager = GridLayoutManager(context, spanCount)
            recyclerView.layoutManager = layoutManager


            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = albumAdapter


            albumAdapter?.notifyDataSetChanged()
            alertDialogAndroid?.show()


            val thePositive = alertDialogAndroid?.getButton(DialogInterface.BUTTON_POSITIVE)
            thePositive?.visibility = View.GONE
            val theNegative = alertDialogAndroid?.getButton(DialogInterface.BUTTON_NEGATIVE)
            theNegative?.visibility = View.GONE
        }
    }

    interface MyInterface {
        fun onComplete(albumEntry: Album)
    }


    fun loading(state: Boolean, msg: String) {
        if (state) {
            status.text = "" + msg + "......"
        } else {
            status.text = "" + msg + "......"
        }
    }

    override fun onDateSet(
        view: DatePickerDialog,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int,
        yearEnd: Int,
        monthOfYearEnd: Int,
        dayOfMonthEnd: Int
    ) {

        val daystart = com.google.type.Date.newBuilder()
            .setDay(dayOfMonth)
            .setMonth(monthOfYear + 1)
            .setYear(year)
            .build()
        val dayend = com.google.type.Date.newBuilder()
            .setDay(dayOfMonthEnd)
            .setMonth(monthOfYearEnd + 1)
            .setYear(yearEnd)
            .build()

        val dateRange = DateRange.newBuilder()
            .setStartDate(daystart)
            .setEndDate(dayend)
            .build()

        val dateFilter = DateFilter.newBuilder()
            .addRanges(dateRange)
            .build()
        val filters = Filters.newBuilder()
            .setDateFilter(dateFilter)
            .build()


        activity?.supportFragmentManager?.beginTransaction()?.addToBackStack("media")?.replace(
            R.id.container,
            FragmentMediaItems.newInstance(
                MediaItemHolder(
                    MediaType.FILTER,
                    filters,
                    PreferenceHelper.defaultPrefs(requireContext()).getString(PreferenceHelper.ACCESS_TOKEN, "")
                )
            )
        )?.commit()


    }

    private fun filter(s: IntArray) {
        val cp = ContentFilter.newBuilder()
        for (vs in s) {
            when (vs) {
                0 -> cp.addIncludedContentCategories(ContentCategory.PEOPLE)
                1 -> cp.addIncludedContentCategories(ContentCategory.BIRTHDAYS)
                2 -> cp.addIncludedContentCategories(ContentCategory.WEDDINGS)
                3 -> cp.addIncludedContentCategories(ContentCategory.SELFIES)
                4 -> cp.addIncludedContentCategories(ContentCategory.ANIMALS)
                5 -> cp.addIncludedContentCategories(ContentCategory.FOOD)
                6 -> cp.addIncludedContentCategories(ContentCategory.LANDMARKS)
                7 -> cp.addIncludedContentCategories(ContentCategory.SPORT)
                8 -> cp.addIncludedContentCategories(ContentCategory.PERFORMANCES)
                9 -> cp.addIncludedContentCategories(ContentCategory.LANDSCAPES)
            }
        }


        val filters = Filters.newBuilder()
            .setContentFilter(cp.build())
            .build()

        activity?.supportFragmentManager?.beginTransaction()?.addToBackStack("media")?.replace(
            R.id.container,
            FragmentMediaItems.newInstance(
                MediaItemHolder(
                    MediaType.FILTER,
                    filters,
                    PreferenceHelper.defaultPrefs(requireContext()).getString(PreferenceHelper.ACCESS_TOKEN, "")
                )
            )
        )?.commit()


    }


}
