package org.teamspark.sp.subprojectmaps;




import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.support.v4.app.FragmentTransaction;


import com.actionbarsherlock.app.*;
import com.actionbarsherlock.view.*;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.teamspark.sp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class MapActivity extends SherlockFragmentActivity implements SearchList.searchHouseListener, OnRouteCalcCompleted,
        LocationListener, LocationSource, GoogleMap.OnCameraChangeListener {

    private static HashMap<Integer,LatLng> Housing;
    private LocationSource.OnLocationChangedListener mListener;
    private LocationManager lManager; //для маршрута
    private RouteHandler routeHandler;
    private static boolean isNeedToAddMarkersOfDoors = true;
    private static boolean isNeedToAddMarkersOfCorps = true;
    private static boolean isNeedToAddMarkersOfCorpsMore = false;

    private static final float Need_Zoom_Of_Center = 16;
    private static final float Need_Zoom_Of_Finded = 17;
    private static final int Number_Of_Home = 7;
    private static final String Tag = "MyActivity";
    private static int Number_Of_Searched_House = 0;
    private static final char LastCharBeforeNumber = '№';


    SupportMapFragment mapFragment;
    SearchList searchListFragment;
    GoogleMap map;
    FragmentTransaction fragmentTransaction;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setBtnGoVisibility(false);
        searchListFragment = new SearchList();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        map = mapFragment.getMap();
        if (map == null) {
            showMessage(getString(R.string.err_google_maps_not_available));
            finish();
            return;
        }
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setOnCameraChangeListener(this);

        lManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        lManager.requestLocationUpdates( lManager.getBestProvider(new Criteria(), true), 1, 1000, this);

        Housing = initMarkers();
        addMarkersOfCorps();
        toHome(map);

    }

    /**
     * <p>Получает HashMap с номерами и координатами корпусов</p>
     * @return HashMap с номерами и координатами корпусов
     */
    public static HashMap<Integer, LatLng> getHousing() {
        return Housing;
    }

    /**
     * <p>Центрирует на корпусе №7 с наперед указаным zoom</p>
     * @param map Карта на которой мы центрируемся
     */
    private void toHome(GoogleMap map) {
        Log.d(Tag, "toHome: entry");
        centeringOnLocation(map, Housing.get(Number_Of_Home), Need_Zoom_Of_Center);
        Log.d(Tag, "toHome: ended");
    }

    /**
     * <p>Вырезает из title маркера номер корпуса</p>
     * @param title Название маркера
     * @return Возвращает 1, если номера нет, и другое значение, если номер есть
     */
    private int findNumberOfHouseByTitle(String title){
        CharSequence charSequence = title;
        for (int i = 0; i < charSequence.length(); i++)
            if (charSequence.charAt(i) == LastCharBeforeNumber )
                return Integer.parseInt((String) charSequence.subSequence(i + 1, charSequence.length()));
        return 1;
    }

    /**
     * <p>Добавляет маркеры на карту</p>
     * Проходит по всем элементам Housing и добавляет маркеры с задаными номерами
     * корпусов и их кординатами. Ставит слушатель на каждый маркер. При клике на маркер
     * появляется кнопка для указания маршрута в даную точку координат
     */
    private void addMarkersOfDoors(){
        Log.d(Tag, "addMarkersOfDoors: entry");
        if (Housing.isEmpty()){
            showMessage(getString(R.string.no_elem_in_hmap));
            return;
        }

        Iterator<Integer> it = Housing.keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            map.addMarker(new MarkerOptions()
                    .position(Housing.get(key))
                    .title(getString(R.string.title_for_marker) + key))
                    .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.door_small));

        }

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                setNumberOfSearchedHouse(findNumberOfHouseByTitle(marker.getTitle()));
                setBtnGoVisibility(true);
                return true;
            }
        });
        Log.d(Tag, "addMarkersOfDoors: ended");
    }

    /**
     * <p>Добавляет маркеры корпусов на карту</p>
     * Добавляет маркеры с задаными номерами
     * корпусов и их кординатами. Ставит слушатель на каждый маркер. При клике на маркер
     * появляется кнопка для указания маршрута в даную точку координат
     */
    private void addMarkersOfCorps(){
        Log.d(Tag, "addMarkersCorps: entry");

        //TODO For all markers do the same

        //house #1
        map.addMarker(new MarkerOptions()
                .position(new LatLng(50.449476,30.460786))
                .title(getString(R.string.title_for_marker) + 1))
                .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.corp1_76));
        //house #7
        map.addMarker(new MarkerOptions()
                .position(new LatLng(50.448606,30.457046))
                .title(getString(R.string.title_for_marker) + 7))
                .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.corp7_76));
        //house #14
        map.addMarker(new MarkerOptions()
                .position(new LatLng(50.447807,30.456159))
                .title(getString(R.string.title_for_marker) + 14))
                .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.corp14_76));

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                setNumberOfSearchedHouse(findNumberOfHouseByTitle(marker.getTitle()));
                setBtnGoVisibility(true);
                return true;
            }
        });
        Log.d(Tag, "addMarkersCorps: ended");
    }

    /**
     * <p>Считывает xml-файл с номерами корпусов и соответствующими координатами</p>
     * @return HashMap<Номер корпуса, Координаты></> для удобного использования при добавлении маркеров
     */
    private HashMap<Integer,LatLng> initMarkers(){
        Log.d(Tag, "initMarkers: entry");
        String Markers[] = getResources().getStringArray(R.array.markers);
        HashMap<Integer,LatLng> hashMapLatLngStr = new HashMap<Integer, LatLng>();
        try {
            for (String marker : Markers){
                JSONObject JSONMarker = new JSONObject(marker);
                hashMapLatLngStr.put(JSONMarker.getInt("title"), new LatLng(JSONMarker.getDouble("lat"),
                        JSONMarker.getDouble("lng")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(Tag, "initMarkers: ended");
        return hashMapLatLngStr;
    }

    /**
     * <p>Центрирует карту в заданых координатах и с заданым зумом</p>
     * @param map Карта на которой мы центрируемся
     * @param location Координаты на которых мы центрируемся
     * @param zoom Заданое увеличение
     */
    private void centeringOnLocation(GoogleMap map, LatLng location, float zoom){
        Log.d(Tag, "centering: entry");

        if (location == null){
            showMessage(getString(R.string.err_point_absent));
        }
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, zoom);
        map.animateCamera(cameraUpdate);


        Log.d(Tag, "centering: ended");
    }

    /**
     * <p>Устанавливает номер дома для поиска</p>
     * @param neededHouse Номер дома
     */
    public void setNumberOfSearchedHouse(int neededHouse){
        Number_Of_Searched_House = neededHouse;
    }

    /**
     * <p>Получает с фрагмента "список" номер корпуса, что ищем</p>
     * Прячет фрагмент "список", центрирует карту над заданым домом и задействует появление кнопки
     * проложения маршрута
     * @param numberOfHouse Номер корпуса
     */
    public void findByHousingFromFragment(int numberOfHouse){
        hideSearchList();
        findByHousing(map, numberOfHouse);
        setBtnGoVisibility(true);
    }

    /**
     * <p>Находит корпус по номеру</p>
     * Проверяет есть ли корпус с заданным номером. Центрирует карту над этим корпусом.
     * @param map Карта с которой мы работаем
     * @param numberOfHouse Номер корпуса
     */
    private void findByHousing(GoogleMap map, int numberOfHouse){
        Log.d(Tag, "findByHousing: entry");
        if(!Housing.containsKey(numberOfHouse)){
            showMessage(getString(R.string.no_such_elem));
            return;
        }
        centeringOnLocation(map, Housing.get(numberOfHouse), Need_Zoom_Of_Finded);
        Log.d(Tag, "findByHousing: ended");
    }

    /**
     * <p>Выводит сообщение на экран</p>
     * @param text Текст сообщения
     */
    private void showMessage(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast msg = Toast.makeText(context, text,duration);
        msg.show();
    }

    /**
     * <p>Показывет на экране фрагмент "список"</p>
     */
    private void showSearchList(){

        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (!searchListFragment.isAdded()){
            fragmentTransaction.hide(mapFragment);
            fragmentTransaction.add(R.id.frgmCont, searchListFragment);
        } else {
            if (searchListFragment.isVisible()){
                fragmentTransaction.hide(searchListFragment);
                fragmentTransaction.show(mapFragment);
            } else {
                fragmentTransaction.hide(mapFragment);
                fragmentTransaction.show(searchListFragment);
            }
        }
        fragmentTransaction.commit();

    }

    /**
     * Прячет фрагмент "список"
     */
    private void hideSearchList(){
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.hide(searchListFragment);
        fragmentTransaction.show(mapFragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.item_center:
                toHome(map);
                break;
            case R.id.item_search:
                setBtnGoVisibility(false);
                showSearchList();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * <p>Задействет/отключает  кнопку проложения маршрута</p>
     * @param visibility Если true - задействуем, если false - отключаем
     */
    private void setBtnGoVisibility(boolean visibility){
        Button btnGo = (Button)findViewById(R.id.btnWay);
        if (visibility) {
            btnGo.setVisibility(View.VISIBLE);
            btnGo.setClickable(true);
        }
        else {
            btnGo.setVisibility(View.INVISIBLE);
            btnGo.setClickable(false);
        }

    }

    private void setBtnGoName(String nameOfButton){
        Button btnGo = (Button) findViewById(R.id.btnWay);
        btnGo.setText(nameOfButton);
    }

    /**
     * <p>Рисуем маршрут на карте от нашего местоположения к нужному корпусу</p>
     * <p>Срабатывает при нажатии кнопки проложения маршрута</p>
     * @param view Наша кнопка
     */
    public void onClickGo(View view){
        //drawLine(map, getUserLocation(), Housing.get(Number_Of_Searched_House));
        if (getUserLocation() == null) { showMessage("Your location is not available. Check your configuration");
            return;}
        Button btnGo = (Button) findViewById(R.id.btnWay);
        if (btnGo.getText().equals("Відмінити")){
            map.clear();
            addMarkersOfCorps();
            setBtnGoVisibility(false);
            setBtnGoName("Прямувати");
            setNumberOfSearchedHouse(0); //we don`t need more route
        } else {
            map.clear();
            addMarkersOfCorps();
            buildRoute();
            setBtnGoName("Відмінити");
        }

    }

    /**
     * <p>Получает местоположение пользователя</p>
     * @return Координаты пользователя
     */
    private LatLng getUserLocation(){

        map.setMyLocationEnabled(true);
        Location myLoc = map.getMyLocation();
        if (myLoc != null) {
            double myLocLatitude = map.getMyLocation().getLatitude();
            double myLocLongitude = map.getMyLocation().getLongitude();
            return new LatLng(myLocLatitude, myLocLongitude);
        }else{
            showMessage("Ваше місцезнаходження не доступне");
            return null;
        }
    }

    //методы для маршрутизации
    @Override
    public void onRouteCalcBegin() {
        pd = new ProgressDialog(this);
        //pd.setTitle("Зачекайте");
        pd.setMessage("Будується маршрут...");
        pd.show();
    }

    @Override
    public void onRouteCompleted(ArrayList route) {
        if (getUserLocation() == null){
            showMessage("Сервіс не доступний. Перевірте налаштування");
            return;
        }
        map.addMarker(new MarkerOptions().position((LatLng)route.get(0)))
                .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map.addPolyline((new PolylineOptions()
                .color(Color.rgb(0, 137, 255))
                .width(5))
                .addAll(route))
                .setGeodesic(true);
        map.addMarker(new MarkerOptions().position((LatLng)route.get(route.size() - 1)))
                .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        if(pd != null) pd.dismiss();


    }

    @Override
    public void errorMessageFromGMaps(String message){
        showMessage(message);
        if(pd != null) pd.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( lManager != null ){
            lManager.requestLocationUpdates(lManager.getBestProvider(new Criteria(), true), 1, 1000, this);
        }
    }
    protected void onPause() {
        if( lManager != null ){
            lManager.removeUpdates(this);
        }
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        if( mListener != null ){
            mListener.onLocationChanged( location );
        }
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;

    }

    @Override
    public void deactivate() {
        mListener = null;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void buildRoute(){
        if (Number_Of_Searched_House == 0) {
            Log.d(Tag, "Need to chose the object to what we build route");
            return;
        }
        routeHandler = new RouteHandler(this);
        routeHandler.calculateRoute(getUserLocation().latitude, getUserLocation().longitude,
                Housing.get(Number_Of_Searched_House).latitude, Housing.get(Number_Of_Searched_House).longitude,
                RouteHandler.FINE_ROUTE);
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.d(Tag, "Zoom: " + cameraPosition.zoom);

        if (cameraPosition.zoom <= 14) {
            map.clear();
            isNeedToAddMarkersOfDoors = true;
            isNeedToAddMarkersOfCorps = true;
            isNeedToAddMarkersOfCorpsMore = true;
        } else if (isNeedToAddMarkersOfDoors && (cameraPosition.zoom > 18)){
            addMarkersOfDoors();
            addMarkersOfCorps();
            buildRoute();
            isNeedToAddMarkersOfDoors = false;
            isNeedToAddMarkersOfCorps = false;
            isNeedToAddMarkersOfCorpsMore = true;
        } else if (isNeedToAddMarkersOfCorpsMore && (cameraPosition.zoom <= 17)
                && isNeedToAddMarkersOfCorpsMore) {
            map.clear();
            addMarkersOfCorps();
            buildRoute();
            isNeedToAddMarkersOfCorpsMore = false;
            isNeedToAddMarkersOfDoors = true;
        }


    }


}
