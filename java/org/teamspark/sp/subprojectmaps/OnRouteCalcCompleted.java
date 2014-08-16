package org.teamspark.sp.subprojectmaps;

import java.util.ArrayList;

public interface OnRouteCalcCompleted{
    void onRouteCalcBegin();
    void onRouteCompleted( ArrayList route );
    void errorMessageFromGMaps(String message);
}
