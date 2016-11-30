package com.thetacab.hp.cargar.Activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.hujiaweibujidao.wava.Techniques;
import com.github.hujiaweibujidao.wava.YoYo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thetacab.hp.cargar.Animations;
import com.thetacab.hp.cargar.Constants;
import com.thetacab.hp.cargar.FairCalculation;
import com.thetacab.hp.cargar.GoogleDirectionsApiWrapper;
import com.thetacab.hp.cargar.GoogleReverseGeocodingApiWrapper;
import com.thetacab.hp.cargar.Order;
import com.thetacab.hp.cargar.R;
import com.thetacab.hp.cargar.SystemBarTintManager;
import com.thetacab.hp.cargar.Utils;
import com.wang.avi.AVLoadingIndicatorView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MapsFragment extends Fragment implements
        OnMapReadyCallback, LocationListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "MapsFragment";
    private boolean animateCurrLocationButton;

    private GoogleMap mMap;
    Context context;
    MapsFragment fragment;

    GoogleDirectionsApiWrapper googleDirectionsApiWrapper;
    GoogleApiClient mGoogleApiClient;
    android.location.Location mLastLocation;

    //Fields
    int noOfAnimationsRunning;
    boolean movingUpAnimation;
    boolean movingDownAnimation;
    boolean sourceEntered;
    boolean destinationEntered;
    boolean canTapSelectionIcon;
    boolean driverIsArriving;
    boolean sourceLatLngRetrieved;
    boolean inTrip;
    private LatLng sourceLatLng;
    private LatLng destinationLatLng;
    private Marker sourceMarker;
    private Marker destinationMarker;
    public LatLng centerOfMapLatLng;
    private String DIRECTION_API_KEY;
    private String sourceAddress;
    private String destinationAddress;
    public FairCalculation fairCalculation;
    boolean activateFairEstimation;
    private int currentCabSelection;

    /////////////////////////////
    View rootView;

    //Views
    @InjectView(R.id.notify_selection_toast_text_view)
    TextView notifySelectionToastTV;
    ImageButton currLocButton;
    @InjectView(R.id.bike_selection_image_button)
    ImageButton bikeSelectionIB;
    @InjectView(R.id.cab_selection_card_view)
    CardView cabSelectionCV;
    @InjectView(R.id.cab_type_selection_layout)
    LinearLayout cabSelectionLayout;
    CardView markerButton;
    CardView searchSouceCardView;
    CardView searchDestinationCardView;
    @InjectView(R.id.source_destination_selection_layout)
    CardView sourceDestinationSelectionLayoutCV;
    TextView markerButtonTextView;
    ImageView markerAtCenterOfMapIV;
    @InjectView(R.id.source_bar_cross_image_button)
    ImageButton sourceBarCrossIB;
    ImageButton destinationBarCrossIB;
    Button requestCabButton;
    TextView fairQuoteTV;
    CardView tripCodeCV;
    TextView tripCodeTV;
    ImageView driverProfilePicIV;
    ProgressBar mSourceProgressBar;
    ProgressBar mDestinationProgressBar;
    @InjectView(R.id.blurView)
    FrameLayout blurView;
    @InjectView(R.id.finding_taxi_text_view)
    TextView findingTaxiTV;
    @InjectView(R.id.finding_taxi_animation_view)
    AVLoadingIndicatorView findCabAnimView;
    @InjectView(R.id.cancel_trip_button)
    Button cancelTripButton;
    @InjectView(R.id.cab_arrived_animation)
    AVLoadingIndicatorView cabHasArrivedAnimView;

    PlaceAutocompleteFragment sourceAddressAutocCompleteFragment;
    PlaceAutocompleteFragment destinationAddressAutoCompleteFragment;

    private OnFragmentInteractionListener mListener;

    public MapsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_maps, container, false);
        ButterKnife.inject(this, rootView);
        fragment = this;
        animateCurrLocationButton = true;
        setupAllViews();
        initializeFields();
        setupGooglePlacesAPI();
        googleDirectionsApiWrapper.setFairEstimateTV(fairQuoteTV);
        setStatusBarTranslucent(rootView);
        getLocationPermissions();
        markerButton.setCardBackgroundColor(Color.parseColor("#ddF9BA32"));
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    void setupGooglePlacesAPI() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(getActivity())
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(Utils.getCurrUser() == null){return;}

        FirebaseDatabase.getInstance().getReference()
                .child("AppStatus")
                .child(Utils.getUid())
                .setValue(1);
    }


    void getLocationPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void setStatusBarTranslucent(View rootView) {
        View v = rootView.findViewById(R.id.map);
        View v1 = rootView.findViewById(R.id.source_destination_selection_layout);
        if (v != null && v1 != null) {
            int paddingTop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? Utils.getStatusBarHeight(context) : 0;
            RelativeLayout.LayoutParams mapLayoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
            RelativeLayout.LayoutParams searchCardLayoutParams = (RelativeLayout.LayoutParams) v1.getLayoutParams();
            mapLayoutParams.topMargin -= paddingTop;
            searchCardLayoutParams.topMargin += paddingTop;
        }

        SystemBarTintManager tintManager = new SystemBarTintManager(getActivity());
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setStatusBarAlpha(0.2f);
        tintManager.setNavigationBarAlpha(0.2f);
        tintManager.setTintAlpha(0.2f);
        tintManager.setStatusBarTintResource(R.drawable.selected);
        tintManager.setTintColor(Color.parseColor("#007DC0"));
    }



    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            mLastLocation = location;
            if(Utils.getCurrUser()==null){
                return;
            }

            FirebaseDatabase.getInstance().getReference().child("PassengerLocation").
                    child(Utils.getUid()).child("lat").setValue(location.getLatitude());
            FirebaseDatabase.getInstance().getReference().child("PassengerLocation").
                    child(Utils.getUid()).child("lng").setValue(location.getLongitude());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.v(TAG, " code callback: "+requestCode);
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    mGoogleApiClient.disconnect();
                    mGoogleApiClient.connect();
                }
            }
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        /**
         * He we will get the user's current
         * location and setup the map camera
         * to that location
         */
        //startLocationUpdates();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getLocationPermissions();
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            LatLng currentLoc = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder().
                            target(currentLoc).
                            zoom((float) 15.5).
                            tilt((float) 70).
                            build()
            ));
        }else{
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            Log.e("Location: ", " Location was null");
        }
    }

    void initializeFields(){
        DIRECTION_API_KEY = getResources().getString(R.string.google_directions_api_key);
        googleDirectionsApiWrapper = new GoogleDirectionsApiWrapper(DIRECTION_API_KEY,fragment);
        fairCalculation = new FairCalculation(0);
        googleDirectionsApiWrapper.setCabType(0);
        googleDirectionsApiWrapper.changeEstimateWhenCabTypeChanged();
        canTapSelectionIcon = true;
        currentCabSelection = Constants.SELECT_BIKE;
        noOfAnimationsRunning = 0;
        movingUpAnimation = false;
        movingDownAnimation = false;
        sourceEntered = false;
        destinationEntered = false;
        driverIsArriving = false;
        inTrip = false;
        activateFairEstimation = false;
        sourceLatLngRetrieved = false;
    }

    /**
     * Called when a fragment is first attached to its activity.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param activity
     * @deprecated See {@link #onAttach(Context)}.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context=activity;
    }

    public void showDistanceNotSupportedDialog(){
        new MaterialDialog.Builder(getActivity()).title("Trip Too Long")
                .content("We currently don't support journeys more than 20km out side NUST H-12 premises.")
                .titleColorRes(R.color.primary_dark)
                .backgroundColor(Color.WHITE)
                .positiveColorRes(R.color.primary)
                .positiveText("OK")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        makeDestinationBarEmpty();
                    }
                })
                .show();
    }

    void setupAllViews() {
        linkViews();

        setAllOnClickListeners();

        setOnPlaceSelectedListenerOnSourceBar();
        setOnPlaceSelectedListnerOnDestinationBar();

        //Set the hints in the source and destination bars.
        sourceAddressAutocCompleteFragment.setHint("Enter Source");
        destinationAddressAutoCompleteFragment.setHint("Enter Destination");

        //Set Max Card Elevation for source and destination Address Cards
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //ex. if ics is met then do this
            searchDestinationCardView.setMaxCardElevation(Utils.fromDpToPx(20f));
            searchSouceCardView.setMaxCardElevation(Utils.fromDpToPx(20f));
            cabSelectionCV.setMaxCardElevation(Utils.fromDpToPx(30f));
            cabSelectionCV.setCardElevation(Utils.fromDpToPx(10f));
        }
    }

    /**
     * Link Java Fields to XML
     */
    void linkViews() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            sourceAddressAutocCompleteFragment = (PlaceAutocompleteFragment)
                    getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_source);
            destinationAddressAutoCompleteFragment = (PlaceAutocompleteFragment)
                    getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_destination);
        }
        markerButton = (CardView) rootView.findViewById(R.id.button_on_top_of_marker);
        currLocButton = (ImageButton) rootView.findViewById(R.id.current_location_button);
        searchSouceCardView = (CardView) rootView.findViewById(R.id.search_source_card_view);
        searchDestinationCardView = (CardView) rootView.findViewById(R.id.search_destination_card_view);
        markerAtCenterOfMapIV = (ImageView) rootView.findViewById(R.id.marker_at_center_of_map_image_view);
        markerAtCenterOfMapIV = (ImageView) rootView.findViewById(R.id.marker_at_center_of_map_image_view);
        destinationBarCrossIB = (ImageButton) rootView.findViewById(R.id.destination_bar_cross_image_button);
        requestCabButton = (Button) rootView.findViewById(R.id.request_cab_button);
        fairQuoteTV = (TextView) rootView.findViewById(R.id.request_cab_price_text_view);
        markerButtonTextView = (TextView) rootView.findViewById(R.id.marker_button_text_view);
        tripCodeCV = (CardView) rootView.findViewById(R.id.trip_code_card_view);
        tripCodeTV = (TextView) rootView.findViewById(R.id.trip_code_text_view);
        driverProfilePicIV = (ImageView) rootView.findViewById(R.id.driver_image_view);
        mSourceProgressBar = (ProgressBar) rootView.findViewById(R.id.source_progress_bar);
        mDestinationProgressBar = (ProgressBar) rootView.findViewById(R.id.destination_progress_bar);
    }

    void setOnPlaceSelectedListenerOnSourceBar(){
        sourceAddressAutocCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                if(isSourceLatLngWithinBounds(place.getLatLng())) {
                    GoogleReverseGeocodingApiWrapper apiWrapper = new GoogleReverseGeocodingApiWrapper(DIRECTION_API_KEY,getActivity());
                    apiWrapper.setBackConnectionListener(new GoogleReverseGeocodingApiWrapper.OnBadConnectionListener() {
                        @Override
                        public void onConnectionFailed() {
                            mSourceProgressBar.setVisibility(View.GONE);
                            mDestinationProgressBar.setVisibility(View.GONE);
                        }
                    });
                    apiWrapper.setLatLng(place.getLatLng()).requestAddress().setOnAddressRetrievedListener(new GoogleReverseGeocodingApiWrapper.OnAddressRetrievedListener() {
                        @Override
                        public void onAddressRetrieved(String resultingAddress) {
                            if (resultingAddress.contains("Islamabad") || resultingAddress.contains("Rawalpindi")) {
                                sourceBarCrossIB.setVisibility(View.VISIBLE);
                                searchSouceCardView.setClickable(false);
                                searchSouceCardView.setFocusable(false);
                                if (destinationEntered && sourceEntered) {
                                    animateCurrLocationButton = false;
                                } else {
                                    sourceEntered = true;
                                }
                                if (!destinationEntered) {
                                    // this is the source location
                                    sourceLatLng = place.getLatLng();

                                    // make the source bar show the complete address of the place
                                    sourceAddressAutocCompleteFragment.setText(place.getAddress());
                                    sourceAddress = place.getAddress().toString();

                                    // animate the camera to the source location
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), (float) 15.5));

                                    // add a marker to the current location indicating that it is the source
                                    if (sourceMarker != null) {
                                        sourceMarker.remove();
                                    }
                                    sourceMarker = mMap.addMarker(new MarkerOptions().
                                            position(place.getLatLng()).
                                            icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_green)).
                                            title("Source").
                                            visible(true)
                                    );

                                    sourceEnteredHighLightDestinationBar();

                                    /**
                                     * as the source have been entered turn the marker
                                     * and button to set destination mode
                                     */
                                    turnMarkerIntoSetDestinationMarker();
                                } else {
                                    // this is the source location
                                    sourceLatLng = place.getLatLng();

                                    // make the source bar show the complete address of the place
                                    sourceAddressAutocCompleteFragment.setText(place.getAddress());
                                    sourceAddress = place.getAddress().toString();

                                    // animate the camera to the source location
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), (float) 15.5));

                                    // add a marker to the current location indicating that it is the source
                                    if (sourceMarker != null) {
                                        sourceMarker.remove();
                                    }
                                    sourceMarker = mMap.addMarker(new MarkerOptions().
                                            position(place.getLatLng()).
                                            icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_green)).
                                            title("Source").
                                            visible(true)
                                    );

                                    // draw path between source and destination as both of them have been entered
                                    if (googleDirectionsApiWrapper.pathPresent()) {
                                        googleDirectionsApiWrapper.removePath();
                                    }
                                    googleDirectionsApiWrapper.from(sourceLatLng).to(destinationLatLng).retreiveDirections().setShow20Warning(true).setTextView(fairQuoteTV).setMap(mMap).drawPathOnMap();

                                    // make sourcebar green and make marker disappear
                                    makeSourceBarGreen();
                                    makeMarkerDisappear();

                                    // show the request cab UI
                                    animateRequestCabView(true);
                                }
                            } else {
                                sourceAddressAutocCompleteFragment.setText(null);
                                Toast.makeText(getActivity(), "Currently we only support rides in the twin cities", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sourceAddressAutocCompleteFragment.setText(null);
                        }
                    },200);
                    Toast.makeText(getActivity(),"Currently, pick up points can only be in premisis of NUST H-12",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    void setOnPlaceSelectedListnerOnDestinationBar(){
        destinationAddressAutoCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(final Place place) {
                GoogleReverseGeocodingApiWrapper googleReverseGeocodingApiWrapper = new GoogleReverseGeocodingApiWrapper(DIRECTION_API_KEY,getActivity());
                googleReverseGeocodingApiWrapper.setBackConnectionListener(new GoogleReverseGeocodingApiWrapper.OnBadConnectionListener() {
                    @Override
                    public void onConnectionFailed() {
                        mSourceProgressBar.setVisibility(View.GONE);
                        mDestinationProgressBar.setVisibility(View.GONE);
                    }
                });
                googleReverseGeocodingApiWrapper.setLatLng(place.getLatLng()).requestAddress().setOnAddressRetrievedListener(new GoogleReverseGeocodingApiWrapper.OnAddressRetrievedListener() {
                    @Override
                    public void onAddressRetrieved(String resultingAddress) {
                        if(resultingAddress.contains("Islamabad")||resultingAddress.contains("Rawalpindi")) {
                            destinationBarCrossIB.setVisibility(View.VISIBLE);
                            if (sourceEntered && destinationEntered) {
                                animateCurrLocationButton = false;
                            } else {
                                destinationEntered = true;
                            }
                            if (!sourceEntered) {
                                // this is the destination location
                                destinationLatLng = place.getLatLng();

                                // make the destination bar show the complete address of the place
                                destinationAddressAutoCompleteFragment.setText(place.getAddress());
                                destinationAddress = place.getAddress().toString();

                                // animate the camera to the destination location
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), (float) 15.5));

                                // add a marker to the current location indicating that it is the destination
                                if (destinationMarker != null) {
                                    destinationMarker.remove();
                                }
                                destinationMarker = mMap.addMarker(new MarkerOptions().
                                        position(place.getLatLng()).
                                        icon(BitmapDescriptorFactory.fromResource(R.mipmap.flag_blue)).
                                        title("Destination").
                                        visible(true)
                                );

                                destinationEnteredHighlightSourceBar();

                                /**
                                 * as the destination has been entered turn the marker
                                 * and button to set source mode
                                 */
                                turnMarkerIntoSetSourceMarker();
                            } else {
                                // this is the destination location
                                destinationLatLng = place.getLatLng();

                                // make the destination bar show the complete address of the place
                                destinationAddressAutoCompleteFragment.setText(place.getAddress());
                                destinationAddress = place.getAddress().toString();

                                // animate the camera to the destination location
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), (float) 15.5));

                                // add a marker to the current location indicating that it is the destination
                                if (destinationMarker != null) {
                                    destinationMarker.remove();
                                }
                                destinationMarker = mMap.addMarker(new MarkerOptions().
                                        position(place.getLatLng()).
                                        icon(BitmapDescriptorFactory.fromResource(R.mipmap.flag_blue)).
                                        title("Destination").
                                        visible(true)
                                );

                                if (googleDirectionsApiWrapper.pathPresent()) {
                                    googleDirectionsApiWrapper.removePath();
                                }
                                // draw path between source and destination as both of them have been entered
                                googleDirectionsApiWrapper.from(sourceLatLng).to(destinationLatLng).retreiveDirections().setShow20Warning(true).setTextView(fairQuoteTV).setMap(mMap).drawPathOnMap();


                                makeDestinationBarBlue();
                                makeMarkerDisappear();

                                // show request cab UI
                                animateRequestCabView(true);
                            }
                        }else{
                            destinationAddressAutoCompleteFragment.setText(null);
                            Toast.makeText(getActivity(),"Currently we only support rides in twin cities",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    void setAllOnClickListeners() {
        setOnClickListenersOnCabTypeSelection();
        setOnClickListnerForMarkerButton();
        setOnCancelListenerForSourceCard();
        setOnCancelListenerForDestinationCard();
        setOnClickListenerOnRequestCabButton();
        setOnClickListenerOnCancelTripButton();
        setOnClickListenerOnCurrLocationButton();
    }

    public void showToast(String s){
        Toast.makeText(getActivity(),s,Toast.LENGTH_SHORT).show();
    }


    void setOnClickListenerOnCancelTripButton(){
        cancelTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancellTrip();
            }
        });
    }

    void setOnClickListenerOnRequestCabButton(){
        requestCabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

                if(Utils.getCurrUser() == null){return;}

                mDatabase.child("Order").child(Utils.getUid()).setValue(new Order(
                        sourceAddress,
                        destinationAddress,
                        String.valueOf(sourceLatLng.latitude),
                        String.valueOf(sourceLatLng.longitude),
                        String.valueOf(destinationLatLng.latitude),
                        String.valueOf(destinationLatLng.longitude),
                        currentCabSelection,null,
                        Utils.extractRequiredKey(System.currentTimeMillis())
                ));

                mDatabase.child("State").child(Utils.getUid()).setValue(Constants.FINDING_CAB_STATE);
                // move the souce and destination selection card view up
                animateSourceDestinatonSelectionLayoutUp();

                // move the request button with it's text view and the cab selection card view down.
                animateCarTypeSelectionAndRequestLayoutDown();

                //show finding cab animation
                showFindingCabScreen();
            }
        });
    }

    void animateSourceDestinatonSelectionLayoutUp(){
        YoYo.with(Techniques.SlideOutUp).duration(1000).playOn(sourceDestinationSelectionLayoutCV);
    }

    void showFindingCabScreen(){

        Animations.makeVisible(findCabAnimView,findingTaxiTV,blurView,cancelTripButton);
        // show loading animation
        Animations.playYoYoAnimOnMultipleViews(Techniques.FadeIn,1000,findCabAnimView,findingTaxiTV,blurView);
        YoYo.with(Techniques.SlideOutRight).duration(1000).playOn(currLocButton);
        YoYo.with(Techniques.SlideInUp).duration(500).playOn(cancelTripButton);
        LatLng sourceLatLng = sourceMarker.getPosition();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(sourceLatLng));
    }


    void animateCarTypeSelectionAndRequestLayoutDown(){
        YoYo.with(Techniques.SlideOutDown).duration(1000).playOn(cabSelectionCV);
        YoYo.with(Techniques.SlideOutDown).duration(1000).playOn(notifySelectionToastTV);
        YoYo.with(Techniques.SlideOutDown).duration(1000).playOn(fairQuoteTV);
        YoYo.with(Techniques.SlideOutDown).duration(1000).playOn(requestCabButton);
    }


    void setOnCancelListenerForDestinationCard(){
        destinationBarCrossIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDestinationBarEmpty();
            }
        });
    }

    void makeDestinationBarEmpty(){
        animateCurrLocationButton = true;
        destinationMarker.setVisible(false);
        destinationBarCrossIB.setVisibility(View.GONE);
        destinationAddressAutoCompleteFragment.setText("");
        destinationAddressAutoCompleteFragment.setHint("Enter Destination");
        destinationEntered = false;
        if(!sourceEntered){
            elevateSourceBar();
            makeDestinationBarBlank();
            turnMarkerIntoSetSourceMarker();
        }else{
            googleDirectionsApiWrapper.removePath();
            sourceEnteredHighLightDestinationBar();
            turnMarkerIntoSetDestinationMarker();
            animateRequestCabView(false);
        }
    }

    void setOnClickListenersOnCabTypeSelection(){
        bikeSelectionIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCabSelection = Constants.SELECT_BIKE;
                googleDirectionsApiWrapper.setCabType(currentCabSelection);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bikeSelectionIB.setImageTintList(ColorStateList.valueOf(Color.parseColor("#007DC0")));
                }
                animateSelectionToast("YOU HAVE SELECTED A BIKE");
            }
        });
    }

    void setOnClickListnerForMarkerButton() {
        markerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerOfMapLatLng = mMap.getCameraPosition().target;
                boolean isInBounds = true;
                if(!sourceEntered){
                    if(!isSourceLatLngWithinBounds(centerOfMapLatLng)){
                        isInBounds = false;
                    }
                }

                if(!sourceEntered){
                    if(isInBounds) {
                        mSourceProgressBar.setVisibility(View.VISIBLE);
                        disableMarkerButton();
                    }
                }else if(!destinationEntered){
                    mDestinationProgressBar.setVisibility(View.VISIBLE);
                    disableMarkerButton();
                }
                if(isInBounds) {
                    GoogleReverseGeocodingApiWrapper googleReverseGeocodingApiWrapper = new GoogleReverseGeocodingApiWrapper(DIRECTION_API_KEY,getActivity());
                    googleReverseGeocodingApiWrapper.setBackConnectionListener(new GoogleReverseGeocodingApiWrapper.OnBadConnectionListener() {
                        @Override
                        public void onConnectionFailed() {
                            mSourceProgressBar.setVisibility(View.GONE);
                            mDestinationProgressBar.setVisibility(View.GONE);
                            enableMarkerButton();
                        }
                    });
                    googleReverseGeocodingApiWrapper.setLatLng(centerOfMapLatLng).
                            requestAddress().setOnAddressRetrievedListener(new GoogleReverseGeocodingApiWrapper.OnAddressRetrievedListener() {
                        @Override
                        public void onAddressRetrieved(String resultingAddress) {
                            mSourceProgressBar.setVisibility(View.GONE);
                            mDestinationProgressBar.setVisibility(View.GONE);
                            enableMarkerButton();
                            if (resultingAddress.contains("Islamabad") || resultingAddress.contains("Rawalpindi")) {
                                if (!sourceEntered && !destinationEntered) {
                                    /**
                                     * make the cross button on the source card visible
                                     * so that the user can cancel the source that they
                                     * have set if they want to enter a different one.
                                     */
                                    sourceBarCrossIB.setVisibility(View.VISIBLE);

                                    /**
                                     * put the current address in the source bar as the
                                     * user has selected this place as thier source
                                     */
                                    sourceAddressAutocCompleteFragment.setText(resultingAddress);
                                    sourceAddress = resultingAddress;

                                    /**
                                     * Adjust the view for entering destination mode
                                     */
                                    turnMarkerIntoSetDestinationMarker();
                                    sourceEnteredHighLightDestinationBar();

                                    // Add a marker a the source indicating that it is the source
                                    sourceMarker = mMap.addMarker(new MarkerOptions()
                                            .position(centerOfMapLatLng)
                                            .title("Source").icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_green)));
                                    sourceMarker.setVisible(true);

                                    // the source has been entered
                                    sourceEntered = true;

                                    // this is the source LatLng
                                    sourceLatLng = centerOfMapLatLng;
                                } else if (sourceEntered && !destinationEntered) {
                                    /**
                                     * make the cross button on the destination bar
                                     * visible so that the user cancel the destination
                                     * if they want to enter a different one.
                                     */
                                    destinationBarCrossIB.setVisibility(View.VISIBLE);

                                    /**
                                     * Put the current place's address as the destination in
                                     * the destination bar as the user has selected this
                                     * place as thier destination
                                     */
                                    destinationAddressAutoCompleteFragment.setText(resultingAddress);
                                    destinationAddress = resultingAddress;

                                    /**
                                     * make marker and button disappear as both source and destination have
                                     * been entered.
                                     */
                                    makeMarkerDisappear();

                                    makeDestinationBarBlue();

                                    // this is the destination LatLng
                                    destinationLatLng = centerOfMapLatLng;

                                    // draw path between source and destination
                                    googleDirectionsApiWrapper.from(sourceLatLng).to(destinationLatLng).retreiveDirections().setShow20Warning(true).setTextView(fairQuoteTV).setMap(mMap).drawPathOnMap();

                                    /**
                                     * Add a marker at this place clearly indication that it is
                                     * the destination.
                                     */
                                    destinationMarker = mMap.addMarker(new MarkerOptions()
                                            .position(centerOfMapLatLng)
                                            .title("Destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.flag_blue)));
                                    destinationMarker.setVisible(true);

                                    // the destination has been entered
                                    destinationEntered = true;

                                    /**
                                     * as both source and destination have been entered
                                     * make the request cab UI visible to the user.
                                     */

                                    animateRequestCabView(true);

                                } else if (!sourceEntered && destinationEntered) {
                                    /**
                                     * make the cross button on the source card visible
                                     * so that the user can cancel the source that they
                                     * have set if they want to enter a different one.
                                     */
                                    sourceBarCrossIB.setVisibility(View.VISIBLE);

                                    /**
                                     * put the current address in the source bar as the
                                     * user has selected this place as thier source
                                     */
                                    sourceAddressAutocCompleteFragment.setText(resultingAddress);
                                    sourceAddress = resultingAddress;

                                    /**
                                     * make marker and button disappear as both source and destination have
                                     * been entered.
                                     */
                                    makeMarkerDisappear();

                                    makeSourceBarGreen();

                                    // this is the source LatLng
                                    sourceLatLng = centerOfMapLatLng;

                                    // draw path between source and destination
                                    googleDirectionsApiWrapper.from(sourceLatLng).to(destinationLatLng).retreiveDirections().setShow20Warning(true).setTextView(fairQuoteTV).setMap(mMap).drawPathOnMap();

                                    // Add a marker a the source indicating that it is the source
                                    sourceMarker = mMap.addMarker(new MarkerOptions()
                                            .position(centerOfMapLatLng)
                                            .title("Source").icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_green)));
                                    sourceMarker.setVisible(true);

                                    /**
                                     * as both source and destination have been entered
                                     * make the request cab UI visible to the user.
                                     */
                                    animateRequestCabView(true);

                                    // the source has been entered
                                    sourceEntered = true;


                                } else {
                                    /**
                                     * This condition is impossible as when
                                     * both the source and destination have
                                     * been entered the marker button cannot
                                     * be clicked
                                     */

                                }
                            } else {
                                Toast.makeText(getActivity(), "Currently, we only support rides in Twin Cities.", Toast.LENGTH_SHORT).show();
                            }


                        }
                    });
                }else{
                    Toast.makeText(getActivity(),"Currently, pick up points can only be in premisis of NUST H-12",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    void disableMarkerButton(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                markerButton.setClickable(false);
                markerButton.setFocusable(false);
            }
        },0);
    }

    void enableMarkerButton(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                markerButton.setClickable(true);
                markerButton.setFocusable(true);
            }
        },0);
    }

    private boolean isSourceLatLngWithinBounds(LatLng centerOfMapLatLng) {
        return centerOfMapLatLng.latitude<=33.666302&&
                        centerOfMapLatLng.longitude>= 72.958939
                        &&centerOfMapLatLng.latitude>=33.628988
                        &&centerOfMapLatLng.longitude<=73.017064;
    }

    void setOnCancelListenerForSourceCard(){
        sourceBarCrossIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCurrLocationButton = true;
                sourceBarCrossIB.setVisibility(View.GONE);
                sourceMarker.setVisible(false);
                sourceAddressAutocCompleteFragment.setText("");
                sourceAddressAutocCompleteFragment.setHint("Enter Source");
                sourceEntered = false;
                if(!destinationEntered){
                    elevateSourceBar();
                    makeDestinationBarBlank();
                    turnMarkerIntoSetSourceMarker();
                }else{
                    googleDirectionsApiWrapper.removePath();
                    destinationEnteredHighlightSourceBar();
                    turnMarkerIntoSetSourceMarker();
                    animateRequestCabView(false);
                }
            }
        });
    }

    void destinationEnteredHighlightSourceBar(){
        elevateSourceBar();
        makeDestinationBarBlue();
    }


    void turnMarkerIntoSetSourceMarker(){
        markerButton.setVisibility(View.VISIBLE);
        markerAtCenterOfMapIV.setVisibility(View.VISIBLE);
        markerButtonTextView.setText("SET SOURCE");
        markerButton.setCardBackgroundColor(Color.parseColor("#ddF9BA32"));
        markerAtCenterOfMapIV.setImageResource(R.mipmap.marker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            markerAtCenterOfMapIV.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F9BA32")));
        }
    }


    void makeDestinationBarBlank(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchDestinationCardView.setCardElevation(Utils.fromDpToPx(1f));
        }
        searchDestinationCardView.setCardBackgroundColor(Color.parseColor("#aaffffff"));
    }

    void elevateSourceBar(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchSouceCardView.setCardBackgroundColor(Color.parseColor("#ffffff"));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchSouceCardView.setCardElevation(Utils.fromDpToPx(6f));
        }
    }

    void makeDestinationBarBlue(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchDestinationCardView.setCardElevation(Utils.fromDpToPx(1f));
        }
        searchDestinationCardView.setCardBackgroundColor(Color.parseColor("#cc426E86"));
    }

    void animateRequestCabView(boolean up){
        if(up){
            Animations.makeVisible(requestCabButton,fairQuoteTV);
            Animations.playYoYoAnimOnMultipleViews(Techniques.SlideInUp,1000,requestCabButton,fairQuoteTV);
            if(animateCurrLocationButton) {
                RelativeLayout.LayoutParams currLocationButtonParams = (RelativeLayout.LayoutParams) currLocButton.getLayoutParams();
                currLocationButtonParams.bottomMargin += requestCabButton.getHeight();
                currLocationButtonParams.bottomMargin += fairQuoteTV.getHeight();
            }
        }else{
            Animations.makeVisible(fairQuoteTV,requestCabButton);
            Animations.playYoYoAnimOnMultipleViews(Techniques.SlideOutDown,1000,fairQuoteTV,requestCabButton);
            RelativeLayout.LayoutParams currLocationButtonParams = (RelativeLayout.LayoutParams) currLocButton.getLayoutParams();
            currLocationButtonParams.bottomMargin-=requestCabButton.getHeight();
            currLocationButtonParams.bottomMargin-=fairQuoteTV.getHeight();
        }

    }

    void setOnClickListenerOnCurrLocationButton(){
        currLocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLastLocation!=null){
                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastLocation.getLatitude(),
                                            mLastLocation.getLongitude()
                                    ),15.5f)
                    );
                }
            }
        });
    }

    void makeMarkerDisappear(){
        markerButton.setVisibility(View.INVISIBLE);
        markerAtCenterOfMapIV.setVisibility(View.INVISIBLE);
    }

    void turnMarkerIntoSetDestinationMarker(){
        markerButton.setVisibility(View.VISIBLE);
        markerAtCenterOfMapIV.setVisibility(View.VISIBLE);
        markerButtonTextView.setText("SET DESTINATION");
        markerButton.setCardBackgroundColor(Color.parseColor("#dd426E86"));
        markerAtCenterOfMapIV.setImageResource(R.mipmap.flag);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            markerAtCenterOfMapIV.setImageTintList(ColorStateList.valueOf(Color.parseColor("#426E86")));
        }
    }

    void sourceEnteredHighLightDestinationBar(){
        makeSourceBarGreen();
        elevateDestinationBar();
    }

    void makeSourceBarGreen(){
        searchSouceCardView.setCardBackgroundColor(Color.parseColor("#ccF9BA32"));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchSouceCardView.setCardElevation(Utils.fromDpToPx(1f));
        }
    }

    void elevateDestinationBar(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchDestinationCardView.setCardElevation(Utils.fromDpToPx(6f));
        }
        searchDestinationCardView.setCardBackgroundColor(Color.parseColor("#ffffff"));
    }

    void animateSelectionToast(String message) {
        if(noOfAnimationsRunning>=1){
            noOfAnimationsRunning--;
        }

        notifySelectionToastTV.setText(message);
        Animations.makeVisible(notifySelectionToastTV);
        if(!movingDownAnimation) {
            YoYo.with(Techniques.SlideInUp).listen(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    movingUpAnimation = true;
                    noOfAnimationsRunning++;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    movingUpAnimation = false;
                }
            }).duration(500).playOn(notifySelectionToastTV);
        }
        if(noOfAnimationsRunning == 1) {
            YoYo.with(Techniques.SlideOutDown).listen(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    movingDownAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    movingDownAnimation = false;
                    if(noOfAnimationsRunning>0){
                        noOfAnimationsRunning--;
                    }
                }
            }).duration(500).delay(2500).playOn(notifySelectionToastTV);

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //LatLngBounds latLngBounds = new LatLngBounds(new LatLng(33.547869, 73.275766),new LatLng(33.925381, 72.742947));
        //mMap.setLatLngBoundsForCameraTarget(latLngBounds);
        mMap.getUiSettings().setCompassEnabled(false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void cancellTrip(){
        if(Utils.getCurrUser() == null){return;}

        FirebaseDatabase.getInstance().getReference()
                .child("AcceptedOrders")
                .child(Utils.getUid())
                .setValue(null);


        FirebaseDatabase.getInstance().getReference().
                child("CanceledTripsServer").
                child(Utils.getUid()).
                setValue("null");
        FirebaseDatabase.getInstance().getReference().
                child("CanceledTripsDriver").
                child(Utils.getUid()).
                setValue("null");

        FirebaseDatabase.getInstance().getReference().
                child("State").child(Utils.getUid()).setValue(Constants.SET_SOURCE_STATE);

        Intent intent = new Intent(getActivity(),MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        getActivity().finish();
        startActivity(intent);
    }
}
