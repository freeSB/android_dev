package org.teamspark.sp.subprojectmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;

/**
 * Класс возвращает полилинию дороги, если указан mapView, отрисовывается на нём
 * @author Богдан
 *
 */
public class RouteHandler extends AsyncTask<String,String,String> {

    private final static String TAG = "RouteHandler";

    /**
     * Для точного интерполирования марштура
     */
    public static final int FINE_ROUTE = 1;

    /**
     * Для грубого интерполирования маршрута
     */
    public static final int COARSE_ROUTE = 2;

    private final HttpClient client = new DefaultHttpClient();
    private String content;
    private boolean error = false;
    private String errorMsg = "";

    private ArrayList polyline;

    private int accuracyRoute = 1;

    private long distance;

    private OnRouteCalcCompleted listener;

    public RouteHandler( OnRouteCalcCompleted l ){
        this.listener = l;
    }

    public double getDistance(){
        Log.d(TAG, "distance = " + distance + "m");
        return distance/1000;
    }

    public void calculateRoute( double latStart, double lonStart, double latEnd, double lonEnd, int accuracy ){

        this.accuracyRoute = accuracy;

        StringBuilder origin = new StringBuilder();
        origin.append( Double.toString(latStart));
        origin.append(",");
        origin.append( Double.toString(lonStart));

        StringBuilder destination = new StringBuilder();
        destination.append( Double.toString(latEnd));
        destination.append(",");
        destination.append( Double.toString(lonEnd));

        List nameValuePairs = new ArrayList();
        nameValuePairs.add(new BasicNameValuePair("origin", origin.toString()));
        nameValuePairs.add(new BasicNameValuePair("destination", destination.toString() ));
        nameValuePairs.add(new BasicNameValuePair("sensor", "false"));
        nameValuePairs.add(new BasicNameValuePair("mode", "walking"));
        String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        execute( "http://maps.google.com/maps/api/directions/json" + "?" + paramString );

    }

    public boolean isError(){
        return error;
    }

    public String getErrorMsg(){
        return errorMsg;
    }

    @Override
    protected void onPreExecute(){
        listener.onRouteCalcBegin();
    }
    @Override
    protected String doInBackground(String... urls) {

        Log.d(TAG, "RouteHandler::doInBackground");

        try {
            Log.v(TAG, urls[0]);
            HttpPost httppost = new HttpPost(urls[0]);
            ResponseHandler responseHandler = new BasicResponseHandler();
            content = (String)client.execute(httppost, responseHandler);
        } catch (ClientProtocolException e) {
            Log.d(TAG, "GetRouteHandler::ClientProtocolException");
            e.printStackTrace();
            error = true;
            cancel(true);
        } catch (IOException e) {
            Log.d(TAG, "GetRouteHandler::IOException");
            e.printStackTrace();
            error = true;
            cancel(true);
        }
        return content;
    }

    protected void onPostExecute(String content) {
        if (error) {
            errorMsg = "Перевірте налаштування доступу до мережі INTERNET";
        } else {
            try {
                JSONObject response = new JSONObject(content);
                String status = response.getString("status");
                Log.v(TAG, content);
                if (status.equalsIgnoreCase("OK")) {
                    polyline = new ArrayList();

                    JSONArray routesArray = response.getJSONArray("routes");
                    JSONObject route = routesArray.getJSONObject(0);
                    // массив с информацией об отрезке маршрута
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);

                    JSONObject distanceObj = leg.getJSONObject("distance");
                    distance = distanceObj.getLong("value");

                    JSONObject durationObj = leg.getJSONObject("duration");

                    // содержит куб выделения информационного окна для маршрута.
                    JSONObject bounds = route.getJSONObject("bounds");
                    JSONObject bounds_southwest = bounds.getJSONObject("southwest");
                    JSONObject bounds_northeast = bounds.getJSONObject("northeast");

                    double maxLat = bounds_northeast.getDouble("lat");
                    double maxLon = bounds_northeast.getDouble("lng");
                    double minLat = bounds_southwest.getDouble("lat");
                    double minLon = bounds_southwest.getDouble("lng");


                    JSONArray steps = leg.getJSONArray("steps");
                    for (int i = 0; i < steps.length(); i++){
                        JSONObject step = steps.getJSONObject(i);
                        JSONObject start_location = step.getJSONObject("start_location");
                        JSONObject end_location = step.getJSONObject("end_location");

                        double latitudeStart = start_location.getDouble("lat");
                        double longitudeStart = start_location.getDouble("lng");
                        double latitudeEnd = end_location.getDouble("lat");
                        double longitudeEnd = end_location.getDouble("lng");
                        LatLng startGeoPoint = new LatLng(latitudeStart, longitudeStart);
                        LatLng endGeoPoint = new LatLng(latitudeEnd, longitudeEnd);
                        JSONObject polylineObject = step.getJSONObject("polyline");
                        if (accuracyRoute == FINE_ROUTE) {
                            List points = decodePoly(polylineObject.getString("points"));
                            Log.d(TAG, " " + points.size());
                            polyline.addAll(points);
                        } else {
                            polyline.add(startGeoPoint);
                            polyline.add(endGeoPoint);
                        }
                    }

                } else if (status.equalsIgnoreCase("NOT_FOUND")) {
                    errorMsg += "По крайней мере для одной заданной точки (исходной точки, " +
                            "пункта назначения или путевой точки) геокодирование невозможно.";
                } else if (status.equalsIgnoreCase("ZERO_RESULTS")) {
                    errorMsg += "Между исходной точкой и пунктом назначения не найдено ни одного маршрута.";
                } else if (status.equalsIgnoreCase("MAX_WAYPOINTS_EXCEEDED")) {
                    errorMsg += "в запросе задано слишком много waypoints. Максимальное количество" +
                            " waypoints равно 8 плюс исходная точка и пункт назначения.";
                } else if (status.equalsIgnoreCase("INVALID_REQUEST")) {
                    errorMsg += "Запрос недопустим";
                } else if (status.equalsIgnoreCase("OVER_QUERY_LIMIT")) {
                    errorMsg += "Cлужба получила слишком много запросов от вашего приложения " +
                            "в разрешенный период времени.";
                } else if (status.equalsIgnoreCase("REQUEST_DENIED")) {
                    errorMsg += "Cлужба Directions отклонила запрос вашего приложения";
                } else if (status.equalsIgnoreCase("UNKNOWN_ERROR")) {
                    errorMsg += "Oбработка запроса маршрута невозможна из-за ошибки сервера. " +
                            "При повторной попытке запрос может быть успешно выполнен";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (error) {
            listener.errorMessageFromGMaps(errorMsg);
            return;
        }
        listener.onRouteCompleted( polyline );


    }

    /**
     * Декодирует полилинию из переданной гуглом строки
     * @param encoded
     * @return
     */
    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( lat/1E5, lng/1E5);
            poly.add(p);
        }

        return poly;
    }

}
