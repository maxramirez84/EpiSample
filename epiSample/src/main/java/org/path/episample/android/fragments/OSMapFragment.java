package org.path.episample.android.fragments;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager.OnNetworkActiveListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.path.common.android.data.CensusModel;
import org.path.common.android.data.PlaceModel;
import org.path.common.android.utilities.PlaceNameUtil;
import org.path.episample.android.R;
import org.path.episample.android.activities.ODKActivity;
import org.path.episample.android.provider.DirectionProvider;
import org.path.episample.android.provider.DirectionProvider.DirectionEventListener;
import org.path.episample.android.provider.DirectionProvider.LocationEventListener;
import org.path.episample.android.tasks.CensusListLoader;
import org.path.episample.android.utilities.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

import static org.path.episample.android.R.string.pdf_load_string;


public class OSMapFragment extends Fragment implements DirectionEventListener,OnNetworkActiveListener, LocationEventListener , LoaderManager.LoaderCallbacks<List<CensusModel>>{
    private String CKey = "FQ5hUk0oTuXUVCBS3kkTrFJnPTurQ09T4e3zrWlw";
    public static final int ID = R.layout.fragment_osmap;
    private static final int CENSUS_MAP_LOADER = 0x93;
    private List<CensusModel> mCensus;
    protected MapView mMapView;
    private CensusAdapter mInstances;
    private CompassOverlay mCompassOverlay;
    private ItemizedIconOverlay<OverlayItem> mMyLocationOverlay;
    RadiusMarkerClusterer mMarkers;
    private Polyline roadOverlay;
    private ArrayList<OverlayItem> mItems = new ArrayList<OverlayItem>();
    private MyLocationNewOverlay myCurrentLocationOverlay;
    private List<Drawable> icons = new ArrayList<>(4);
    private GeoPoint mDestinationPoint;
    private LocationListener mLocationListener;
    private boolean mIsGPSOn = false;
    private boolean mIsNetworkOn = false;
    private  LocationManager mLocationManager;
    private DirectionProvider mDirectionProvider;
    private int mTimer = 0;
    private ConnectivityManager mConnectionManager;
    NetworkInfo activeNetwork;
    boolean isNetworkConnectionOn;
    boolean isWifiEnabled;
    boolean isRouteDefined = false;
    /**
     * Current GPS/WiFi location
     */
    private Location mLocation;

    /**
     * the location event listener
     */
    private LocationEventListener mLocationEventListener;

    @Override
    public void onHeadingToNorthChanged(float heading) {

    }

    @Override
    public void onBearingToCensusLocationChanged(float bearing, float heading) {

    }

    @Override
    public void onNetworkActive() {
     isNetworkConnected();
     isWifiActive();
    }

    public static interface LocationEventListener {
        void onLocationChanged(Location location);
        void onProviderDisabled(String provider);
        void onProviderEnabled(String provider);
        void onStatusChanged(String provider, int status, Bundle extras);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDirectionProvider = new DirectionProvider(this.getActivity());
        mDirectionProvider.setDirectionEventListener(this);
        mDirectionProvider.setLocationEventListener(this);
        mDirectionProvider.start();
        mConnectionManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isNetworkConnectionOn = isNetworkConnected();
        boolean isWifiEnabled = isWifiActive();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mMapView = new MapView(inflater.getContext());
        org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.setUserAgentValue(CKey);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setClickable(true);
        return mMapView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDirectionProvider.start();
        getLoaderManager().initLoader(CENSUS_MAP_LOADER, null, this);
        mInstances = new CensusAdapter(getActivity());
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setTilesScaledToDpi(true);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView.onDetach();
        mDestinationPoint = null;
        mConnectionManager = null;
        mMapView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Loader<List<CensusModel>> onCreateLoader(int id, Bundle args) {
        return new CensusListLoader(getActivity(),
                ((ODKActivity) getActivity()).getAppName(), args);

    }

    @Override
    public void onLoadFinished(Loader<List<CensusModel>> loader, List<CensusModel> data) {

        Drawable clusterIconD = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_cluster, null);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();

        Drawable markerIconDMDA = ResourcesCompat.getDrawable(getResources(), R.drawable.home, null);
        Bitmap markerIconMDA = ((BitmapDrawable) markerIconDMDA).getBitmap();

        Drawable markerIconH = ResourcesCompat.getDrawable(getResources(), R.drawable.red_cross, null);
        Bitmap markerIconHD = ((BitmapDrawable) markerIconDMDA).getBitmap();

        Drawable markerIconW = ResourcesCompat.getDrawable(getResources(), R.drawable.waterdrop, null);
        Bitmap markerIconWD = ((BitmapDrawable) markerIconDMDA).getBitmap();

