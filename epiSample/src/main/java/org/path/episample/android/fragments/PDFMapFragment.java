package org.path.episample.android.fragments;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.OnNetworkActiveListener;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.path.common.android.data.CensusModel;
import org.path.common.android.data.PlaceModel;
import org.path.common.android.utilities.CensusUtil;
import org.path.common.android.utilities.PlaceNameUtil;
import org.path.common.android.utilities.WebLogger;
import org.path.episample.android.R;
import org.path.episample.android.activities.MainMenuActivity;
import org.path.episample.android.activities.ODKActivity;
import org.path.episample.android.provider.DirectionProvider;
import org.path.episample.android.provider.DirectionProvider.DirectionEventListener;
import org.path.episample.android.provider.DirectionProvider.LocationEventListener;
import org.path.episample.android.tasks.CensusListLoader;
import org.path.episample.android.utilities.DistanceUtil;
import org.path.episample.android.views.CompassView;
import org.path.episample.android.views.PdfView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static pdfMapExtractor.ExtractMetaData.ParsePDF;


public class PDFMapFragment extends Fragment implements DirectionEventListener,OnNetworkActiveListener, LocationEventListener, LoaderManager.LoaderCallbacks<List<CensusModel>> {
    public static class myObject {
        myObject(ArrayList<Float> mGpsCords, Bitmap originalBitmap ){
            savedGpsCords = new ArrayList<Float>();
            savedBitmap = originalBitmap;
            savedGpsCords = mGpsCords;
        }
        private ArrayList<Float> getGpsList (){return savedGpsCords;}
        private Bitmap getBitmap (){return savedBitmap;}
        private static Bitmap savedBitmap;
        private static ArrayList<Float> savedGpsCords;
    }
    public static final int ID = R.layout.fragment_pdfmap;
    private TextView mDistanceTextView;
    private volatile ArrayList<Float> mGpsCords;
    private Bundle savedState = null;
    private boolean createdStateInDestroyView;
    private static final String SAVED_BUNDLE_TAG = "saved_bundle";
    private LocationEventListener mLocationEventListener;
    private static final String t = "PDFMapFragment";
    private Location mLocation;
    private View mView;
    private CompassView mCompass;
    private CompassView mCensusLocation;
    private PdfView mPdfView;
    private String mAppName;
    private List<CensusModel> mCensus;
    private DirectionProvider mDirectionProvider;
    public static ArrayList<CensusUtil.FilterCensusList> mFilterCensusList = new ArrayList<CensusUtil.FilterCensusList>();
    public static CensusUtil.Sort mSort = CensusUtil.Sort.POINT_TYPES;
    // public static final String SORT = "sort";
    public static final String CURRENT_LATITUDE = "current_latitude";
    public static final String CURRENT_LONGITUDE = "current_longitude";
    private static final String DIALOG_MSG = "dialogmsg";
    private static final int CENSUS_LIST_LOADER = 0x24;
    private NavigateFragment.CensusListAdapter mInstances;
    private SwipeRefreshLayout mRefreshLayout;
    Bitmap currentBitmap;
    Bitmap OriginalBitmap;
    NetworkInfo activeNetwork;
    boolean isNetworkConnectionOn;
    boolean isWifiEnabled;
    private static myObject tempObject;
    boolean isRouteDefined = false;
    boolean isMapReindered = false;
    static boolean isSaved = false;
    CensusModel mCurrentSelectedCencus;
    private ConnectivityManager mConnectionManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppName = getActivity().getIntent().getStringExtra(
                MainMenuActivity.APP_NAME);
        getActivity().setTitle(R.string.navigate);
        if (mAppName == null || mAppName.length() == 0) {
            mAppName = "PdfMap";
        }
        mGpsCords  = new ArrayList<Float>();
        mConnectionManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetworkConnectionOn = isNetworkConnected();
        boolean isWifiEnabled = isWifiActive();

        setHasOptionsMenu(true);