        mCensus = data;
        getLoaderManager().destroyLoader(CENSUS_MAP_LOADER);
        mMapView.getOverlays().clear();


        SetCurrentLocationOverlay();

        SetCompassOverlay();

        mMarkers = new RadiusMarkerClusterer(getActivity());
        mMarkers.setIcon(clusterIcon);
        mMarkers.getTextPaint().setTextSize(12 * getResources().getDisplayMetrics().density);
        mMarkers.mAnchorV = Marker.ANCHOR_BOTTOM;
        mMarkers.mTextAnchorU = 0.70f;
        mMarkers.mTextAnchorV = 0.27f;
        mMapView.getOverlays().add(mMarkers);


        for (CensusModel c : mCensus){
            Marker mMarker = new Marker(mMapView);
            OverlayItem item = new OverlayItem(c.getHeadName(),c.getHouseNumber(),new GeoPoint(c.getLatitude(),c.getLongitude()));
            if (c.getHeadName().toLowerCase().contains(getString(R.string.hospital)) || c.getComment().toLowerCase().contains(getString(R.string.hospital))){
                mMarker.setIcon(markerIconH);
            }
            else if (c.getHeadName().toLowerCase().contains(getString(R.string.water_source)) || c.getComment().toLowerCase().contains(getString(R.string.water_source))) {
                mMarker.setIcon(markerIconW);
            }
            else {
                mMarker.setIcon(markerIconDMDA);
            }
            mMarker.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mMapView));
            mMarker.setPosition(new GeoPoint(c.getLatitude(),c.getLongitude()));
            mMarker.setRelatedObject(c);
            mMarker.setEnabled(true);
            mMarker.setTitle(c.getHeadName());
            mMarkers.add(mMarker);
            item.setMarker(markerIconDMDA);
            mItems.add(item);
        }

        mMapView.getOverlays().add(mMarkers);

        IMapController mMapViewController = mMapView.getController();
        mMapViewController.setZoom(16);

        if (myCurrentLocationOverlay.getMyLocation() != null)
        {
            mMapViewController.setCenter(myCurrentLocationOverlay.getMyLocation());
        }
        else
        {
            mMapViewController.setCenter(new GeoPoint(34.7457,-86.6727));
        }

        OverlayMyLocation();

        mMapView.invalidate();

    }

    private void OverlayMyLocation() {
        OverlayLocation();
        mMyLocationOverlay.setEnabled(false);
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private void SetCurrentLocationOverlay() {
        myCurrentLocationOverlay = new MyLocationNewOverlay(mMapView);
        myCurrentLocationOverlay.enableMyLocation();
        myCurrentLocationOverlay.enableFollowLocation();
        mMapView.getOverlays().add(myCurrentLocationOverlay);
    }

    private void SetCompassOverlay() {
        mCompassOverlay = new CompassOverlay(getActivity(), mMapView);
        mCompassOverlay.setEnabled(true);
        mCompassOverlay.enableCompass();

        mMapView.getOverlays().add(mCompassOverlay);
    }

    private void OverlayLocation() {
        mMyLocationOverlay = new ItemizedIconOverlay<OverlayItem> (mItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>()
                {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item)
                    {
                        if (myCurrentLocationOverlay.getMyLocation() == null){return false;}
                        ClearPolylineFromOverlay();
                        mDestinationPoint = null;
                        isRouteDefined = false;
                        return true; // We 'handled' this event.
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item)
                    {

                        ClearPolylineFromOverlay();
                        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
                        GeoPoint currentLocationPoint = myCurrentLocationOverlay.getMyLocation();
                        if (myCurrentLocationOverlay == null){return false;}
                        mDestinationPoint = (GeoPoint) item.getPoint();
                        waypoints.add(currentLocationPoint);
                        waypoints.add(mDestinationPoint);
                        new UpdateRoadTask().execute(waypoints);
                        return true;
                    }

                }, getActivity());
    }

    @Override
    public void onLoaderReset(Loader<List<CensusModel>> loader) {
        mMapView.invalidate();

    }

    private void ClearPolylineFromOverlay() {
        List<Overlay> tempOverlayList = mMapView.getOverlays();
        for (Overlay l : tempOverlayList){
            if (l.isEnabled() && (l instanceof Polyline || l instanceof Marker)){
                mMapView.getOverlays().remove(l);
            }

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // set the new location
        if(mDestinationPoint == null) {return;}
        mLocation = location;
        mTimer++;
        GeoPoint currentLocationPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (mLocation.getAccuracy() < 20) {
            ClearPolylineFromOverlay();
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
            waypoints.add(currentLocationPoint);
            waypoints.add(mDestinationPoint);
            new UpdateRoadTask().execute(waypoints);
            if (mLocationEventListener != null) {
                mLocationEventListener.onLocationChanged(location);
            }
        }


    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
         if (mLocationEventListener != null) {
            mLocationEventListener.onStatusChanged(s, i, bundle);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            mIsGPSOn = true;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
            mIsNetworkOn = true;
        }

        if (mLocationEventListener != null) {
            mLocationEventListener.onProviderEnabled(provider);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            mIsGPSOn = false;
        } else if (LocationManager.NETWORK_PROVIDER.equals(provider)) {
            mIsNetworkOn = false;
        }

        if (mLocationEventListener != null) {
            mLocationEventListener.onProviderDisabled(provider);
        }
    }

    private static class CensusAdapter extends ArrayAdapter<CensusModel> {
        private final LayoutInflater mInflater;
        private Context mContext;

        public CensusAdapter(Context context) {
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

    private class UpdateRoadTask extends AsyncTask<Object,Void,Road[]>{

        private ArrayList<GeoPoint> waypoints;
        private RoadManager roadManager;
        Polyline[] mRoadOverlays;
        Polyline roadPolyline;
        Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_default);
        @Override
        protected void onPostExecute(Road[] roads) {
           if (roads[0]== null && isRouteDefined)
           {
               return ;
           }
           else if (roads[0] == null || !(isNetworkConnected())){
                CreateOfflinePolyline();
               isRouteDefined = false;
            }
            else {
                CreateOnlinePolyline(roads);
                isRouteDefined = true;
            }

        }

        @Override
        protected Road[] doInBackground(Object... params) {
            if (isNetworkConnected() || isWifiActive()) {
                if (!isRouteDefined) {
                    waypoints = (ArrayList<GeoPoint>) params[0];
                    roadManager = new MapQuestRoadManager("GpMkLtfPIuXTwXDmILp1WpbAbHeaLl4k");
                    roadManager.addRequestOption("routeType=pedestrian");
                    return roadManager.getRoads(waypoints);
                }
                else {return new Road[]{null};}
            }
            else {
                waypoints = (ArrayList<GeoPoint>) params[0];
                roadManager = new RoadManager() {
                    @Override
                    public Road getRoad(ArrayList<GeoPoint> arrayList) {
                        return null;
                    }

                    @Override
                    public Road[] getRoads(ArrayList<GeoPoint> arrayList) {
                        return new Road[]{null};
                    }
                };
                return roadManager.getRoads(waypoints);
            }

        }

        private void CreateOnlinePolyline(Road[] roads) {
            mRoadOverlays = new Polyline[roads.length];

            for (int i = 0; i < roads.length; i++) {
                roadPolyline = roadManager.buildRoadOverlay(roads[i], Color.BLUE, 20.0f);
                mRoadOverlays[i] = roadPolyline;
                String routeDesc = roads[i].getLengthDurationText(getActivity(), -1);
                roadPolyline.setTitle(getString(R.string.app_name) + " - " + routeDesc);
                roadPolyline.setInfoWindow(new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mMapView));
                roadPolyline.setRelatedObject(i);
                roadPolyline.setVisible(true);
                mMapView.getOverlays().add(roadPolyline);
                mMapView.invalidate();
                for (int j = 0; j < roads[i].mNodes.size(); j++) {
                    RoadNode node = roads[i].mNodes.get(j);
                    Marker nodeMarker = new Marker(mMapView);
                    nodeMarker.setPosition(node.mLocation);
                    nodeMarker.setSnippet(node.mInstructions);
                    nodeMarker.setIcon(nodeIcon);
                    nodeMarker.setTitle("Step " + j);
                    mMapView.getOverlays().add(nodeMarker);
                    mMapView.invalidate();
                }

            }
        }

        private void CreateOfflinePolyline() {
            roadPolyline = new Polyline();
            roadPolyline.setVisible(true);
            roadPolyline.setColor(Color.BLUE);
            roadPolyline.setWidth(20.0f);
            if (waypoints == null) {return;}
            roadPolyline.setPoints(waypoints);
            mMapView.getOverlays().add(roadPolyline);
            mMapView.invalidate();
            int counter = 0;
            for (GeoPoint w : waypoints) {
                Marker nodeMarker = new Marker(mMapView);
                nodeMarker.setPosition(new GeoPoint(w.getLatitude(),w.getLongitude()));
                nodeMarker.setSnippet("EndPoints");
                nodeMarker.setTitle("Step " + counter);
                nodeMarker.setIcon(nodeIcon);
                mMapView.getOverlays().add(nodeMarker);
                counter++;

            }
            counter = 0;
        }

    }

}