        if (mFilterCensusList.size() == 0) {
            mFilterCensusList.add(CensusUtil.FilterCensusList.MainPoint);
            mFilterCensusList.add(CensusUtil.FilterCensusList.AdditionalPoint);
            mFilterCensusList.add(CensusUtil.FilterCensusList.AlternatePoint);
        }
        if (isSaved && tempObject != null){
            OriginalBitmap = tempObject.getBitmap();
            currentBitmap = OriginalBitmap.copy(OriginalBitmap.getConfig(),true);
            mGpsCords = tempObject.getGpsList();
        }
        else{
            //TODO: Make this a string to convert to any language
            Toast.makeText(getActivity(), "LOAD PDF FILE", Toast.LENGTH_LONG).show();
        }

        WebLogger.getLogger(mAppName).i(t, t + ".onCreate appName=" + mAppName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        mView = inflater.inflate(R.layout.fragment_pdfmap, container, false);
        mPdfView = (PdfView) mView.findViewById(R.id.pdfMapView);
        mInstances = new NavigateFragment.CensusListAdapter(getActivity());
        mRefreshLayout = (SwipeRefreshLayout) mView
                .findViewById(R.id.swipe_container);
        mCompass = (CompassView) mView.findViewById(R.id.compass);
        mCensusLocation = (CompassView) mView.findViewById(R.id.censusLocation);
        mDirectionProvider = new DirectionProvider(getActivity());
        mDistanceTextView = (TextView) mView
                .findViewById(R.id.distanceTextView);
        mDistanceTextView = (TextView) mView
                .findViewById(R.id.distanceTextView);
        mDirectionProvider.setDirectionEventListener(this);
        mDirectionProvider.setLocationEventListener(this);
        mDirectionProvider.start();
        mCompass.invalidate();
        mCensusLocation.invalidate();
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(CENSUS_LIST_LOADER, null, this);
        mInstances = new NavigateFragment.CensusListAdapter(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pdf_map_menu, menu);
        return;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.pdf_loader:
                // Method to load the PDF
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "Download"), 0);

                } catch (android.content.ActivityNotFoundException ex) {
                    ex.printStackTrace();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0: {
                if (data == null) {
                    return;
                }
                    Uri uri = data.getData();
                    File extStore = Environment.getExternalStorageDirectory();
                    String uriString = uri.toString();
                    String fileName = null;
                    String filePath = null;
                    String fileExt = null;


                    if (uriString.startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                fileExt = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
                                // Check for proper pdf file extension
                                if (fileExt.compareTo("pdf") != 0) {
                                    return;
                                }
                                // Create the total file path
                                filePath = extStore.toString().concat("/Maps/").concat(fileName);
                            }
                        } finally {
                            cursor.close();
                        }
                    }

                    try {
                        OriginalBitmap = currentBitmap = null;
                        mGpsCords.clear();
                        OriginalBitmap = ParsePDF(filePath, mGpsCords);
                        currentBitmap = OriginalBitmap.copy(OriginalBitmap.getConfig(),true);
                        tempObject = null;

                    } catch (IOException e) {
                        Toast.makeText(getActivity(), "Could not find the file within the \"Maps\" directory.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    if (currentBitmap != null) {
                        DrawBitmapAndCensusPoints(currentBitmap,mCensus,mGpsCords);
                        isSaved = true;
                    }
                    else {}

                }
                break;
            }
        }

    private void DrawBitmapAndCensusPoints(Bitmap bm , List<CensusModel> census , ArrayList<Float> gps) {
        updateLocationCompass();
        mPdfView.cleanCanvas();
        mPdfView.setImageBitmap(bm);
        mPdfView.setGPSBoundsCords(gps);
        mPdfView.setCensusData(census);
        mPdfView.setAdjustViewBounds(true);
        mPdfView.setFocusableInTouchMode(true);
        mPdfView.setFocusable(true);
        mPdfView.invalidate();
        isMapReindered = true;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCensus != null){mCensus = null;}
        if (mConnectionManager != null){mConnectionManager = null;}
        if (mPdfView != null){mPdfView = null;}
        if (currentBitmap != null){currentBitmap=null;}
        if (mCurrentSelectedCencus != null) {mCurrentSelectedCencus = null;}
    }

    @Override
    public void onDestroyView() {
        NavigateFragment.mPosition = -1;
        mDirectionProvider.stop();
        mDirectionProvider.unregisterSensorsListener();
        if (OriginalBitmap != null && mGpsCords.size() > 0) {
            tempObject = new myObject(mGpsCords,OriginalBitmap);
            isSaved = true;
        }
        if (mDirectionProvider != null){mDirectionProvider = null;}
        if (mCurrentSelectedCencus != null){mCurrentSelectedCencus.setIsSelected(false);}
        isMapReindered = false;
        mPdfView.cleanCanvas();
        super.onDestroyView();

    }

    @Override
    public Loader<List<CensusModel>> onCreateLoader(int id, Bundle args) {
        return new CensusListLoader(getActivity(),
                ((ODKActivity) getActivity()).getAppName(), args);

    }

    @Override
    public void onLoadFinished(Loader<List<CensusModel>> loader, List<CensusModel> data) {
        mCensus = data;
        if (isSaved && tempObject != null) {
            Bitmap temp = tempObject.getBitmap();
            currentBitmap = temp.copy(temp.getConfig(),true);
            if (currentBitmap != null) {
                DrawBitmapAndCensusPoints(currentBitmap, mCensus, tempObject.getGpsList());
                tempObject = null;
            }
        }


    }
    private void updateDistance(Location location) {
        if (mDirectionProvider.getDestinationLocation() != null) {
            double distance = DistanceUtil.getDistance(mDirectionProvider
                    .getDestinationLocation().getLatitude(), mDirectionProvider
                    .getDestinationLocation().getLongitude(), location
                    .getLatitude(), location.getLongitude());
                    mDistanceTextView.setText(getActivity().getString(
                    R.string.distance,
                    DistanceUtil.getFormatedDistance(distance)));
        }
    }
    private void updateLocationCompass() {
        int position = NavigateFragment.mPosition;
        if (position >= 0) {
            mDirectionProvider.stop();
            mCurrentSelectedCencus = mCensus.get(position);
            mCurrentSelectedCencus.setIsSelected(true);
            String instanceId = mCurrentSelectedCencus.getInstanceId();
            Location censusLocation = new Location(instanceId);
            censusLocation.setAccuracy((float) mCurrentSelectedCencus.getAccuracy());
            censusLocation.setAltitude(mCurrentSelectedCencus.getAltitude());
            censusLocation.setLatitude(mCurrentSelectedCencus.getLatitude());
            censusLocation.setLongitude(mCurrentSelectedCencus.getLongitude());
            mDirectionProvider.setDestinationLocation(censusLocation);
            mDirectionProvider.setDirectionEventListener(this);
            mDirectionProvider.setLocationEventListener(this);
            mDirectionProvider.start();
            mDistanceTextView.setText(getActivity().getString(R.string.distance,
                    "-"));
            updateDistance(mDirectionProvider.getCurrentLocation());

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isMapReindered && mDirectionProvider != null) {
            if (location != null) {
                mLocation = location;
                mPdfView.setMyCurrentLocation(location.getLatitude(), location.getLongitude());
                updateDistance(mLocation);
                mPdfView.invalidate();

            }

        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (isMapReindered) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    if (mDirectionProvider.getCurrentLocation() != null) {
                        Location mLocation = mDirectionProvider.getCurrentLocation();
                    }
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    break;
            }
        }
    }

    @Override
    public void onHeadingToNorthChanged(float heading) {
        if (isMapReindered) {
            if (isAdded()) {
                mCompass.setDegrees(heading);
            }
        }
    }

    @Override
    public void onBearingToCensusLocationChanged(float bearing, float heading) {
        if (isMapReindered) {
            if (isAdded()) {
                mCensusLocation.setVisibility(View.VISIBLE);
                float rotation = 360 - bearing + heading;
                mCensusLocation.setDegrees(rotation);
            }
        }
    }
    @Override
    public void onLoaderReset(Loader<List<CensusModel>> loader) {
        mInstances.clear();
    }

    @Override
    public void onNetworkActive() {

    }

    private boolean isNetworkConnected() {
        activeNetwork =  mConnectionManager.getActiveNetworkInfo();
        isNetworkConnectionOn = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isNetworkConnectionOn;
    }

    private boolean isWifiActive(){
        activeNetwork =  mConnectionManager.getActiveNetworkInfo();
        boolean isWifiEnabled = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        return isWifiEnabled;
    }
    private static class CensusListAdapter extends ArrayAdapter<CensusModel> {
        private final LayoutInflater mInflater;
        private Context mContext;

        public CensusListAdapter(Context context) {
            super(context, R.layout.census_list_item);
            mContext = context;
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * Populate items in the list.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder viewHolder;

            view = convertView;

            if (view == null) {
                view = mInflater.inflate(R.layout.census_list_item, parent,
                        false);
                viewHolder = new ViewHolder();
                viewHolder.headNameTextView = (TextView) view
                        .findViewById(R.id.headNameTextView);
                viewHolder.houseNoTextView = (TextView) view
                        .findViewById(R.id.houseNoTextView);
                viewHolder.commentTextView = (TextView) view
                        .findViewById(R.id.commentTextView);
                viewHolder.placeNameTextView = (TextView) view
                        .findViewById(R.id.placeNameTextView);
                viewHolder.distanceTextView = (TextView) view
                        .findViewById(R.id.distanceTextView);
                viewHolder.censusImageView = (ImageView) view
                        .findViewById(R.id.censusImageView);
                viewHolder.censusInfoContainer = (RelativeLayout) view
                        .findViewById(R.id.censusInfoContainer);
                viewHolder.censusInfoBaseContainer = (LinearLayout) view
                        .findViewById(R.id.censusInfoBaseContainer);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            CensusModel census = getItem(position);

            if (census.getHeadName() != null) {
                viewHolder.headNameTextView.setText((census.getHeadName()
                        .length() > 70 ? census.getHeadName().substring(0, 70)
                        + " ..." : census.getHeadName()));
            } else {
                viewHolder.headNameTextView.setText("-");
            }

            viewHolder.houseNoTextView.setText(mContext.getString(
                    R.string.house_number, census.getHouseNumber()));

            int commentLength = census.getComment() != null ? census
                    .getComment().length() : 0;

            if (commentLength == 0) {
                viewHolder.commentTextView.setVisibility(View.GONE);
            } else {
                viewHolder.commentTextView.setVisibility(View.VISIBLE);
                viewHolder.commentTextView.setText((census.getComment()
                        .length() > 70 ? census.getComment().substring(0, 70)
                        + " ..." : census.getComment()));
            }

            ArrayList<PlaceModel> placeNames = PlaceNameUtil
                    .getHierarchiesDetail(mContext, census.getPlaceName(),
                            PlaceNameUtil.HirarchyOrder.LowerToHigher);
            String placeName = "";
            if (placeNames != null && placeNames.size() > 1) {
                placeName = ((PlaceModel) placeNames.get(1)).getName() + ", "
                        + ((PlaceModel) placeNames.get(0)).getName();
            } else {
                placeName = census.getPlaceName();
            }

            viewHolder.placeNameTextView.setText(mContext.getString(
                    R.string.place_name_hierarchy, placeName));

            String distance = census.getDistance() == -1 ? "-" : String
                    .valueOf(DistanceUtil.getFormatedDistance(census
                            .getDistance()));
            viewHolder.distanceTextView.setText(mContext.getString(
                    R.string.distance, distance));


            return view;
        }
    }
    static class ViewHolder {
        TextView headNameTextView;
        TextView houseNoTextView;
        TextView commentTextView;
        TextView placeNameTextView;
        TextView distanceTextView;
        ImageView censusImageView;
        RelativeLayout censusInfoContainer;
        LinearLayout censusInfoBaseContainer;
    }
    private void sortCensus(CensusUtil.Sort sort) {
        mSort = sort;
        Bundle bundle = new Bundle();
        // bundle.putInt(SORT, mSort.ordinal());

        if (mDirectionProvider.getCurrentLocation() != null) {
            bundle.putDouble(CURRENT_LATITUDE, mDirectionProvider
                    .getCurrentLocation().getLatitude());
            bundle.putDouble(CURRENT_LONGITUDE, mDirectionProvider
                    .getCurrentLocation().getLongitude());
        }
        getLoaderManager().restartLoader(CENSUS_LIST_LOADER, bundle, this);
    }

}
